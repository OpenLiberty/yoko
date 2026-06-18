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
import org.omg.CORBA.MARSHAL;
import org.omg.CORBA.SystemException;
import org.omg.CORBA.ValueMember;

import javax.rmi.PortableRemoteObject;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectStreamField;
import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.rmi.Remote;
import java.security.PrivilegedActionException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static java.lang.reflect.Modifier.isPublic;
import static java.security.AccessController.doPrivileged;
import static java.util.Collections.unmodifiableMap;
import static java.util.logging.Level.FINER;
import static org.apache.yoko.util.Exceptions.as;
import static org.apache.yoko.util.PrivilegedActions.exAction;
import static org.apache.yoko.util.yasf.Yasf.NON_SERIALIZABLE_FIELD_IS_ABSTRACT_VALUE;

abstract class FieldDescriptor extends ModelElement implements Comparable<FieldDescriptor> {
    static Logger logger = Logger.getLogger(FieldDescriptor.class.getName());

    /**
     * Functional interface for creating FieldDescriptor instances.
     */
    @FunctionalInterface
    private interface FieldDescriptorFactory {
        FieldDescriptor create(Class<?> owner, Class<?> type, String name, Field f, TypeRepository repository);
    }

    private static final LazyReference<Map<Class<?>, FieldDescriptorFactory>> simpleFactoriesRef =
        new LazyReference<>(FieldDescriptor::genSimpleFactories);

    private static Map<Class<?>, FieldDescriptorFactory> genSimpleFactories() {
        Map<Class<?>, FieldDescriptorFactory> map = new HashMap<>();
        // Primitive types
        map.put(boolean.class, BooleanFieldDescriptor::new);
        map.put(byte.class, ByteFieldDescriptor::new);
        map.put(short.class, ShortFieldDescriptor::new);
        map.put(char.class, CharFieldDescriptor::new);
        map.put(int.class, IntFieldDescriptor::new);
        map.put(long.class, LongFieldDescriptor::new);
        map.put(float.class, FloatFieldDescriptor::new);
        map.put(double.class, DoubleFieldDescriptor::new);
        // Other simple types
        map.put(String.class, StringFieldDescriptor::new);
        map.put(Object.class, AnyFieldDescriptor::new);
        map.put(Externalizable.class, AnyFieldDescriptor::new);
        map.put(Serializable.class, AnyFieldDescriptor::new);
        map.put(Remote.class, RemoteFieldDescriptor::new);
        return unmodifiableMap(map);
    }

    private static Map<Class<?>, FieldDescriptorFactory> getSimpleFactories() {
        return simpleFactoriesRef.get();
    }
    /**
     * Represents the access level of a ValueMember field.
     */
    enum ValueMemberAccess {
        PRIVATE(0),
        PUBLIC(1);

        public final short value;

        ValueMemberAccess(int value) {
            this.value = (short) value;
        }
    }

    private final MethodHandle setter;
    private final MethodHandle getter;

    final Class<?> type;

    final Class<?> declaringClass;

    private final LazyReference<ValueMember> valueMemberRef = new LazyReference<>(this::genValueMember);

    private final ValueMemberAccess valueMemberAccess;


    FieldDescriptor(Class<?> owner, Class<?> type, String name, TypeRepository repo) {
        this(owner, type, name, null, repo);
    }

    FieldDescriptor(Class<?> owner, Class<?> type, String name, Field f, TypeRepository repo) {
        super(repo, name);
        this.type = type;
        declaringClass = owner;

        if (null == f) {
            this.valueMemberAccess = ValueMemberAccess.PRIVATE;
            this.setter = null;
            this.getter = null;
        } else {
            int modifiers = f.getModifiers();
            this.valueMemberAccess = isPublic(modifiers) ? ValueMemberAccess.PUBLIC : ValueMemberAccess.PRIVATE;
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            try {
                Field fieldCopy = doPrivileged(exAction(() -> {
                    Field copy = f.getDeclaringClass().getDeclaredField(f.getName());
                    copy.setAccessible(true);
                    return copy;
                }));
                this.getter = lookup.unreflectGetter(fieldCopy);
                this.setter = lookup.unreflectSetter(fieldCopy);
            } catch (PrivilegedActionException pae) {
                throw new RuntimeException(pae.getCause());
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    void setFieldContents(Object o, Object value) throws IOException {
        if (null == setter) throw new IOException("No local field '" + java_name + "' in class " + declaringClass.getName());
        try {
            setter.invoke(o, value);
        } catch (Throwable t) {
            throw as(IOException::new, t, t.getMessage());
        }
    }

    Object getFieldContents(Object o) throws IOException {
        if (null == getter) throw new IOException("No local field '" + java_name + "' in class " + declaringClass.getName());
        try {
            return getter.invoke(o);
        } catch (Throwable t) {
            throw as(IOException::new, t, t.getMessage());
        }
    }

    @Override
    final String genIDLName() { return java_name; }

    ValueMember genValueMember() {
        TypeDescriptor desc = repo.getDescriptor(type);
        TypeDescriptor owner = repo.getDescriptor(declaringClass);
        return new ValueMember(getIDLName(), desc.getRepositoryID(),
                owner.getRepositoryID(), "1.0", desc.getTypeCode(), null,
                valueMemberAccess.value);
    }

    final ValueMember getValueMember() {
        return valueMemberRef.get();
    }

    public Class<?> getType() { return type; }

    /**
     * ordering of fields
     */
    @Override
    public int compareTo(FieldDescriptor other) {
        if (this.isPrimitive() && !other.isPrimitive()) return -1;
        if (!this.isPrimitive() && other.isPrimitive()) return 1;
        return java_name.compareTo(other.java_name);
    }

    public boolean isPrimitive() { return type.isPrimitive(); }

    abstract void read(ObjectReader reader, Object obj) throws IOException;
    abstract void write(ObjectWriter writer, Object obj) throws IOException;
    abstract void readFieldIntoMap(ObjectReader reader, Map<String, Object> map) throws IOException;
    abstract void writeFieldFromMap(ObjectWriter writer, Map<String, Object> map) throws IOException;
    abstract void copyState(Object orig, Object copy, CopyState state);

    static FieldDescriptor get(Field f, TypeRepository repository) {
        return get(f.getDeclaringClass(), f.getType(), f.getName(), f, repository);
    }

    static FieldDescriptor getForSerialPersistentField(Class<?> declaringClass, ObjectStreamField field, TypeRepository repository) {
        Field f = null;
        try {
            f = declaringClass.getDeclaredField(field.getName());
        } catch (NoSuchFieldException e) {
            logger.log(FINER, () -> "Cannot find java field \"" + field.getName()
                    + "\" in class \"" + declaringClass.getName() + "\""
                    + " - perhaps it is handled in readObject()/writeObject()");
        }
        return get(declaringClass, field.getType(), field.getName(), f, repository);
    }

    static FieldDescriptor get(Class<?> owner, Class<?> type, String name, Field f, TypeRepository repository) {
        // Check for exact type matches in simple factories
        FieldDescriptorFactory factory = getSimpleFactories().get(type);
        if (factory != null) {
            return factory.create(owner, type, name, f, repository);
        }

        // Handle types requiring assignability checks
        if (org.omg.CORBA.Object.class.isAssignableFrom(type)) {
            return new CorbaObjectFieldDescriptor(owner, type, name, f, repository);
        }

        if (Remote.class.isAssignableFrom(type)) {
            return new RemoteFieldDescriptor(owner, type, name, f, repository);
        }

        if (Serializable.class.isAssignableFrom(type)) {
            return new ValueFieldDescriptor(owner, type, name, f, repository);
        }

        if (type.isInterface() && type.getMethods().length == 0) {
            // TODO: make this spec-compliant
            // See Java-to-IDL 1.4 section 4.3.11 "Mapping Abstract Interfaces".
            // This check should include methods from parent interfaces,
            // and exclude methods that throw RemoteException (or a superclass of it).
            // Fixing this will require a further Yasf setting and may break
            // compatibility with how other ORBs marshal null values.
            return new ObjectFieldDescriptor(owner, type, name, f, repository);
        }

        // interface classes with methods and non-serializable classes
        return new ValueFieldDescriptor(owner, type, name, f, repository);
    }

    void print(PrintWriter pw, Map<Object, Integer> recurse, Object val) {
        pw.print(java_name);
        pw.print("=");
        try {
            Object obj = getFieldContents(val);
            if (obj == null) {
                pw.print("null");
            } else {
                TypeDescriptor desc = repo.getDescriptor(obj.getClass());
                desc.print(pw, recurse, obj);
            }
        } catch (IllegalStateException | IOException ex) {
            pw.print("<non-local>");
        }
    }
}

class RemoteFieldDescriptor extends FieldDescriptor {
    final Class<?> interfaceType;

    RemoteFieldDescriptor(Class<?> owner, Class<?> type, String name, Field f, TypeRepository repository) {
        super(owner, type, name, f, repository);
        interfaceType = (type.isInterface() ? type : findInterfaceType(type));
    }

    private static Class<?> findInterfaceType(Class<?> type) {
        Class<?> t = type;

        while (!Object.class.equals(t)) {
            for (Class<?> intf : t.getInterfaces()) {
                if (Remote.class.isAssignableFrom(intf)) {
                    return intf;
                }
            }
            t = t.getSuperclass();
        }
        throw new RuntimeException("cannot find remote interface for " + type);
    }

    public void read(ObjectReader reader, Object obj) throws IOException {
        Object value = reader.readRemoteObject(interfaceType);
        setFieldContents(obj, value);
    }

    public void write(ObjectWriter writer, Object obj) throws IOException {
        writer.writeRemoteObject(getFieldContents(obj));
    }

    void copyState(final Object orig, final Object copy, CopyState state) {
        try {
            setFieldContents(copy, state.copy(getFieldContents(orig)));
        } catch (CopyRecursionException e) {
            state.registerRecursion(new CopyRecursionResolver(orig) {
                public void resolve(Object value) {
                    try {
                        setFieldContents(copy, value);
                    } catch (IOException ex) {
                        throw as(InternalError::new, ex, ex.getMessage());
                    }
                }
            });
        } catch (IOException ex) {
            throw as(InternalError::new, ex, ex.getMessage());
        }
    }

    /**
     * @see FieldDescriptor#readFieldIntoMap(ObjectReader, Map)
     */
    void readFieldIntoMap(ObjectReader reader, Map<String, Object> map) {
        Remote value = reader.readRemoteObject(interfaceType);
        map.put(java_name, value);
    }

    /**
     * @see FieldDescriptor#writeFieldFromMap(ObjectWriter, Map)
     */
    void writeFieldFromMap(ObjectWriter writer, Map<String, Object> map) throws IOException {
        Remote value = (Remote) map.get(java_name);
        writer.writeRemoteObject(value);
    }
}

class AnyFieldDescriptor extends FieldDescriptor {
    static final Logger logger = Logger.getLogger(AnyFieldDescriptor.class
            .getName());

    boolean narrowValue;

    AnyFieldDescriptor(Class<?> owner, Class<?> type, String name, Field f, TypeRepository repository) {
        super(owner, type, name, f, repository);
        narrowValue = Remote.class.isAssignableFrom(type);
    }

    public void read(ObjectReader reader, Object obj)
            throws IOException {
        try {
            Object val = reader.readAny();
            if (narrowValue && val != null && !type.isInstance(val)) {
                try {
                    val = PortableRemoteObject.narrow(val, this.type);
                } catch (SecurityException ex) {
                    logger.finer(() -> "Narrow failed" + "\n" + ex);
                    throw ex;
                }
            } else if (val != null && !type.isInstance(val)) {
                throw new MARSHAL("value is instance of "
                        + val.getClass().getName() + " -- should be: "
                        + type.getName());
            }

            setFieldContents(obj, val);
        } catch (IllegalStateException ex) {
            throw as(MARSHAL::new, ex, ex.getMessage());
        }
    }

    public void write(ObjectWriter writer, Object obj)
            throws IOException {
        writer.writeAny(getFieldContents(obj));
    }

    void copyState(final Object orig, final Object copy, CopyState state) {
        try {
            setFieldContents(copy, state.copy(getFieldContents(orig)));
        } catch (CopyRecursionException e) {
            state.registerRecursion(new CopyRecursionResolver(orig) {
                public void resolve(Object value) {
                    try {
                        setFieldContents(copy, value);
                    } catch (IOException ex) {
                        throw as(InternalError::new, ex, ex.getMessage());
                    }
                }
            });
        } catch (IOException ex) {
            throw as(InternalError::new, ex, ex.getMessage());
        }
    }

    /**
     * @see FieldDescriptor#readFieldIntoMap(ObjectReader, Map)
     */
    void readFieldIntoMap(ObjectReader reader, Map<String, Object> map) {
        Object value = reader.readAny();
        map.put(java_name, value);
    }

    /**
     * @see FieldDescriptor#writeFieldFromMap(ObjectWriter, Map)
     */
    void writeFieldFromMap(ObjectWriter writer, Map<String, Object> map) throws IOException {
        Object value = map.get(java_name);
        writer.writeAny(value);
    }
}

class ValueFieldDescriptor extends FieldDescriptor {
    ValueFieldDescriptor(Class<?> owner, Class<?> type, String name, Field f, TypeRepository repository) {
        super(owner, type, name, f, repository);
    }

    public void read(ObjectReader reader, Object obj) throws IOException {
        try {
            Object value;
            if (NON_SERIALIZABLE_FIELD_IS_ABSTRACT_VALUE.isSupported()
                    || type.isInterface()
                    || Serializable.class.isAssignableFrom(type)) {
                value = reader.readValueObject(getType());
            } else {
                // older versions of Yoko treat non-serializable classes as abstract objects
                value = reader.readAbstractObject();
            }
            setFieldContents(obj, value);
        } catch (IllegalStateException ex) {
            throw as(MARSHAL::new, ex, ex.getMessage());
        }
    }

    public void write(ObjectWriter writer, Object obj) throws IOException {
        if (NON_SERIALIZABLE_FIELD_IS_ABSTRACT_VALUE.isSupported()
                || type.isInterface()
                || Serializable.class.isAssignableFrom(type)) {
            try {
                writer.writeValueObject(getFieldContents(obj));
            } catch (SystemException e) {
                throw e;
            } catch (Exception e) {
                throw as(MARSHAL::new, e, "Object of class " + obj.getClass().getName() + " is not a valuetype");
            }
        } else {
            // older versions of Yoko treat non-serializable classes as abstract objects
            writer.writeObject(getFieldContents(obj));
        }
    }

    void copyState(final Object orig, final Object copy, CopyState state) {
        try {
            setFieldContents(copy, state.copy(getFieldContents(orig)));
        } catch (CopyRecursionException e) {
            state.registerRecursion(new CopyRecursionResolver(orig) {
                public void resolve(Object value) {
                    try {
                        setFieldContents(copy, value);
                    } catch (IOException ex) {
                        throw as(InternalError::new, ex, ex.getMessage());
                    }
                }
            });
        } catch (IOException ex) {
            throw as(InternalError::new, ex, ex.getMessage());
        }
    }

    /**
     * @see FieldDescriptor#readFieldIntoMap(ObjectReader, Map)
     */
    void readFieldIntoMap(ObjectReader reader, Map<String, Object> map) {
        Serializable value = (Serializable) reader
                .readValueObject();
        map.put(java_name, value);
    }

    /**
     * @see FieldDescriptor#writeFieldFromMap(ObjectWriter, Map)
     */
    void writeFieldFromMap(ObjectWriter writer, Map<String, Object> map) throws IOException {
        Serializable value = (Serializable) map
                .get(java_name);
        writer.writeValueObject(value);
    }
}

class StringFieldDescriptor extends FieldDescriptor {
    StringFieldDescriptor(Class<?> owner, Class<?> type, String name,
            Field f, TypeRepository repository) {
        super(owner, type, name, f, repository);
    }

    public void read(ObjectReader reader, Object obj) throws IOException {
        String value = (String) reader.readValueObject();
        setFieldContents(obj, value);
    }

    public void write(ObjectWriter writer, Object obj) throws IOException {
        writer.writeValueObject(getFieldContents(obj));
    }

    void copyState(Object orig, Object copy, CopyState state) {
        try {
            setFieldContents(copy, getFieldContents(orig));
        } catch (IOException ex) {
            throw as(InternalError::new, ex, ex.getMessage());
        }
    }

    /**
     * @see FieldDescriptor#readFieldIntoMap(ObjectReader, Map)
     */
    void readFieldIntoMap(ObjectReader reader, Map<String, Object> map) {
        String value = (String) reader.readValueObject();
        map.put(java_name, value);
    }

    /**
     * @see FieldDescriptor#writeFieldFromMap(ObjectWriter, Map)
     */
    void writeFieldFromMap(ObjectWriter writer, Map<String, Object> map) throws IOException {
        String value = (String) map.get(java_name);
        writer.writeValueObject(value);
    }
}

class ObjectFieldDescriptor extends FieldDescriptor {
    ObjectFieldDescriptor(Class<?> owner, Class<?> type, String name,
            Field f, TypeRepository repository) {
        super(owner, type, name, f, repository);
    }

    public void read(ObjectReader reader, Object obj) throws IOException {
        try {
            Object value = reader.readAbstractObject();
            setFieldContents(obj, value);
        } catch (IllegalStateException ex) {
            throw as(MARSHAL::new, ex, ex.getMessage());
        }
    }

    public void write(ObjectWriter writer, Object obj) throws IOException {
        writer.writeObject(getFieldContents(obj));
    }

    void copyState(final Object orig, final Object copy, CopyState state) {
        try {
            setFieldContents(copy, state.copy(getFieldContents(orig)));
        } catch (CopyRecursionException e) {
            state.registerRecursion(new CopyRecursionResolver(orig) {
                public void resolve(Object value) {
                    try {
                        setFieldContents(copy, value);
                    } catch (IOException ex) {
                        throw as(InternalError::new, ex, ex.getMessage());
                    }
                }
            });
        } catch (IOException ex) {
            throw as(InternalError::new, ex, ex.getMessage());
        }
    }

    /**
     * @see FieldDescriptor#readFieldIntoMap(ObjectReader, Map)
     */
    void readFieldIntoMap(ObjectReader reader, Map<String, Object> map) {
        Object value = reader.readAbstractObject();
        map.put(java_name, value);
    }

    /**
     * @see FieldDescriptor#writeFieldFromMap(ObjectWriter, Map)
     */
    void writeFieldFromMap(ObjectWriter writer, Map<String, Object> map) throws IOException {
        Object value = map.get(java_name);
        writer.writeObject(value);
    }
}

class BooleanFieldDescriptor extends FieldDescriptor {
    BooleanFieldDescriptor(Class<?> owner, Class<?> type, String name, Field f, TypeRepository repository) {
        super(owner, type, name, f, repository);
    }

    public void read(ObjectReader reader, Object obj) throws IOException {
        boolean value = reader.readBoolean();
        setFieldContents(obj, value);
    }

    public void write(ObjectWriter writer, Object obj) throws IOException {
        writer.writeBoolean((Boolean) getFieldContents(obj));
    }

    void copyState(Object orig, Object copy, CopyState state) {
        try {
            setFieldContents(copy, getFieldContents(orig));
        } catch (IOException ex) {
            throw as(InternalError::new, ex, ex.getMessage());
        }
    }

    void print(PrintWriter pw, Map<Object, Integer> recurse, Object val) {
        try {
            pw.print(java_name);
            pw.print("=");
            pw.print(getFieldContents(val));
        } catch (IllegalStateException | IOException ex) {
            pw.print("<non-local>");
        }
    }

    /**
     * @see FieldDescriptor#readFieldIntoMap(ObjectReader, Map)
     */
    void readFieldIntoMap(ObjectReader reader, Map<String, Object> map) throws IOException {
        map.put(java_name, reader.readBoolean());
    }

    /**
     * @see FieldDescriptor#writeFieldFromMap(ObjectWriter, Map)
     */
    void writeFieldFromMap(ObjectWriter writer, Map<String, Object> map) throws IOException {
        Boolean value = (Boolean) map.get(java_name);
        if (value == null) {
            writer.writeBoolean(false);
        } else {
            writer.writeBoolean(value);
        }
    }
}

class ByteFieldDescriptor extends FieldDescriptor {
    ByteFieldDescriptor(Class<?> owner, Class<?> type, String name, Field f, TypeRepository repository) {
        super(owner, type, name, f, repository);
    }

    public void read(ObjectReader reader, Object obj) throws IOException {
        byte value = reader.readByte();
        setFieldContents(obj, value);
    }

    public void write(ObjectWriter writer, Object obj) throws IOException {
        writer.writeByte((Byte) getFieldContents(obj));
    }

    void copyState(Object orig, Object copy, CopyState state) {
        try {
            setFieldContents(copy, getFieldContents(orig));
        } catch (IOException ex) {
            throw as(InternalError::new, ex, ex.getMessage());
        }
    }

    void print(PrintWriter pw, Map<Object, Integer> recurse, Object val) {
        try {
            pw.print(java_name);
            pw.print("=");
            pw.print(getFieldContents(val));
        } catch (IllegalStateException | IOException ex) {
            pw.print("<non-local>");
        }
    }

    /**
     * @see FieldDescriptor#readFieldIntoMap(ObjectReader, Map)
     */
    void readFieldIntoMap(ObjectReader reader, Map<String, Object> map) throws IOException {
        map.put(java_name, reader.readByte());
    }

    /**
     * @see FieldDescriptor#writeFieldFromMap(ObjectWriter, Map)
     */
    void writeFieldFromMap(ObjectWriter writer, Map<String, Object> map) throws IOException {
        Byte value = (Byte) map.get(java_name);
        if (value == null) {
            writer.writeByte(0);
        } else {
            writer.writeByte(value);
        }
    }
}

class ShortFieldDescriptor extends FieldDescriptor {
    ShortFieldDescriptor(Class<?> owner, Class<?> type, String name, Field f, TypeRepository repository) {
        super(owner, type, name, f, repository);
    }

    public void read(ObjectReader reader, Object obj) throws IOException {
        short value = reader.readShort();
        setFieldContents(obj, value);
    }

    public void write(ObjectWriter writer, Object obj) throws IOException {
        writer.writeShort((Short) getFieldContents(obj));
    }

    void copyState(Object orig, Object copy, CopyState state) {
        try {
            setFieldContents(copy, getFieldContents(orig));
        } catch (IOException ex) {
            throw as(InternalError::new, ex, ex.getMessage());
        }
    }

    void print(PrintWriter pw, Map<Object, Integer> recurse, Object val) {
        try {
            pw.print(java_name);
            pw.print("=");
            pw.print(getFieldContents(val));
        } catch (IllegalStateException | IOException ex) {
            pw.print("<non-local>");
        }
    }

    /**
     * @see FieldDescriptor#readFieldIntoMap(ObjectReader, Map)
     */
    void readFieldIntoMap(ObjectReader reader, Map<String, Object> map) throws IOException {
        map.put(java_name, reader.readShort());
    }

    /**
     * @see FieldDescriptor#writeFieldFromMap(ObjectWriter, Map)
     */
    void writeFieldFromMap(ObjectWriter writer, Map<String, Object> map) throws IOException {
        Short value = (Short) map.get(java_name);
        if (value == null) {
            writer.writeShort(0);
        } else {
            writer.writeShort(value);
        }
    }
}

class CharFieldDescriptor extends FieldDescriptor {
    CharFieldDescriptor(Class<?> owner, Class<?> type, String name, Field f, TypeRepository repository) {
        super(owner, type, name, f, repository);
    }

    public void read(ObjectReader reader, Object obj) throws IOException {
        char value = reader.readChar();
        setFieldContents(obj, value);
    }

    public void write(ObjectWriter writer, Object obj) throws IOException {
        writer.writeChar((Character) getFieldContents(obj));
    }

    void copyState(Object orig, Object copy, CopyState state) {
        try {
            setFieldContents(copy, getFieldContents(orig));
        } catch (IOException ex) {
            throw as(InternalError::new, ex, ex.getMessage());
        }
    }

    void print(PrintWriter pw, Map<Object, Integer> recurse, Object val) {
        try {
            pw.print(java_name);
            pw.print("=");
            char ch = (Character) getFieldContents(val);
            pw.print(ch);
            pw.print('(');
            pw.print(Integer.toHexString(0xffff & ((int) ch)));
            pw.print(')');
        } catch (IllegalStateException | IOException ex) {
            pw.print("<non-local>");
        }
    }

    /**
     * @see FieldDescriptor#readFieldIntoMap(ObjectReader, Map)
     */
    void readFieldIntoMap(ObjectReader reader, Map<String, Object> map) throws IOException {
        map.put(java_name, reader.readChar());
    }

    /**
     * @see FieldDescriptor#writeFieldFromMap(ObjectWriter, Map)
     */
    void writeFieldFromMap(ObjectWriter writer, Map<String, Object> map) throws IOException {
        Character value = (Character) map.get(java_name);
        if (value == null) {
            writer.writeChar(0);
        } else {
            writer.writeChar(value);
        }
    }
}

class IntFieldDescriptor extends FieldDescriptor {
    IntFieldDescriptor(Class<?> owner, Class<?> type, String name, Field f, TypeRepository repository) {
        super(owner, type, name, f, repository);
    }

    public void read(ObjectReader reader, Object obj) throws IOException {
        int value = reader.readInt();
        logger.finest(() -> "Read int field value " + value);
        setFieldContents(obj, value);
    }

    public void write(ObjectWriter writer, Object obj) throws IOException {
        writer.writeInt((Integer) getFieldContents(obj));
    }

    void copyState(Object orig, Object copy, CopyState state) {
        try {
            setFieldContents(copy, getFieldContents(orig));
        } catch (IOException ex) {
            throw as(InternalError::new, ex, ex.getMessage());
        }
    }

    void print(PrintWriter pw, Map<Object, Integer> recurse, Object val) {
        try {
            pw.print(java_name);
            pw.print("=");
            pw.print(getFieldContents(val));
        } catch (IllegalStateException | IOException ex) {
            pw.print("<non-local>");
        }
    }

    /**
     * @see FieldDescriptor#readFieldIntoMap(ObjectReader, Map)
     */
    void readFieldIntoMap(ObjectReader reader, Map<String, Object> map) throws IOException {
        map.put(java_name, reader.readInt());
    }

    /**
     * @see FieldDescriptor#writeFieldFromMap(ObjectWriter, Map)
     */
    void writeFieldFromMap(ObjectWriter writer, Map<String, Object> map) throws IOException {
        Integer value = (Integer) map.get(java_name);
        if (value == null) {
            writer.writeInt(0);
        } else {
            writer.writeInt(value);
        }
    }
}

class LongFieldDescriptor extends FieldDescriptor {
    LongFieldDescriptor(Class<?> owner, Class<?> type, String name, Field f, TypeRepository repository) {
        super(owner, type, name, f, repository);
    }

    public void read(ObjectReader reader, Object obj) throws IOException {
        long value = reader.readLong();
        logger.finest(() -> "Read long field value " + value);
        setFieldContents(obj, value);
    }

    public void write(ObjectWriter writer, Object obj) throws IOException {
        writer.writeLong((Long) getFieldContents(obj));
    }

    void copyState(Object orig, Object copy, CopyState state) {
        try {
            setFieldContents(copy, getFieldContents(orig));
        } catch (IOException ex) {
            throw as(InternalError::new, ex, ex.getMessage());
        }
    }

    void print(PrintWriter pw, Map<Object, Integer> recurse, Object val) {
        try {
            pw.print(java_name);
            pw.print("=");
            pw.print(getFieldContents(val));
        } catch (IllegalStateException | IOException ex) {
            pw.print("<non-local>");
        }
    }

    /**
     * @see FieldDescriptor#readFieldIntoMap(ObjectReader, Map)
     */
    void readFieldIntoMap(ObjectReader reader, Map<String, Object> map) throws IOException {
        map.put(java_name, reader.readLong());
    }

    /**
     * @see FieldDescriptor#writeFieldFromMap(ObjectWriter, Map)
     */
    void writeFieldFromMap(ObjectWriter writer, Map<String, Object> map) throws IOException {
        Long value = (Long) map.get(java_name);
        if (value == null) {
            writer.writeLong(0);
        } else {
            writer.writeLong(value);
        }
    }
}

class FloatFieldDescriptor extends FieldDescriptor {
    FloatFieldDescriptor(Class<?> owner, Class<?> type, String name, Field f, TypeRepository repository) {
        super(owner, type, name, f, repository);
    }

    public void read(ObjectReader reader, Object obj) throws IOException {
        float value = reader.readFloat();
        setFieldContents(obj, value);
    }

    public void write(ObjectWriter writer, Object obj) throws IOException {
        writer.writeFloat((Float) getFieldContents(obj));
    }

    void copyState(Object orig, Object copy, CopyState state) {
        try {
            setFieldContents(copy, getFieldContents(orig));
        } catch (IOException ex) {
            throw as(InternalError::new, ex, ex.getMessage());
        }
    }

    void print(PrintWriter pw, Map<Object, Integer> recurse, Object val) {
        try {
            pw.print(java_name);
            pw.print("=");
            pw.print(getFieldContents(val));
        } catch (IllegalStateException | IOException ex) {
            pw.print("<non-local>");
        }
    }

    /**
     * @see FieldDescriptor#readFieldIntoMap(ObjectReader, Map)
     */
    void readFieldIntoMap(ObjectReader reader, Map<String, Object> map) throws IOException {
        Float value = reader.readFloat();
        map.put(java_name, value);
    }

    /**
     * @see FieldDescriptor#writeFieldFromMap(ObjectWriter, Map)
     */
    void writeFieldFromMap(ObjectWriter writer, Map<String, Object> map) throws IOException {
        Float value = (Float) map.get(java_name);
        if (value == null) {
            writer.writeFloat(0.0F);
        } else {
            writer.writeFloat(value);
        }
    }

}

class DoubleFieldDescriptor extends FieldDescriptor {
    DoubleFieldDescriptor(Class<?> owner, Class<?> type, String name, Field f, TypeRepository repository) {
        super(owner, type, name, f, repository);
    }

    public void read(ObjectReader reader, Object obj) throws IOException {
        double value = reader.readDouble();
        setFieldContents(obj, value);
    }

    public void write(ObjectWriter writer, Object obj) throws IOException {
        writer.writeDouble((Double) getFieldContents(obj));
    }

    void copyState(Object orig, Object copy, CopyState state) {
        try {
            setFieldContents(copy, getFieldContents(orig));
        } catch (IOException ex) {
            throw as(InternalError::new, ex, ex.getMessage());
        }
    }

    void print(PrintWriter pw, Map<Object, Integer> recurse, Object val) {
        try {
            pw.print(java_name);
            pw.print("=");
            pw.print(getFieldContents(val));
        } catch (IllegalStateException | IOException ex) {
            pw.print("<non-local>");
        }
    }

    /**
     * @see FieldDescriptor#readFieldIntoMap(ObjectReader, Map)
     */
    void readFieldIntoMap(ObjectReader reader, Map<String, Object> map) throws IOException {
        Double value = reader.readDouble();
        map.put(java_name, value);
    }

    /**
     * @see FieldDescriptor#writeFieldFromMap(ObjectWriter, Map)
     */
    void writeFieldFromMap(ObjectWriter writer, Map<String, Object> map) throws IOException {
        Double value = (Double) map.get(java_name);
        if (value == null) {
            writer.writeDouble(0.0D);
        } else {
            writer.writeDouble(value);
        }
    }

}

class CorbaObjectFieldDescriptor extends FieldDescriptor {

    protected CorbaObjectFieldDescriptor(Class<?> owner, Class<?> type, String name, Field f,TypeRepository repository) {
        super(owner, type, name, f, repository);
    }

    void copyState(final Object orig, final Object copy, CopyState state) {
        try {
            setFieldContents(copy, state.copy(getFieldContents(orig)));
        } catch (CopyRecursionException e) {
            state.registerRecursion(new CopyRecursionResolver(orig) {
                public void resolve(Object value) {
                    try {
                        setFieldContents(copy, value);
                    } catch (IOException ex) {
                        throw as(InternalError::new, ex, ex.getMessage());
                    }
                }
            });
        } catch (IOException ex) {
            throw as(InternalError::new, ex, ex.getMessage());
        }
    }

    void read(ObjectReader reader, Object obj) throws IOException {
        Object value = reader.readCorbaObject(type);
        setFieldContents(obj, value);
    }

    void readFieldIntoMap(ObjectReader reader, Map<String, Object> map) {
        Object value = reader.readCorbaObject(type);
        map.put(java_name, value);

    }

    void write(ObjectWriter writer, Object obj) throws IOException {
        writer.writeCorbaObject(getFieldContents(obj));
    }

    void writeFieldFromMap(ObjectWriter writer, Map<String, Object> map) throws IOException {
        org.omg.CORBA.Object value = (org.omg.CORBA.Object) map.get(java_name);
        writer.writeCorbaObject(value);
    }
}
