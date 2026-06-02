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
import java.lang.reflect.Field;
import java.rmi.Remote;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import static java.lang.reflect.Modifier.isFinal;
import static java.lang.reflect.Modifier.isPublic;
import static java.util.Collections.unmodifiableMap;
import static java.util.logging.Level.FINER;
import static org.apache.yoko.util.Exceptions.as;
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

    private final org.apache.yoko.rmi.util.corba.Field field;

    final Class<?> type;

    final Class<?> declaringClass;

    final boolean isFinal;

    private final LazyReference<ValueMember> valueMemberRef = new LazyReference<>(this::genValueMember);

    private final ValueMemberAccess valueMemberAccess;

    protected FieldDescriptor(Class<?> owner, Class<?> type, String name, Field f, TypeRepository repo) {
        super(repo, name);
        this.type = type;
        declaringClass = owner;

        if (null == f) {
            this.valueMemberAccess = ValueMemberAccess.PRIVATE;
            this.field = null;
            isFinal = false;
        } else {
            int modifiers = f.getModifiers();
            this.valueMemberAccess = isPublic(modifiers) ? ValueMemberAccess.PUBLIC : ValueMemberAccess.PRIVATE;
            this.field = new org.apache.yoko.rmi.util.corba.Field(f);
            isFinal = isFinal(modifiers);
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

    org.apache.yoko.rmi.util.corba.Field getField() {
        return Optional.ofNullable(field)
                .orElseThrow(() -> new IllegalStateException("cannot read/write using serialPersistentFields"));
    }

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
            Object obj = getField().get(val);
            if (obj == null) {
                pw.print("null");
            } else {
                TypeDescriptor desc = repo.getDescriptor(obj.getClass());
                desc.print(pw, recurse, obj);
            }
        } catch (IllegalStateException ex) {
            pw.print("<non-local>");
        } catch (IllegalAccessException ex) {
            /*
             * } catch (RuntimeException ex) { System.err.println
             * ("SystemException in FieldDescriptor "+field); System.err.println
             * ("value = "+val); ex.printStackTrace ();
             */
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
        try {
            Object value = reader.readRemoteObject(interfaceType);
            getField().set(obj, value);
        } catch (IllegalAccessException | IllegalStateException ex) {
            throw as(IOException::new, ex, ex.getMessage());
        }
    }

    public void write(ObjectWriter writer, Object obj) throws IOException {
        try {
            writer.writeRemoteObject(getField().get(obj));
        } catch (IllegalAccessException | IllegalStateException ex) {
            throw as(IOException::new, ex, ex.getMessage());
        }
    }

    void copyState(final Object orig, final Object copy, CopyState state) {
        final org.apache.yoko.rmi.util.corba.Field field;
        try {
            field = getField();
        } catch (IllegalStateException ex) {
            throw as(InternalError::new, ex, ex.getMessage());
        }
        try {
            field.set(copy, state.copy(field.get(orig)));
        } catch (CopyRecursionException e) {
            state.registerRecursion(new CopyRecursionResolver(orig) {
                public void resolve(Object value) {
                    try {
                        field.set(copy, value);
                    } catch (IllegalAccessException ex) {
                        throw as(InternalError::new, ex, ex.getMessage());
                    }
                }
            });
        } catch (IllegalAccessException ex) {
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

            getField().set(obj, val);
        } catch (IllegalAccessException | IllegalStateException ex) {
            throw as(MARSHAL::new, ex, ex.getMessage());
        }
    }

    public void write(ObjectWriter writer, Object obj)
            throws IOException {
        try {
            writer.writeAny(getField().get(obj));
        } catch (IllegalAccessException | IllegalStateException ex) {
            throw as(IOException::new, ex, ex.getMessage());
        }
    }

    void copyState(final Object orig, final Object copy, CopyState state) {
        final org.apache.yoko.rmi.util.corba.Field field;
        try {
            field = getField();
        } catch (IllegalStateException ex) {
            throw as(InternalError::new, ex, ex.getMessage());
        }
        try {
            field.set(copy, state.copy(field.get(orig)));
        } catch (CopyRecursionException e) {
            state.registerRecursion(new CopyRecursionResolver(orig) {
                public void resolve(Object value) {
                    try {
                        field.set(copy, value);
                    } catch (IllegalAccessException ex) {
                        throw as(InternalError::new, ex, ex.getMessage());
                    }
                }
            });
        } catch (IllegalAccessException ex) {
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
            getField().set(obj, value);
        } catch (IllegalAccessException | IllegalStateException ex) {
            throw as(MARSHAL::new, ex, ex.getMessage());
        }
    }

    public void write(ObjectWriter writer, Object obj) throws IOException {
        try {
            if (NON_SERIALIZABLE_FIELD_IS_ABSTRACT_VALUE.isSupported()
                    || type.isInterface()
                    || Serializable.class.isAssignableFrom(type)) {
                try {
                    writer.writeValueObject(getField().get(obj));
                } catch (SystemException e) {
                    throw e;
                } catch (Exception e) {
                    throw as(MARSHAL::new, e, "Object of class " + obj.getClass().getName() + " is not a valuetype");
                }
            } else {
                // older versions of Yoko treat non-serializable classes as abstract objects
                writer.writeObject(getField().get(obj));
            }
        } catch (IllegalAccessException | IllegalStateException ex) {
            throw as(IOException::new, ex, ex.getMessage());
        }
    }

    void copyState(final Object orig, final Object copy, CopyState state) {
        final org.apache.yoko.rmi.util.corba.Field field;
        try {
            field = getField();
        } catch (IllegalStateException ex) {
            throw as(InternalError::new, ex, ex.getMessage());
        }
        try {
            field.set(copy, state.copy(field.get(orig)));
        } catch (CopyRecursionException e) {
            state.registerRecursion(new CopyRecursionResolver(orig) {
                public void resolve(Object value) {
                    try {
                        field.set(copy, value);
                    } catch (IllegalAccessException ex) {
                        throw as(InternalError::new, ex, ex.getMessage());
                    }
                }
            });
        } catch (IllegalAccessException ex) {
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
        try {
            String value = (String) reader.readValueObject();
            getField().set(obj, value);
        } catch (IllegalAccessException | IllegalStateException ex) {
            throw as(IOException::new, ex, ex.getMessage());
        }
    }

    public void write(ObjectWriter writer, Object obj) throws IOException {
        try {
            writer.writeValueObject(getField().get(obj));
        } catch (IllegalAccessException | IllegalStateException ex) {
            throw as(IOException::new, ex, ex.getMessage());
        }
    }

    void copyState(Object orig, Object copy, CopyState state) {
        try {
            org.apache.yoko.rmi.util.corba.Field field = getField();
            field.set(copy, field.get(orig));
        } catch (IllegalAccessException | IllegalStateException ex) {
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
            getField().set(obj, value);
        } catch (IllegalAccessException | IllegalStateException ex) {
            throw as(MARSHAL::new, ex, ex.getMessage());
        }
    }

    public void write(ObjectWriter writer, Object obj) throws IOException {
        try {
            writer.writeObject(getField().get(obj));
        } catch (IllegalAccessException | IllegalStateException ex) {
            throw as(IOException::new, ex, ex.getMessage());
        }
    }

    void copyState(final Object orig, final Object copy, CopyState state) {
        final org.apache.yoko.rmi.util.corba.Field field;
        try {
            field = getField();
        } catch (IllegalStateException ex) {
            throw as(InternalError::new, ex, ex.getMessage());
        }
        try {
            field.set(copy, state.copy(field.get(orig)));
        } catch (CopyRecursionException e) {
            state.registerRecursion(new CopyRecursionResolver(orig) {
                public void resolve(Object value) {
                    try {
                        field.set(copy, value);
                    } catch (IllegalAccessException ex) {
                        throw as(InternalError::new, ex, ex.getMessage());
                    }
                }
            });
        } catch (IllegalAccessException ex) {
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
        try {
            boolean value = reader.readBoolean();
            getField().setBoolean(obj, value);
        } catch (IllegalAccessException | IllegalStateException ex) {
            throw as(IOException::new, ex, ex.getMessage());
        }
    }

    public void write(ObjectWriter writer, Object obj) throws IOException {
        try {
            writer.writeBoolean(getField().getBoolean(obj));
        } catch (IllegalAccessException | IllegalStateException ex) {
            throw as(IOException::new, ex, ex.getMessage());
        }
    }

    void copyState(Object orig, Object copy, CopyState state) {
        try {
            org.apache.yoko.rmi.util.corba.Field field = getField();
            field.setBoolean(copy, field.getBoolean(orig));
        } catch (IllegalAccessException | IllegalStateException ex) {
            throw as(InternalError::new, ex, ex.getMessage());
        }
    }

    void print(PrintWriter pw, Map<Object, Integer> recurse, Object val) {
        try {
            pw.print(java_name);
            pw.print("=");
            pw.print(getField().getBoolean(val));
        } catch (IllegalStateException ex) {
            pw.print("<non-local>");
        } catch (IllegalAccessException ignored) {
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
        try {
            byte value = reader.readByte();
            getField().setByte(obj, value);
        } catch (IllegalAccessException | IllegalStateException ex) {
            throw as(IOException::new, ex, ex.getMessage());
        }
    }

    public void write(ObjectWriter writer, Object obj) throws IOException {
        try {
            writer.writeByte(getField().getByte(obj));
        } catch (IllegalAccessException | IllegalStateException ex) {
            throw as(IOException::new, ex, ex.getMessage());
        }
    }

    void copyState(Object orig, Object copy, CopyState state) {
        try {
            org.apache.yoko.rmi.util.corba.Field field = getField();
            field.setByte(copy, field.getByte(orig));
        } catch (IllegalAccessException | IllegalStateException ex) {
            throw as(InternalError::new, ex, ex.getMessage());
        }
    }

    void print(PrintWriter pw, Map<Object, Integer> recurse, Object val) {
        try {
            pw.print(java_name);
            pw.print("=");
            pw.print(getField().getByte(val));
        } catch (IllegalStateException ex) {
            pw.print("<non-local>");
        } catch (IllegalAccessException ignored) {
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
        try {
            short value = reader.readShort();
            getField().setShort(obj, value);
        } catch (IllegalAccessException | IllegalStateException ex) {
            throw as(IOException::new, ex, ex.getMessage());
        }
    }

    public void write(ObjectWriter writer, Object obj) throws IOException {
        try {
            writer.writeShort(getField().getShort(obj));
        } catch (IllegalAccessException | IllegalStateException ex) {
            throw as(IOException::new, ex, ex.getMessage());
        }
    }

    void copyState(Object orig, Object copy, CopyState state) {
        try {
            org.apache.yoko.rmi.util.corba.Field field = getField();
            field.setShort(copy, field.getShort(orig));
        } catch (IllegalAccessException | IllegalStateException ex) {
            throw as(InternalError::new, ex, ex.getMessage());
        }
    }

    void print(PrintWriter pw, Map<Object, Integer> recurse, Object val) {
        try {
            pw.print(java_name);
            pw.print("=");
            pw.print(getField().getShort(val));
        } catch (IllegalStateException ex) {
            pw.print("<non-local>");
        } catch (IllegalAccessException ignored) {
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
        try {
            char value = reader.readChar();
            getField().setChar(obj, value);
        } catch (IllegalAccessException | IllegalStateException ex) {
            throw as(IOException::new, ex, ex.getMessage());
        }
    }

    public void write(ObjectWriter writer, Object obj) throws IOException {
        try {
            writer.writeChar(getField().getChar(obj));
        } catch (IllegalAccessException | IllegalStateException ex) {
            throw as(IOException::new, ex, ex.getMessage());
        }
    }

    void copyState(Object orig, Object copy, CopyState state) {
        try {
            org.apache.yoko.rmi.util.corba.Field field = getField();
            field.setChar(copy, field.getChar(orig));
        } catch (IllegalAccessException | IllegalStateException ex) {
            throw as(InternalError::new, ex, ex.getMessage());
        }
    }

    void print(PrintWriter pw, Map<Object, Integer> recurse, Object val) {
        try {
            pw.print(java_name);
            pw.print("=");
            char ch = getField().getChar(val);
            pw.print(ch);
            pw.print('(');
            pw.print(Integer.toHexString(0xffff & ((int) ch)));
            pw.print(')');
        } catch (IllegalStateException ex) {
            pw.print("<non-local>");
        } catch (IllegalAccessException ignored) {
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
        try {
            int value = reader.readInt();
            logger.finest(() -> "Read int field value " + value);
            getField().setInt(obj, value);
        } catch (IllegalAccessException | IllegalStateException ex) {
            throw as(IOException::new, ex, ex.getMessage());
        }
    }

    public void write(ObjectWriter writer, Object obj) throws IOException {
        try {
            writer.writeInt(getField().getInt(obj));
        } catch (IllegalAccessException | IllegalStateException ex) {
            throw as(IOException::new, ex, ex.getMessage());
        }
    }

    void copyState(Object orig, Object copy, CopyState state) {
        try {
            org.apache.yoko.rmi.util.corba.Field field = getField();
            field.setInt(copy, field.getInt(orig));
        } catch (IllegalAccessException | IllegalStateException ex) {
            throw as(InternalError::new, ex, ex.getMessage());
        }
    }

    void print(PrintWriter pw, Map<Object, Integer> recurse, Object val) {
        try {
            pw.print(java_name);
            pw.print("=");
            pw.print(getField().getInt(val));
        } catch (IllegalStateException ex) {
            pw.print("<non-local>");
        } catch (IllegalAccessException ignored) {
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
        try {
            long value = reader.readLong();
            logger.finest(() -> "Read long field value " + value);
            getField().setLong(obj, value);
        } catch (IllegalAccessException | IllegalStateException ex) {
            throw as(IOException::new, ex, ex.getMessage());
        }
    }

    public void write(ObjectWriter writer, Object obj) throws IOException {
        try {
            writer.writeLong(getField().getLong(obj));
        } catch (IllegalAccessException | IllegalStateException ex) {
            throw as(IOException::new, ex, ex.getMessage());
        }
    }

    void copyState(Object orig, Object copy, CopyState state) {
        try {
            org.apache.yoko.rmi.util.corba.Field field = getField();
            field.setLong(copy, field.getLong(orig));
        } catch (IllegalAccessException | IllegalStateException ex) {
            throw as(InternalError::new, ex, ex.getMessage());
        }
    }

    void print(PrintWriter pw, Map<Object, Integer> recurse, Object val) {
        try {
            pw.print(java_name);
            pw.print("=");
            pw.print(getField().getLong(val));
        } catch (IllegalStateException ex) {
            pw.print("<non-local>");
        } catch (IllegalAccessException ignored) {
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
        try {
            float value = reader.readFloat();
            getField().setFloat(obj, value);
        } catch (IllegalAccessException | IllegalStateException ex) {
            throw as(IOException::new, ex, ex.getMessage());
        }
    }

    public void write(ObjectWriter writer, Object obj) throws IOException {
        try {
            writer.writeFloat(getField().getFloat(obj));
        } catch (IllegalAccessException | IllegalStateException ex) {
            throw as(IOException::new, ex, ex.getMessage());
        }
    }

    void copyState(Object orig, Object copy, CopyState state) {
        try {
            org.apache.yoko.rmi.util.corba.Field field = getField();
            field.setFloat(copy, field.getFloat(orig));
        } catch (IllegalAccessException | IllegalStateException ex) {
            throw as(InternalError::new, ex, ex.getMessage());
        }
    }

    void print(PrintWriter pw, Map<Object, Integer> recurse, Object val) {
        try {
            pw.print(java_name);
            pw.print("=");
            pw.print(getField().getFloat(val));
        } catch (IllegalStateException ex) {
            pw.print("<non-local>");
        } catch (IllegalAccessException ignored) {
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
        try {
            double value = reader.readDouble();
            getField().setDouble(obj, value);
        } catch (IllegalAccessException | IllegalStateException ex) {
            throw as(IOException::new, ex, ex.getMessage());
        }
    }

    public void write(ObjectWriter writer, Object obj) throws IOException {
        try {
            writer.writeDouble(getField().getDouble(obj));
        } catch (IllegalAccessException | IllegalStateException ex) {
            throw as(IOException::new, ex, ex.getMessage());
        }
    }

    void copyState(Object orig, Object copy, CopyState state) {
        try {
            org.apache.yoko.rmi.util.corba.Field field = getField();
            field.setDouble(copy, field.getDouble(orig));
        } catch (IllegalAccessException | IllegalStateException ex) {
            throw as(InternalError::new, ex, ex.getMessage());
        }
    }

    void print(PrintWriter pw, Map<Object, Integer> recurse, Object val) {
        try {
            pw.print(java_name);
            pw.print("=");
            pw.print(getField().getDouble(val));
        } catch (IllegalStateException ex) {
            pw.print("<non-local>");
        } catch (IllegalAccessException ignored) {
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
        final org.apache.yoko.rmi.util.corba.Field field;
        try {
            field = getField();
        } catch (IllegalStateException ex) {
            throw as(InternalError::new, ex, ex.getMessage());
        }
        try {
            field.set(copy, state.copy(field.get(orig)));
        } catch (CopyRecursionException e) {
            state.registerRecursion(new CopyRecursionResolver(orig) {
                public void resolve(Object value) {
                    try {
                        field.set(copy, value);
                    } catch (IllegalAccessException ex) {
                        throw as(InternalError::new, ex, ex.getMessage());
                    }
                }
            });
        } catch (IllegalAccessException ex) {
            throw as(InternalError::new, ex, ex.getMessage());
        }
    }

    void read(ObjectReader reader, Object obj) throws IOException {
        Object value = reader.readCorbaObject(null);
        try {
            getField().set(obj, value);
        } catch (IllegalAccessException e) {
            throw as(IOException::new, e, e.getMessage());
        }
    }

    void readFieldIntoMap(ObjectReader reader, Map<String, Object> map) {
        Object value = reader.readCorbaObject(null);
        map.put(java_name, value);

    }

    void write(ObjectWriter writer, Object obj) throws IOException {
        try {
            writer.writeCorbaObject(getField().get(obj));
        }
        catch(IllegalAccessException e) {
            throw as(IOException::new, e, e.getMessage());
        }
    }

    void writeFieldFromMap(ObjectWriter writer, Map<String, Object> map) throws IOException {
        org.omg.CORBA.Object value = (org.omg.CORBA.Object) map.get(java_name);
        writer.writeCorbaObject(value);
    }
}
