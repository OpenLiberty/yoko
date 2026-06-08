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
import org.omg.CORBA.INTERNAL;
import org.omg.CORBA.ORB;
import org.omg.CORBA.TypeCode;
import org.omg.CORBA.portable.InputStream;
import org.omg.CORBA.portable.OutputStream;

import java.io.PrintWriter;
import java.rmi.Remote;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.logging.Logger;

import static org.apache.yoko.util.Exceptions.as;

abstract class TypeDescriptor extends ModelElement {
    protected static final Logger logger = Logger.getLogger(TypeDescriptor.class.getName());

    private final Class<?> type;
    private final java.util.function.Function<InputStream, Object> readFunction;
    private final java.util.function.BiConsumer<OutputStream, Object> writeFunction;

    final Class<?> getType() { return type; }

    private final LazyReference<String> repIdRef = new LazyReference<>(this::genRepId);

    private final LazyReference<String> packageNameRef = new LazyReference<>(this::genPackageName);
    String genPackageName() {
        int idx = java_name.lastIndexOf('.');
        return ((idx < 0) ? "" : java_name.substring(0, idx));
    }
    final String getPackageName() {
        return packageNameRef.get();
    }

    private final LazyReference<String> typeNameRef = new LazyReference<>(this::genTypeName);
    String genTypeName() {
        int idx = java_name.lastIndexOf('.');
        return ((idx < 0) ? java_name : java_name.substring(idx + 1));
    }
    final String getTypeName() {
        return typeNameRef.get();
    }

    private final LazyReference<FullKey> keyRef = new LazyReference<>(this::genKey);
    FullKey genKey() {
        return new FullKey(getRepositoryID(), getType());
    }
    final FullKey getKey() {
        return keyRef.get();
    }

    private final LazyReference<TypeDescriptor> initializedRef = new LazyReference<>(this::firstInit, () -> this);

    final TypeDescriptor getInitialized() {
        return initializedRef.get();
    }

    private TypeDescriptor firstInit() {
        init();
        repo.addToRepIdDescriptors.accept(this);
        return this;
    }

    public static class SimpleKey {
        private final String repid;

        public SimpleKey(String repid) {
            this.repid = repid;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((repid == null) ? 0 : repid.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (!(obj instanceof SimpleKey)) return false;
            return Objects.equals(repid, ((SimpleKey)obj).repid);
        }
    }

    public static final class FullKey extends SimpleKey {
        private final Class<?> localType;

        public FullKey(String repid, Class<?> localType) {
            super(repid);
            this.localType = localType;
        }

        @Override
        public int hashCode() {
            // must just be the same as SimpleKey's hashCode
            return super.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (!(obj instanceof SimpleKey)) return false;
            if (obj instanceof FullKey &&
                    !Objects.equals(localType, ((FullKey) obj).localType)) return false;
            return super.equals(obj);
        }
    }

    @Override
    public String toString() {
        return String.format("%s{class=\"%s\",repId=\"%s\"}",
                this.getClass().getName(), getType(),
                getRepositoryID());
    }

    interface ReadFn extends Function<InputStream, Object> {}
    interface WriteFn extends BiConsumer<OutputStream,Object> {}

    protected TypeDescriptor(Class<?> type, TypeRepository repository, ReadFn readFn, WriteFn writeFn) {
        super(repository, type.getName());
        this.type = type;
        this.readFunction = readFn;
        this.writeFunction = writeFn;
    }

    @Override
    String genIDLName() {
        return java_name.replace('.', '_');
    }

    String genRepId() {
        return String.format("RMI:%s:%016X", getType().getName(), 0);
    }
    final String getRepositoryID() {
        return repIdRef.get();
    }

    private final LazyReference<RemoteInterfaceDescriptor> remoteInterfaceRef = new LazyReference<>(this::genRemoteInterface);
    RemoteInterfaceDescriptor genRemoteInterface() {
        throw new UnsupportedOperationException("class " + getType() + " does not implement " + Remote.class.getName());
    }
    final RemoteInterfaceDescriptor getRemoteInterface() {
        return remoteInterfaceRef.get();
    }



    /** Read an instance of this value from a CDR stream */
    public final Object read(InputStream in) {
        return readFunction.apply(in);
    }

    /** Write an instance of this value to a CDR stream */
    public final void write(OutputStream out, Object val) {
        writeFunction.accept(out, val);
    }

    public boolean isCustomMarshalled() {
        return false;
    }

    private final LazyReference<Long> classHashRef = new LazyReference<>(this::genClassHash);

    final long getClassHash() {
        return classHashRef.get();
    }

    long genClassHash() {
        return 0L;
    }

    /**
     * Creates a JVM signature string for the given type.
     * Used for computing RMI hash codes and method signatures.
     *
     * @param type the class to create a signature for
     * @return the JVM signature string (e.g., "I" for int, "Ljava/lang/String;" for String)
     */
    static String makeSignature(Class<?> type) {
        if (type.isPrimitive()) {
            // Use switch for better readability and performance
            if (boolean.class == type) return "Z";
            if (byte.class == type) return "B";
            if (short.class == type) return "S";
            if (char.class == type) return "C";
            if (int.class == type) return "I";
            if (long.class == type) return "J";
            if (float.class == type) return "F";
            if (double.class == type) return "D";
            if (void.class == type) return "V";
            throw new RuntimeException("unknown primitive class: " + type);
        }

        if (type.isArray()) {
            // Build signature while traversing to component type
            StringBuilder sb = new StringBuilder();
            Class<?> componentType = type;
            while (componentType.isArray()) {
                sb.append('[');
                componentType = componentType.getComponentType();
            }
            sb.append(makeSignature(componentType));
            return sb.toString();
        }

        // Object type signature
        return "L" + type.getName().replace('.', '/') + ";";
    }

    void init() {
        try {
            getTypeCode();
        } catch (INTERNAL internal) {
            throw internal;
        } catch (RuntimeException | Error ex) {
            throw as(INTERNAL::new, ex);
        }
    }

    private final LazyReference<TypeCode> typeCodeRef = new LazyReference<>(this::genTypeCode, this::genTypeCodePlaceholder);
    abstract TypeCode genTypeCode();
    TypeCode genTypeCodePlaceholder() {
        ORB orb = ORB.init();
        return orb.create_recursive_tc(getRepositoryID());
    }
    final TypeCode getTypeCode() { return typeCodeRef.get(); }

    Object copyObject(Object value, CopyState state) {
        throw new InternalError("cannot copy " + value.getClass().getName());
    }

    void writeMarshalValue(PrintWriter pw, String outName,
            String paramName) {
        pw.print(outName);
        pw.print('.');
        pw.print("write_");
        pw.print(getIDLName());
        pw.print('(');
        pw.print(paramName);
        pw.print(')');
    }

    void writeUnmarshalValue(PrintWriter pw, String inName) {
        pw.print(inName);
        pw.print('.');
        pw.print("read_");
        pw.print(getIDLName());
        pw.print('(');
        pw.print(')');
    }

    void addDependencies(Set<Class<?>> classes) {
    }

    boolean copyInStub() {
        return true;
    }

    void print(PrintWriter pw, Map<Object,Integer> recurse, Object val) {
        if (val == null) {
            pw.print("null");
        }

        Integer old = (Integer) recurse.get(val);
        if (old != null) {
            pw.print("^" + old);
        } else {
            pw.println(getType().getName() + "@"
                    + Integer.toHexString(System.identityHashCode(val)));
        }
    }

    /**
     * Method copyBetweenStates.
     *
     * @return boolean
     */
    boolean copyBetweenStates() { return true; }

    /**
     * Method copyWithinState.
     *
     * @return boolean
     */
    boolean copyWithinState() {
        return true;
    }

}
