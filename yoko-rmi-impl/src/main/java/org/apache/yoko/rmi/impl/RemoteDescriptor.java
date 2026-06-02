/*
 * Copyright 2026 IBM Corporation and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an \"AS IS\" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.apache.yoko.rmi.impl;

import org.apache.yoko.util.concurrent.LazyReference;
import org.omg.CORBA.ORB;
import org.omg.CORBA.TypeCode;
import org.omg.CORBA.portable.InputStream;
import org.omg.CORBA.portable.OutputStream;

import javax.rmi.PortableRemoteObject;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.security.AccessController.doPrivileged;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableList;
import static java.util.function.Function.identity;
import static java.util.logging.Level.FINER;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toCollection;
import static javax.rmi.CORBA.Util.writeRemoteObject;
import static org.apache.yoko.util.Predicates.not;
import static org.apache.yoko.util.PrivilegedActions.getClassLoader;
import static org.apache.yoko.util.PrivilegedActions.getDeclaredMethods;
import static org.apache.yoko.util.PrivilegedActions.getInterfaces;

abstract class RemoteDescriptor extends TypeDescriptor {
    private final LazyReference<Map<String, MethodDescriptor>> methodMapRef = new LazyReference<>(this::genMethodMap);

    private final LazyReference<Map<Method, MethodDescriptor>> reflMethodMapRef = new LazyReference<>(this::genReflMethodMap);

    private final LazyReference<Collection<MethodDescriptor>> operationsRef = new LazyReference<>(this::genOperations);

    private final LazyReference<List<RemoteDescriptor>> superDescriptorsRef = new LazyReference<>(this::genSuperDescriptors);

    @Override
    abstract RemoteInterfaceDescriptor genRemoteInterface();


    private final LazyReference<List<String>> idsRef = new LazyReference<>(this::genIds);

    private Map<String, MethodDescriptor> getMethodMap() {
        return methodMapRef.get();
    }

    private Map<Method, MethodDescriptor> getReflMethodMap() {
        return reflMethodMapRef.get();
    }

    private Collection<MethodDescriptor> getOperations() {
        return operationsRef.get();
    }

    private List<RemoteDescriptor> getSuperDescriptors() {
        return superDescriptorsRef.get();
    }

    private List<String> getIds() {
        return idsRef.get();
    }

    List<String> genIds() {
        return genAllRemoteInterfaces(getType()).stream()
                .map(repo::getDescriptor)
                .map(TypeDescriptor::getRepositoryID)
                .collect(collectingAndThen(
                        Collectors.toList(),
                        Collections::unmodifiableList));
    }

    private static final String[] EMPTY_STRINGS = {};
    public String[] all_interfaces() {
        return getIds().toArray(EMPTY_STRINGS);
    }

    private Map<String, MethodDescriptor> genMethodMap() {
        return getOperations().stream()
                .collect(collectingAndThen(
                        Collectors.toMap(MethodDescriptor::getIDLName, identity()),
                        Collections::unmodifiableMap));
    }

    public MethodDescriptor getMethod(String idlName) {
        return getMethodMap().get(idlName);
    }

    void debugMethodMap() {
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(() -> "METHOD MAP FOR " + getType().getName());
            getMethodMap().forEach((idlName, desc) ->
                logger.finer(() -> "IDL " + idlName + " -> " + desc.reflectedMethod));
        }
    }

    private Map<Method, MethodDescriptor> genReflMethodMap() {
        return getOperations().stream()
                .collect(collectingAndThen(
                        Collectors.toMap(MethodDescriptor::getReflectedMethod, identity()),
                        Collections::unmodifiableMap));
    }

    public MethodDescriptor getMethod(Method reflMethod) {
        return getReflMethodMap().get(reflMethod);
    }

    RemoteDescriptor(Class<?> type, TypeRepository repository) {
        super(type, repository);
    }

    private static final MethodDescriptor[] EMPTY_DESCRIPTORS = {};
    public MethodDescriptor[] getMethods() {
        return getOperations().toArray(EMPTY_DESCRIPTORS);
    }

    private List<RemoteDescriptor> genSuperDescriptors() {
        Class<?>[] supers = doPrivileged(getInterfaces(getType()));

        return Arrays.stream(supers)
                .filter(not(Remote.class::equals))
                .filter(not(Object.class::equals))
                .filter(Remote.class::isAssignableFrom)
                .filter(Class::isInterface)
                .map(repo::getDescriptor)
                .map(RemoteDescriptor.class::cast)
                .collect(collectingAndThen(
                        Collectors.toList(),
                        Collections::unmodifiableList));
    }

    private Collection<MethodDescriptor> genOperations() {
        // first step is to build the helpers for any super classes
        ArrayList<MethodDescriptor> methodList = getSuperDescriptors().stream()
                .flatMap(superHelper -> superHelper.getOperations().stream())
                .collect(toCollection(ArrayList::new));

        // next, build the method helpers for this class
        List<Method> localMethods = getLocalMethods();

        // Collect all methods (super + local) for overload and case-sensitivity detection
        List<Method> allMethods = Stream.concat(
                methodList.stream().map(MethodDescriptor::getReflectedMethod),
                localMethods.stream()
        ).collect(collectingAndThen(
                Collectors.toList(),
                Collections::unmodifiableList));

        // Calculate overloadedMethodNames - methods with same name but different signatures
        Set<String> overloadedMethodNames = getOverloadedMethodNames(allMethods);

        // Calculate caseSensitiveMethodNames - methods that differ only in case
        Set<String> caseSensitiveMethodNames = getCaseSensitiveMethodNames(allMethods);

        localMethods.forEach(method -> {
            MethodDescriptor op = new MethodDescriptor(method, repo);
            String mname = op.java_name;

            // is there another method that differs only in case?
            if (caseSensitiveMethodNames.contains(mname)) op.setCaseSensitive();

            // is this method overloaded?
            if (overloadedMethodNames.contains(mname)) op.setOverloaded();

            op.init();
            methodList.add(op);
        });

        // Remove duplicates by using a map keyed by IDL name
        return unmodifiableCollection(
                methodList.stream()
                        .peek(desc -> logger.finer(() -> "Adding method " + desc.java_name + " under " + desc.getIDLName()))
                        .collect(Collectors.toMap(MethodDescriptor::getIDLName, identity(), (current, updated) -> updated))
                        .values());
    }

    private static Set<String> getOverloadedMethodNames(List<Method> allMethods) {
        return allMethods.stream()
                .collect(groupingBy(Method::getName, mapping(RemoteDescriptor::createMethodSelector, Collectors.toSet())))
                .entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .map(Map.Entry::getKey)
                .collect(collectingAndThen(
                        Collectors.toSet(),
                        Collections::unmodifiableSet));
    }

    private static Set<String> getCaseSensitiveMethodNames(List<Method> allMethods) {
        return allMethods.stream()
                .map(Method::getName)
                .collect(groupingBy(String::toLowerCase, Collectors.toSet()))
                .entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .flatMap(entry -> entry.getValue().stream())
                .collect(collectingAndThen(
                        Collectors.toSet(),
                        Collections::unmodifiableSet));
    }

    private static String createMethodSelector(Method m) {
        String params = Arrays.stream(m.getParameterTypes())
                .map(Class::getName)
                .collect(joining(", "));
        return m.getName() + "(" + params + ")";
    }


    private List<Method> getLocalMethods() {
        return unmodifiableList(addNonRemoteInterfaceMethods(getType(), new ArrayList<>()));
    }

    private ArrayList<Method> addNonRemoteInterfaceMethods(Class<?> clz, ArrayList<Method> result) {
        Method[] methods;
        try {
            methods = doPrivileged(getDeclaredMethods(clz));
        } catch (NoClassDefFoundError e) {
            ClassLoader clzClassLoader = doPrivileged(getClassLoader(clz));
            logger.log(FINER, e, () -> "cannot find class " + e.getMessage() + " from "
                    + clz.getName() + " (classloader " + clzClassLoader + "): "
                    + e.getMessage());
            throw e;
        }
        // since this is a remote interface, we need to add everything
        Collections.addAll(result, methods);

        Arrays.stream(clz.getInterfaces())
                .filter(not(Remote.class::isAssignableFrom))
                .forEach(iface -> addNonRemoteInterfaceMethods(iface, result));
        return result;
    }

    boolean isRemoteMethod(Method m) {
        return Arrays.stream(m.getExceptionTypes())
                .anyMatch(RemoteException.class::isAssignableFrom);
    }

    static RemoteInterfaceDescriptor genMostSpecificRemoteInterface(Class<?> type, TypeRepository repo) {
        final SortedSet<Class<?>> remoteInterfaces = genAllRemoteInterfaces(type);
        if (remoteInterfaces.isEmpty()) {
            throw new RuntimeException(type.getName() + " has no remote interfaces");
        }
        //first remoteInterface is the most specific
        return repo.getDescriptor(remoteInterfaces.first()).getRemoteInterface();
    }

    private static final Comparator<Class<?>> INTERFACE_COMPARATOR = (c1, c2) -> {
        if (c1.equals(c2)) return 0;
        if (c1.isAssignableFrom(c2)) return 1;
        if (c2.isAssignableFrom(c1)) return -1;
        //classes are unrelated, so sort on class name
        return c1.getName().compareTo(c2.getName());
    };

    private static SortedSet<Class<?>> genAllRemoteInterfaces(Class<?> type) {
        return addRemoteInterfacesToSet(type, new TreeSet<>(INTERFACE_COMPARATOR));
    }

    private static SortedSet<Class<?>> addRemoteInterfacesToSet(Class<?> type, SortedSet<Class<?>> interfaces) {
        if (Remote.class.equals(type)) return interfaces;
        if (type.isInterface()) interfaces.add(type);
        Optional.ofNullable(type.getSuperclass())
                .filter(not(Object.class::equals))
                .ifPresent(parent -> addRemoteInterfacesToSet(parent, interfaces));
        Arrays.stream(type.getInterfaces())
                .filter(Remote.class::isAssignableFrom)
                .forEach(i -> addRemoteInterfacesToSet(i, interfaces));
        return interfaces;
    }

    /** Read an instance of this value from a CDR stream */
    @Override
    public Object read(InputStream in) {
        return PortableRemoteObject.narrow(in.read_Object(), getType());
    }

    /** Write an instance of this value to a CDR stream */
    @Override
    public void write(OutputStream out, Object val) {
        writeRemoteObject(out, val);
    }

    @Override
    protected final TypeCode genTypeCode() {
        return ORB.init().create_interface_tc(getRepositoryID(), getType().getName());
    }

    @Override
    void writeMarshalValue(PrintWriter pw, String outName, String paramName) {
        pw.print("javax.rmi.CORBA.Util.writeRemoteObject(");
        pw.print(outName);
        pw.print(',');
        pw.print(paramName);
        pw.print(')');
    }

    @Override
    void writeUnmarshalValue(PrintWriter pw, String inName) {
        pw.print('(');
        pw.print(getType().getName());
        pw.print(')');
        pw.print(PortableRemoteObject.class.getName());
        pw.print(".narrow(");
        pw.print(inName);
        pw.print('.');
        pw.print("read_Object(),");
        pw.print(getType().getName());
        pw.print(".class)");
    }

    static String classNameFromStub(String name) {
        if (name.startsWith("org.omg.stub."))
            name = name.substring("org.omg.stub.".length());

        // strip xx._X_Stub -> xx.X
        int idx = name.lastIndexOf('.');
        if (name.charAt(idx + 1) == '_' && name.endsWith("_Stub")) {
            if (idx == -1) {
                return name.substring(1, name.length() - 5);
            } else {
                return name.substring(0, idx + 1) /* package. */
                        + name.substring(idx + 2, name.length() - 5);
            }
        }

        return null;
    }

    private static String stubClassName(Class<?> c) {

        String cname = c.getName();

        String pkgname;
        int idx = cname.lastIndexOf('.');
        if (idx == -1) {
            pkgname = "org.omg.stub";
        } else {
            pkgname = "org.omg.stub." + cname.substring(0, idx);
        }

        String cplain = cname.substring(idx + 1);

        return pkgname + "._" + cplain + "_Stub";
    }

    void writeStubClass(PrintWriter pw) {

        Class<?> c = getType();
        String cname = c.getName();
        String fullname = stubClassName(c);
        //String stubname = fullname.substring(fullname.lastIndexOf('.') + 1);
        String pkgname = fullname.substring(0, fullname.lastIndexOf('.'));
        String cplain = cname.substring(cname.lastIndexOf('.') + 1);

        pw.println("/** ");
        pw.println(" *  RMI/IIOP stub for " + cname);
        pw.println(" *  Generated using Apache Yoko stub generator.");
        pw.println(" */");

        pw.println("package " + pkgname + ";\n");

        pw.println("public class _" + cplain + "_Stub");
        pw.println("\textends javax.rmi.CORBA.Stub");
        pw.println("\timplements " + cname);
        pw.println("{");

        //
        // construct String[] _ids;
        //
        pw.println("\tprivate static final String[] _ids = {");
        getIds().forEach(id -> pw.println("\t\t\"" + id + "\","));
        pw.println("\t};\n");

        pw.println("\tpublic String[] _ids() {");
        pw.println("\t\treturn _ids;");
        pw.println("\t}");

        //
        // now, construct stub methods
        //
        getOperations().forEach(meth -> meth.writeStubMethod(pw));

        pw.println("}");
    }

    String getStubClassName() {
        return stubClassName(getType());
    }

    @Override
    void addDependencies(Set<Class<?>> classes) {
        Class<?> c = getType();

        if (c == Remote.class || classes.contains(c))
            return;

        classes.add(c);

        Optional.ofNullable(c.getSuperclass())
                .map(repo::getDescriptor)
                .ifPresent(desc -> desc.addDependencies(classes));

        Arrays.stream(c.getInterfaces())
                .map(repo::getDescriptor)
                .forEach(desc -> desc.addDependencies(classes));

        getOperations().forEach(mth -> mth.addDependencies(classes));
    }

    @Override
    boolean copyInStub() {
        return false;
    }
}
