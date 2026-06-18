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
import org.omg.CORBA.ValueMember;

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

