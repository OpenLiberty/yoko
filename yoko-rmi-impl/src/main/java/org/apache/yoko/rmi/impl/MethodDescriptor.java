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

import org.apache.yoko.rmi.api.RemoteOnewayException;
import org.apache.yoko.util.concurrent.LazyReference;
import org.omg.CORBA.ORB;
import org.omg.CORBA.SystemException;
import org.omg.CORBA.portable.InputStream;
import org.omg.CORBA.portable.OutputStream;
import org.omg.CORBA.portable.ResponseHandler;
import org.omg.CORBA.portable.UnknownException;

import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.rmi.MarshalException;
import java.rmi.RemoteException;
import java.rmi.UnexpectedException;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.Character.isUpperCase;
import static java.lang.Character.toLowerCase;
import static java.security.AccessController.doPrivileged;
import static java.util.function.Predicate.isEqual;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.WARNING;
import static javax.rmi.CORBA.Util.mapSystemException;
import static org.apache.yoko.util.Exceptions.as;
import static org.apache.yoko.util.PrivilegedActions.action;
import static org.apache.yoko.util.Streams.concatStreams;

public final class MethodDescriptor extends ModelElement {
    static final Logger logger = Logger.getLogger(MethodDescriptor.class.getName());

    /** The reflected method object for this method */
    final Method reflectedMethod;
    final LazyReference<Boolean> responseExpectedRef = new LazyReference<>(this::genResponseExpected);
    final LazyReference<TypeDescriptor> returnTypeRef = new LazyReference<>(this::genReturnType);
    final LazyReference<List<TypeDescriptor>> parameterTypesRef = new LazyReference<>(this::genParameterTypes);
    final LazyReference<List<ExceptionDescriptor>> exceptionTypesRef = new LazyReference<>(this::genExceptionTypes);
    final LazyReference<Boolean> copyWithinStateRef = new LazyReference<>(this::genCopyWithinState);

    private static boolean notRemoteException(Class<?> aClass) {
        return !RemoteException.class.isAssignableFrom(aClass);
    }

    private Boolean genResponseExpected() {
        if (!getReturnType().getType().equals(Void.class)) {
            return true;
        }

        return getExceptionTypes().stream()
                .map(TypeDescriptor::getType)
                .noneMatch(isEqual(RemoteOnewayException.class));
    }

    public boolean getResponseExpected() {
        return responseExpectedRef.get();
    }

    TypeDescriptor genReturnType() {
        Class<?> returnType = doPrivileged(action(reflectedMethod::getReturnType));
        return repo.getDescriptor(returnType);
    }

    private TypeDescriptor getReturnType() {
        return returnTypeRef.get();
    }

    List<TypeDescriptor> genParameterTypes() {
        Class<?>[] paramTypes = doPrivileged(action(reflectedMethod::getParameterTypes));
        return Arrays.stream(paramTypes)
                .map(repo::getDescriptor)
                .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
    }

    private List<TypeDescriptor> getParameterTypes() {
        return parameterTypesRef.get();
    }

    List<ExceptionDescriptor> genExceptionTypes() {
        Class<?>[] exceptionTypes = doPrivileged(action(reflectedMethod::getExceptionTypes));
        return Arrays.stream(exceptionTypes)
                .map(repo::getDescriptor)
                .map(ExceptionDescriptor.class::cast)
                .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
    }

    private List<ExceptionDescriptor> getExceptionTypes() {
        return exceptionTypesRef.get();
    }

    Boolean genCopyWithinState() {
        return getParameterTypes().stream()
                .anyMatch(TypeDescriptor::copyWithinState);
    }

    private boolean getCopyWithinState() {
        return copyWithinStateRef.get();
    }

    public Method getReflectedMethod() {
        return reflectedMethod;
    }

    MethodDescriptor(Method method, TypeRepository repository) {
        super(repository, method.getName());
        reflectedMethod = method;
    }

    /**
     * Copy a set of arguments. If sameState=true, then we're invoking on the
     * same RMIState, i.e. an environment with the same context class loader.
     */
    Object[] copyArguments(Object[] args, boolean sameState, ORB orb) throws RemoteException {
        if (!sameState) {

            try {
                OutputStream out = orb.create_output_stream();
                List<TypeDescriptor> paramTypes = getParameterTypes();
                IntStream.range(0, args.length)
                        .filter(i -> paramTypes.get(i).copyBetweenStates())
                        .forEach(i -> paramTypes.get(i).write(out, args[i]));

                InputStream in = out.create_input_stream();
                IntStream.range(0, args.length)
                        .filter(i -> paramTypes.get(i).copyBetweenStates())
                        .forEach(i -> args[i] = paramTypes.get(i).read(in));
            } catch (SystemException ex) {
                logger.log(FINE, ex, () -> "Exception occurred copying arguments");
                throw mapSystemException(ex);
            }

        } else if (getCopyWithinState()) {
            CopyState state = new CopyState(repo);
            List<TypeDescriptor> paramTypes = getParameterTypes();
            IntStream.range(0, args.length)
                    .filter(i -> paramTypes.get(i).copyWithinState())
                    .forEach(i -> {
                try {
                    args[i] = state.copy(args[i]);
                } catch (CopyRecursionException e) {
                    final int idx = i;
                    final Object[] args_arr = args;
                    state.registerRecursion(new CopyRecursionResolver(
                            args[i]) {
                        public void resolve(Object value) {
                            args_arr[idx] = value;
                        }
                    });
                }
            });
        }

        return args;
    }

    Object copyResult(Object result, boolean sameState, ORB orb) throws RemoteException {
        if (null == result) return null;

        if (!sameState) {
            if (getReturnType().copyBetweenStates()) {
                try {
                    OutputStream out = orb.create_output_stream();
                    getReturnType().write(out, result);

                    InputStream in = out.create_input_stream();
                    return getReturnType().read(in);
                } catch (SystemException ex) {
                    logger.log(FINE, ex, () -> "Exception occurred copying result");
                    throw mapSystemException(ex);
                }
            }
        } else if (getCopyWithinState()) {
            CopyState state = new CopyState(repo);
            try {
                return state.copy(result);
            } catch (CopyRecursionException e) {
                throw as(MarshalException::new, e, "cannot copy recursive value?");
            }
        }

        return result;
    }

    /**
     * read the arguments to this method, and return them as an array of objects
     */
    public Object[] readArguments(InputStream in) {
        return getParameterTypes().stream()
                .map(desc -> desc.read(in))
                .toArray();
    }

    public void writeArguments(OutputStream out, Object[] args) {
        /*
         * if (log.isDebugEnabled ()) { java.util.Map recurse = new
         * org.apache.yoko.rmi.util.IdentityHashMap (); java.io.CharArrayWriter cw = new
         * java.io.CharArrayWriter (); java.io.PrintWriter pw = new
         * java.io.PrintWriter (cw);
         * 
         * pw.print ("invoking "); pw.print (reflected_method.toString ());
         * 
         * for (int i = 0; i < parameter_count; i++) { pw.print (" arg["+i+"] =
         * "); if (args[i] == null) { pw.write ("null"); } else { TypeDescriptor
         * desc;
         * 
         * try { desc = getTypeRepository ().getDescriptor (args[i].getClass
         * ()); } catch (RuntimeException ex) { desc = getParameterTypes()[i]; }
         * 
         * desc.print (pw, recurse, args[i]); } }
         * 
         * pw.close (); log.debug (cw.toString ()); }
         */

        List<TypeDescriptor> paramTypes = getParameterTypes();
        IntStream.range(0, paramTypes.size()).forEach(i -> paramTypes.get(i).write(out, args[i]));
    }

    /** write the result of this method */
    public void writeResult(OutputStream out, Object value) {
        getReturnType().write(out, value);
    }

    public OutputStream writeException(ResponseHandler response, Throwable ex) {
        return getExceptionTypes().stream()
                .filter(exceptionType -> exceptionType.getType().isInstance(ex))
                .findFirst()
                .map(exceptionType -> {
                    org.omg.CORBA_2_3.portable.OutputStream out = (org.omg.CORBA_2_3.portable.OutputStream) response.createExceptionReply();
                    out.write_string(exceptionType.getExceptionRepositoryID());
                    out.write_value(ex);
                    return out;
                })
                .orElseThrow(() -> {
                    logger.log(WARNING, ex, () -> "unhandled exception: " + ex.getMessage());
                    return new UnknownException(ex);
                });
    }

    public void readException(InputStream in) throws Throwable {
        org.omg.CORBA_2_3.portable.InputStream in2 = (org.omg.CORBA_2_3.portable.InputStream) in;

        String exId = in.read_string();
        Throwable ex = (Throwable) in2.read_value();

        boolean matched = getExceptionTypes().stream()
                .map(ExceptionDescriptor::getExceptionRepositoryID)
                .anyMatch(exId::equals);
        if (matched) throw ex;

        if (ex instanceof Exception) {
            throw as(UnexpectedException::new, ex, ex.getMessage());
        } else if (ex instanceof Error) {
            throw ex;
        }
    }

    public Object readResult(InputStream in) {

        /*
         * if (log.isDebugEnabled ()) { java.util.Map recurse = new
         * org.apache.yoko.rmi.util.IdentityHashMap (); java.io.CharArrayWriter cw = new
         * java.io.CharArrayWriter (); java.io.PrintWriter pw = new
         * java.io.PrintWriter (cw);
         * 
         * pw.print ("returning from "); pw.println (reflected_method.toString
         * ()); pw.print (" => ");
         * 
         * if (result == null) { pw.write ("null"); } else {
         * 
         * TypeDescriptor desc;
         * 
         * try { desc = getTypeRepository ().getDescriptor (result.getClass ()); }
         * catch (RuntimeException ex) { desc = getReturnType(); }
         * 
         * desc.print (pw, recurse, result); }
         * 
         * pw.close (); log.debug (cw.toString ()); }
         */

        return getReturnType().read(in);
    }

    String transformOverloading(String mname) {
        StringBuilder buf = new StringBuilder(mname);

        List<TypeDescriptor> paramTypes = getParameterTypes();
        if (paramTypes.isEmpty()) {
            buf.append("__");
        } else {
            for (TypeDescriptor parameterType : paramTypes) {
                buf.append("__");
                buf.append(parameterType.getIDLName());
            }
        }

        return buf.toString();
    }

    boolean isOverloaded = false;

    boolean isCaseSensitive = false;

    void setOverloaded() { isOverloaded = true; }

    void setCaseSensitive() { isCaseSensitive = true; }

    public void init() { }

    @Override
    String genIDLName() {
        String idl_name;

        if (isSetterMethod()) {
            idl_name = "_set_" + transformIdentifier(attributeName());
        } else if (isGetterMethod()) {
            idl_name = "_get_" + transformIdentifier(attributeName());
        } else {
            idl_name = transformIdentifier(java_name);
        }

        if (isCaseSensitive) {
            idl_name = transformCaseSensitive(idl_name);
        }

        if (isOverloaded) {
            idl_name = transformOverloading(idl_name);
        }
        return idl_name;
    }

    private String attributeName() {
        String methodName = java_name;
        StringBuilder buf = new StringBuilder();

        int pfxLen;
        if (methodName.startsWith("get"))
            pfxLen = 3;
        else if (methodName.startsWith("is"))
            pfxLen = 2;
        else if (methodName.startsWith("set"))
            pfxLen = 3;
        else
            throw new RuntimeException("methodName " + methodName + " is not attribute");

        if (methodName.length() >= (pfxLen + 2)
                && isUpperCase(methodName.charAt(pfxLen))
                && isUpperCase(methodName.charAt(pfxLen + 1))) {
            return methodName.substring(pfxLen);
        }

        buf.append(toLowerCase(methodName.charAt(pfxLen)));
        buf.append(methodName.substring(pfxLen + 1));
        return buf.toString();
    }

    private boolean isSetterMethod() {
        Method m = getReflectedMethod();

        String name = m.getName();
        if (!name.startsWith("set"))
            return false;

        if (name.length() == 3)
            return false;

        if (!isUpperCase(name.charAt(3)))
            return false;

        if (!m.getReturnType().equals(Void.TYPE))
            return false;

        return m.getParameterTypes().length == 1;
    }

    private boolean isGetterMethod() {
        Method m = getReflectedMethod();

        String name = m.getName();

        int pfxLen;
        if (name.startsWith("get"))
            pfxLen = 3;
        else if (name.startsWith("is")
                && m.getReturnType().equals(Boolean.TYPE))
            pfxLen = 2;
        else
            return false;

        if (name.length() == pfxLen)
            return false;

        if (!isUpperCase(name.charAt(pfxLen)))
            return false;

        if (m.getReturnType().equals(Void.TYPE))
            return false;

        return m.getParameterTypes().length == 0;
    }

    static String transformIdentifier(String name) {
        StringBuilder buf = new StringBuilder();

        // it cannot start with underscore; prepend a 'J'
        if (name.charAt(0) == '_')
            buf.append('J');

        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);

            // basic identifier elements
            if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')
                    || (ch >= '0' && ch <= '9') || (ch == '_')) {
                buf.append(ch);
            }

            // dot becomes underscore
            else if (ch == '.') {
                buf.append('_');
            }

            // else, use translate to UXXXX unicode
            else {
                buf.append('U');
                String hex = Integer.toHexString(ch);
                switch (hex.length()) {
                case 1:
                    buf.append('0');
                case 2:
                    buf.append('0');
                case 3:
                    buf.append('0');
                }
                buf.append(hex);
            }
        }

        return buf.toString();
    }

    private static String transformCaseSensitive(String name) {
        StringBuilder buf = new StringBuilder(name);
        buf.append('_');

        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);

            // basic identifier elements
            if (isUpperCase(ch)) {
                buf.append('_');
                buf.append(i);
            }
        }

        return buf.toString();
    }

    void writeStubMethod(PrintWriter pw) {
        pw.println("\t/**");
        pw.println("\t *");
        pw.println("\t */");

        writeMethodHead(pw);
        pw.println("\t{");

        pw.println("\t\tboolean marshal = !" + UTIL + ".isLocal (this);");
        pw.println("\t\tmarshal: while(true) {");
        pw.println("\t\tif(marshal) {");

        writeMarshalCall(pw);

        pw.println("\t\t} else {");

        writeLocalCall(pw);

        pw.println("\t\t}}");

        // pw.println ("throw new org.omg.CORBA.NO_IMPLEMENT(\"local
        // invocation\")");

        pw.println("\t}\n");
    }

    void writeMethodHead(PrintWriter pw) {
        pw.print("\tpublic ");
        writeJavaType(pw, reflectedMethod.getReturnType());

        pw.print(' ');
        pw.print(java_name);

        pw.print(" (");
        Class<?>[] args = reflectedMethod.getParameterTypes();
        for (int i = 0; i < args.length; i++) {
            if (i != 0) {
                pw.print(", ");
            }

            writeJavaType(pw, args[i]);
            pw.print(" arg");
            pw.print(i);
        }
        pw.println(")");

        Class<?>[] ex = reflectedMethod.getExceptionTypes();
        pw.print("\t\tthrows ");
        for (int i = 0; i < ex.length; i++) {
            if (i != 0) {
                pw.print(", ");
            }

            writeJavaType(pw, ex[i]);
        }

        pw.println();
    }

    static void writeJavaType(PrintWriter pw, Class<?> type) {
        if (type.isArray()) {
            writeJavaType(pw, type.getComponentType());
            pw.print("[]");
        } else {
            pw.print(type.getName());
        }
    }

    static final String SERVANT = "org.omg.CORBA.portable.ServantObject";

    static final String INPUT = "org.omg.CORBA_2_3.portable.InputStream";

    static final String OUTPUT = "org.omg.CORBA_2_3.portable.OutputStream";

    static final String UTIL = "javax.rmi.CORBA.Util";

    void writeMarshalCall(PrintWriter pw) {
        pw.println("\t\t" + INPUT + " in = null;");
        pw.println("\t\ttry {");

        pw.println("\t\t\t" + OUTPUT + " out " + "= (" + OUTPUT
                + ")_request (\"" + getIDLName() + "\", true);");
        pw.println("\t\t\ttry{");

        Class<?>[] args = reflectedMethod.getParameterTypes();
        for (int i = 0; i < args.length; i++) {
            TypeDescriptor desc = repo.getDescriptor(args[i]);
            pw.print("\t\t\t");
            desc.writeMarshalValue(pw, "out", "arg" + i);
            pw.println(";");
        }

        pw.println("\t\t\tin = (" + INPUT + ")_invoke(out);");

        Class<?> rtype = reflectedMethod.getReturnType();
        if (rtype == Void.TYPE) {
            pw.println("\t\t\treturn;");
        } else {
            pw.print("\t\t\treturn (");
            writeJavaType(pw, rtype);
            pw.print(")");

            TypeDescriptor desc = repo.getDescriptor(rtype);
            desc.writeUnmarshalValue(pw, "in");
            pw.println(";");
        }

        pw
                .println("\t\t} catch (org.omg.CORBA.portable.ApplicationException ex) {");

        pw.println("\t\t\t" + INPUT + " exin = (" + INPUT
                + ")ex.getInputStream();");
        pw.println("\t\t\tString exname = exin.read_string();");

        Arrays.stream(reflectedMethod.getExceptionTypes())
                .filter(MethodDescriptor::notRemoteException)
                .map(repo::getDescriptor)
                .map(ExceptionDescriptor.class::cast)
                .forEach(desc -> {
            pw.println("\t\t\tif (exname.equals(\""
                    + desc.getExceptionRepositoryID() + "\"))");
            pw.print("\t\t\t\tthrow (");
            writeJavaType(pw, desc.getType());
            pw.print(")exin.read_value(");
            writeJavaType(pw, desc.getType());
            pw.println(".class);");
        });

        pw.println("\t\t\tthrow new java.rmi.UnexpectedException(exname,ex);");

        pw.println("\t\t} catch (org.omg.CORBA.portable.RemarshalException ex) {");
        pw.println("\t\t\tcontinue marshal;");
        pw.println("\t\t} finally {");
        pw.println("\t\t\t if(in != null) _releaseReply(in);");
        pw.println("\t\t}");

        pw.println("\t\t} catch (org.omg.CORBA.SystemException ex) {");
        pw.println("\t\t\t throw " + UTIL + ".mapSystemException(ex);");
        pw.println("\t\t}");
    }

    void writeLocalCall(PrintWriter pw) {
        Class<?> thisClass = reflectedMethod.getDeclaringClass();

        pw.println("\t\t\t" + SERVANT + " so = _servant_preinvoke (");
        pw.println("\t\t\t\t\"" + getIDLName() + "\",");
        pw.print("\t\t\t\t");
        writeJavaType(pw, thisClass);
        pw.println(".class);");
        pw
                .print("\t\t\tif (so==null || so.servant==null || !(so.servant instanceof ");
        writeJavaType(pw, thisClass);
        pw.print("))");
        pw.println(" { marshal=true; continue marshal; }");

        pw.println("\t\t\ttry {");

        // copy arguments
        Class<?>[] args = reflectedMethod.getParameterTypes();
        if (args.length == 1) {
            if (repo.getDescriptor(args[0]).copyInStub()) {
                pw.print("\t\t\t\targ0 = (");
                writeJavaType(pw, args[0]);
                pw.println(")" + UTIL + ".copyObject(arg0, _orb());");
            }
        } else if (args.length > 1) {
            boolean[] copy = new boolean[args.length];
            int copyCount = 0;

            for (int i = 0; i < args.length; i++) {
                TypeDescriptor td = repo.getDescriptor(args[i]);
                copy[i] = td.copyInStub();
                if (copy[i]) {
                    copyCount += 1;
                }
            }

            if (copyCount > 0) {
                pw.println("\t\t\t\tObject[] args = new Object[" + copyCount
                        + "];");
                int pos = 0;
                for (int i = 0; i < args.length; i++) {
                    if (copy[i]) {
                        pw.println("\t\t\t\targs[" + (pos++) + "] = arg" + i
                                + ";");
                    }
                }
                pw.println("\t\t\t\targs=" + UTIL
                        + ".copyObjects(args,_orb());");
                pos = 0;
                for (int i = 0; i < args.length; i++) {
                    if (copy[i]) {
                        pw.print("\t\t\t\targ" + i + "=(");
                        writeJavaType(pw, args[i]);
                        pw.println(")args[" + (pos++) + "];");
                    }
                }
            }
        }

        // now, invoke!
        Class<?> out = reflectedMethod.getReturnType();
        pw.print("\t\t\t\t");
        if (out != Void.TYPE) {
            writeJavaType(pw, out);
            pw.print(" result = ");
        }

        pw.print("((");
        writeJavaType(pw, thisClass);
        pw.print(")so.servant).");
        pw.print(java_name);
        pw.print("(");

        for (int i = 0; i < args.length; i++) {
            if (i != 0) {
                pw.print(',');
            }
            pw.print("arg" + i);
        }

        pw.println(");");

        pw.print("\t\t\t\treturn ");
        if (Void.TYPE == out) {
            pw.println(";");
        } else {
            TypeDescriptor td = repo.getDescriptor(out);
            if (td.copyInStub()) {
                pw.print('(');
                writeJavaType(pw, out);
                pw.print(')');
                pw.println(UTIL + ".copyObject (result, _orb());");
            } else {
                pw.println("result;");
            }
        }

        pw.println("\t\t\t} finally {");
        pw.println("\t\t\t\t_servant_postinvoke (so);");
        pw.println("\t\t\t}");
    }

    void addDependencies(Set<Class<?>> classes) {
        concatStreams(
                Stream.of(reflectedMethod.getReturnType()),
                Arrays.stream(reflectedMethod.getParameterTypes()),
                Arrays.stream(reflectedMethod.getExceptionTypes()))
                .map(repo::getDescriptor)
                .forEach(desc -> desc.addDependencies(classes));
    }

}
