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
import org.omg.CORBA.AttributeDescription;
import org.omg.CORBA.Initializer;
import org.omg.CORBA.MARSHAL;
import org.omg.CORBA.ORB;
import org.omg.CORBA.OperationDescription;
import org.omg.CORBA.TypeCode;
import org.omg.CORBA.VM_NONE;
import org.omg.CORBA.ValueDefPackage.FullValueDescription;
import org.omg.CORBA.ValueMember;
import org.omg.CORBA.portable.InputStream;
import org.omg.CORBA.portable.OutputStream;
import org.omg.CORBA.portable.UnknownException;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.ObjectStreamField;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.rmi.Remote;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivilegedActionException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static java.security.AccessController.doPrivileged;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableSet;
import static java.util.Comparator.comparing;
import static java.util.function.Function.identity;
import static java.util.logging.Level.WARNING;
import static java.util.stream.Collectors.collectingAndThen;
import static org.apache.yoko.io.Buffer.createReadBuffer;
import static org.apache.yoko.logging.VerboseLogging.MARSHAL_IN_LOG;
import static org.apache.yoko.logging.VerboseLogging.MARSHAL_LOG;
import static org.apache.yoko.logging.VerboseLogging.MARSHAL_OUT_LOG;
import static org.apache.yoko.rmi.impl.FieldDescriptor.getForSerialPersistentField;
import static org.apache.yoko.rmi.impl.RemoteDescriptor.genMostSpecificRemoteInterface;
import static org.apache.yoko.rmi.util.StringUtil.convertToValidIDLNames;
import static org.apache.yoko.util.Exceptions.as;
import static org.apache.yoko.util.PrivilegedActions.exAction;
import static org.apache.yoko.util.PrivilegedActions.getDeclaredField;
import static org.apache.yoko.util.PrivilegedActions.getDeclaredFields;
import static org.apache.yoko.util.PrivilegedActions.getDeclaredMethod;
import static org.apache.yoko.util.PrivilegedActions.getField;
import static org.apache.yoko.util.PrivilegedActions.getNoArgConstructor;
import static org.apache.yoko.util.PrivilegedActions.makeAccessible;
import static sun.reflect.ReflectionFactory.getReflectionFactory;

class ValueDescriptor extends TypeDescriptor {
    private final LazyReference<Function<Serializable, Serializable>> writeReplacerRef = new LazyReference<>(this::genWriteReplacer);

    private final LazyReference<Function<Serializable, Serializable>> readResolverRef = new LazyReference<>(this::genReadResolver);

    private final LazyReference<Supplier<Serializable>> blankInstanceSupplierRef = new LazyReference<>(this::genBlankInstanceSupplier);

    private final LazyReference<MethodHandle> writeObjectHandleRef = new LazyReference<>(this::genWriteObjectHandle);

    private final LazyReference<MethodHandle> readObjectHandleRef = new LazyReference<>(this::genReadObjectHandle);

    private final LazyReference<Boolean> customMarshalledRef = new LazyReference<>(this::genCustomMarshalled);

    private final LazyReference<Boolean> chunkedRef = new LazyReference<>(this::genChunked);

    private final LazyReference<Long> serialVersionUidRef = new LazyReference<>(this::genSerialVersionUid);

    private final LazyReference<ValueWriter> valueWriterRef = new LazyReference<>(this::genValueWriter);
    private final LazyReference<ValueReader> valueReaderRef;

    @FunctionalInterface
    interface ValueReader extends BiFunction<ObjectReader, Serializable, Serializable> {}

    private final LazyReference<ValueDescriptor> superDescriptorRef = new LazyReference<>(this::genSuperDescriptor);

    protected final LazyReference<List<FieldDescriptor>> fieldsRef = new LazyReference<>(this::genFields);

    private final LazyReference<Boolean> immutableValueRef = new LazyReference<>(this::genImmutableValue);

    private final LazyReference<String> customRepIdRef = new LazyReference<>(this::genCustomRepId);

    private static final Set<? extends Class<? extends Serializable>> IMMUTABLE_VALUE_CLASSES = unmodifiableSet(new HashSet<>(asList(Integer.class,
            Character.class, Boolean.class, Byte.class, Long.class, Float.class, Double.class, Short.class)));

    ValueDescriptor(Class<?> type, TypeRepository repository) {
        this(type, repository, null, null, null);
    }

    ValueDescriptor(Class<?> type, TypeRepository repository, ReadFn readFn, WriteFn writeFn) {
        this(type, repository, readFn, writeFn, null);
    }

    ValueDescriptor(Class<?> type, TypeRepository repository, WriteFn writeFn, Supplier<ValueReader> readerSuppler) {
        this(type, repository, null, writeFn, readerSuppler);
    }

    private ValueDescriptor(Class<?> type, TypeRepository repository, ReadFn readFn, WriteFn writeFn, Supplier<ValueReader> readerSuppler) {
        super(type, repository,
            null == readFn ? genVanillaReadFn(type) : readFn,
            null == writeFn ? ValueDescriptor::vanillaWriteFn : writeFn);
        valueReaderRef = new LazyReference<>(null == readerSuppler ? this::genValueReader : readerSuppler);
    }

    private static ReadFn genVanillaReadFn(Class<?> type) {
        return in -> ((org.omg.CORBA_2_3.portable.InputStream) in).read_value(type);
    }

    private static void vanillaWriteFn(java.io.OutputStream out, Object val) {
        ((org.omg.CORBA_2_3.portable.OutputStream) out).write_value((Serializable) val);
    }

    protected boolean isEnum() { return false; }

    boolean isExternalizable() { return Externalizable.class.isAssignableFrom(getType()); }

    private boolean isSerializable() { return Serializable.class.isAssignableFrom(getType()); }

    private boolean isRmiStub() { return RMIStub.class.isAssignableFrom(getType()); }

    private boolean isImmutableValue() { return immutableValueRef.get(); }

    private boolean genImmutableValue() {
        return IMMUTABLE_VALUE_CLASSES.contains(getType());
    }

    @Override
    final RemoteInterfaceDescriptor genRemoteInterface() {
        Class<?> type = getType();
        return Remote.class.isAssignableFrom(type) ?
                genMostSpecificRemoteInterface(type, repo) :
                super.genRemoteInterface();
    }

    @Override
    String genRepId() {
        long hashCode = getClassHash();
        return String.format("RMI:%s:%016X:%016X", convertToValidIDLNames(getType().getName()), hashCode, getSerialVersionUid());
    }

    private String genCustomRepId() {
        return String.format("RMI:org.omg.custom.%s", getRepositoryID().substring(4));
    }

    final String getCustomRepositoryID() {
        return customRepIdRef.get();
    }

    final long getSerialVersionUid() {
        return serialVersionUidRef.get();
    }

    long genSerialVersionUid() {
        return Optional.ofNullable(findSerialVersionUIDField())
                .map(field -> {
                    try {
                        return field.getLong(null);
                    } catch (IllegalAccessException ex) {
                        return null;
                    }
                })
                .orElseGet(() -> Optional.ofNullable(ObjectStreamClass.lookup(getType()))
                        .map(ObjectStreamClass::getSerialVersionUID)
                        .orElse(0L));
    }

    /**
     * Filters out static and transient fields.
     */
    private boolean isSerializableField(Field f) {
        int mod = f.getModifiers();
        return !Modifier.isStatic(mod) && !Modifier.isTransient(mod);
    }

    ValueDescriptor genSuperDescriptor() {
        return Optional.ofNullable(getType().getSuperclass())
                .filter(sc -> sc != Object.class)
                .map(repo::getDescriptor)
                .filter(ValueDescriptor.class::isInstance)
                .map(ValueDescriptor.class::cast)
                .orElse(null);
    }

    final ValueDescriptor getSuperDescriptor() {
        return superDescriptorRef.get();
    }

    private ValueReader genSuperReader() {
        return Optional.ofNullable(getSuperDescriptor())
                .map(this::createSuperValueReader)
                .orElse((reader, val) -> val);
    }

    private ValueReader createSuperValueReader(ValueDescriptor superDesc) {
        return (reader, val) -> {
            try {
                return superDesc.readValue(reader, val);
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        };
    }


    Function<Serializable, Serializable> genWriteReplacer() {
        Method found = null;
        for (Class<?> curr = getType(); curr != null; curr = curr.getSuperclass()) {
            try {
                found = doPrivileged(getDeclaredMethod(curr, "writeReplace"));
                doPrivileged(makeAccessible(found));
                break;
            } catch (Exception ignored) {
            }
        }

        if (null == found) return identity();

        MethodHandle handle;
        try {
            handle = MethodHandles.lookup().unreflect(found);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot create MethodHandle for writeReplace", e);
        }

        return val -> {
            try {
                return (Serializable) handle.invoke(val);
            } catch (Error | RuntimeException e) {
                throw e;
            } catch (Throwable t) {
                throw as(UnknownException::new, t, t);
            }
        };
    }

    Function<Serializable, Serializable> genReadResolver() {
        Method method;
        try {
            method = doPrivileged(getDeclaredMethod(getType(), "readResolve"));
            doPrivileged(makeAccessible(method));
        } catch (Exception ignored) {
            return identity();
        }

        MethodHandle handle;
        try {
            handle = MethodHandles.lookup().unreflect(method);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot create MethodHandle for readResolve", e);
        }

        return val -> {
            try {
                return (Serializable) handle.invoke(val);
            } catch (Error | RuntimeException e) {
                throw e;
            } catch (Throwable t) {
                throw as(UnknownException::new, t, t);
            }
        };
    }



    MethodHandle genWriteObjectHandle() {
        Class<?> type = getType();
        try {
            Method method = doPrivileged(getDeclaredMethod(type, "writeObject", ObjectOutputStream.class));

            // Validate the method
            int modifiers = method.getModifiers();
            if (!Modifier.isPrivate(modifiers)
                    || Modifier.isStatic(modifiers)
                    || method.getDeclaringClass() != type) {
                return null;
            }

            doPrivileged(makeAccessible(method));
            return MethodHandles.lookup().unreflect(method);
        } catch (PrivilegedActionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof NoSuchMethodException) {
                return null;
            }
            throw as(RuntimeException::new, cause, "Cannot create MethodHandle for writeObject");
        } catch (IllegalAccessException e) {
            throw as(RuntimeException::new, e, "Cannot create MethodHandle for writeObject");
        }
    }

    private Optional<MethodHandle> getOptionalReadObjectHandle() {
        return Optional.ofNullable(readObjectHandleRef.get());
    }

    private Optional<MethodHandle> getOptionalWriteObjectHandle() {
        return Optional.ofNullable(writeObjectHandleRef.get());
    }

    MethodHandle genReadObjectHandle() {
        Class<?> type = getType();
        try {
            Method method = doPrivileged(getDeclaredMethod(type, "readObject", ObjectInputStream.class));

            // Validate the method
            int modifiers = method.getModifiers();
            if (!Modifier.isPrivate(modifiers)
                    || Modifier.isStatic(modifiers)
                    || method.getDeclaringClass() != type) {
                return null;
            }

            doPrivileged(makeAccessible(method));
            return MethodHandles.lookup().unreflect(method);
        } catch (PrivilegedActionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof NoSuchMethodException) {
                return null;
            }
            throw as(RuntimeException::new, cause, "Cannot create MethodHandle for readObject");
        } catch (IllegalAccessException e) {
            throw as(RuntimeException::new, e, "Cannot create MethodHandle for readObject");
        }
    }




    private Field findSerialVersionUIDField() {
        try {
            Field field = doPrivileged(getDeclaredField(getType(), "serialVersionUID"));
            if (Modifier.isStatic(field.getModifiers())) {
                return doPrivileged(makeAccessible(field));
            }
            return null;
        } catch (Exception ignored) {
            return null;
        }
    }

    ObjectStreamField[] findSerialPersistentFields() {
        try {
            Field field = doPrivileged(getDeclaredField(getType(), "serialPersistentFields"));
            field = doPrivileged(makeAccessible(field));
            return (ObjectStreamField[]) field.get(null);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Supplier<Serializable> genBlankInstanceSupplier() {
        return findConstructor()
                .map(constructor -> (Supplier<Serializable>) () -> {
                    try {
                        return (Serializable) constructor.newInstance();
                    } catch (IllegalAccessException ex) {
                        throw as(MARSHAL::new, ex, "cannot call " + constructor);
                    } catch (IllegalArgumentException | InstantiationException ex) {
                        throw as(MARSHAL::new, ex, ex.getMessage());
                    } catch (InvocationTargetException ex) {
                        final Throwable t = ex.getTargetException();
                        throw as(UnknownException::new, t, t);
                    } catch (NullPointerException ex) {
                        MARSHAL_IN_LOG.log(WARNING, ex, () -> "unable to create instance of " + getType().getName());
                        MARSHAL_IN_LOG.warning(() -> "constructor => " + constructor);
                        throw ex;
                    }
                })
                .orElse(() -> null);
    }

    private Optional<Constructor<?>> findConstructor() {
        if (isExternalizable()) {
            return findExternalizableConstructor();
        } else if (isSerializable() && !getType().isInterface()) {
            return findSerializableConstructor();
        }
        return Optional.empty();
    }

    private Optional<Constructor<?>> findExternalizableConstructor() {
        Class<?> type = getType();
        try {
            Constructor<?> constructor = doPrivileged(getNoArgConstructor(type));
            constructor = doPrivileged(makeAccessible(constructor));
            return Optional.of(constructor);
        } catch (Exception ex) {
            MARSHAL_LOG.log(WARNING, ex, () -> "Class " + type.getName()
                    + " is not properly externalizable. It has no default constructor.");
            return Optional.empty();
        }
    }

    private Optional<Constructor<?>> findSerializableConstructor() {
        Class<?> initClass = getFirstNonSerializableSuperclass();
        Class<?> type = getType();

        if (initClass == null) {
            MARSHAL_LOG.warning(() -> "Class " + type.getName()
                    + " is not properly serializable. It has no non-serializable super-class");
            return Optional.empty();
        }

        try {
            Constructor<?> initConstructor = doPrivileged(getNoArgConstructor(initClass));

            if (!isConstructorAccessible(initConstructor, initClass)) {
                return Optional.empty();
            }

            Constructor<?> constructor = getReflectionFactory().newConstructorForSerialization(type, initConstructor);

            if (constructor == null) {
                MARSHAL_LOG.warning(() -> "Unable to get constructor for serialization for class " + java_name);
                return Optional.empty();
            }

            constructor = doPrivileged(makeAccessible(constructor));
            return Optional.of(constructor);

        } catch (Exception ex) {
            MARSHAL_LOG.log(WARNING, ex, () -> "Class " + type.getName()
                    + " is not properly serializable. First non-serializable super-class ("
                    + initClass.getName() + ") has no default constructor.");
            return Optional.empty();
        }
    }

    private boolean isConstructorAccessible(Constructor<?> constructor, Class<?> initClass) {
        int modifiers = constructor.getModifiers();
        if (Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers)) {
            return true;
        }

        Class<?> type = getType();
        if (!samePackage(type, initClass)) {
            MARSHAL_LOG.warning(() -> "Class " + type.getName()
                    + " is not properly serializable. The default constructor of its first "
                    + "non-serializable super-class (" + initClass.getName() + ") is not accessible.");
            return false;
        }

        return true;
    }

    final List<FieldDescriptor> getFields() {
        return fieldsRef.get();
    }

    List<FieldDescriptor> genFields() {
        return buildFieldDescriptors();
    }

    private List<FieldDescriptor> buildFieldDescriptors() {
        if (!isSerializable()) return emptyList();

        return Optional.ofNullable(findSerialPersistentFields())
                .map(this::buildFieldDescriptorsFromSerialPersistentFields)
                .orElseGet(this::buildFieldDescriptorsFromDeclaredFields);
    }

    private List<FieldDescriptor> buildFieldDescriptorsFromDeclaredFields() {
        Field[] declaredFields = doPrivileged(getDeclaredFields(getType()));
        return Arrays.stream(declaredFields)
                .filter(this::isSerializableField)
                .map(f -> doPrivileged(makeAccessible(f)))
                .map(f -> FieldDescriptor.get(f, repo))
                .sorted()
                .collect(collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
    }

    private List<FieldDescriptor> buildFieldDescriptorsFromSerialPersistentFields(ObjectStreamField[] serialPersistentFields) {
        return Arrays.stream(serialPersistentFields)
                .map(streamField -> Optional.ofNullable(findMatchingField(streamField))
                        .orElseGet(() -> getForSerialPersistentField(getType(), streamField, repo)))
                .sorted()
                .collect(collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
    }

    private FieldDescriptor findMatchingField(ObjectStreamField streamField) {
        try {
            Field reflectionField = doPrivileged(getField(getType(), streamField.getName()));
            reflectionField = doPrivileged(makeAccessible(reflectionField));

            if (reflectionField.getType() == streamField.getType()) {
                return FieldDescriptor.get(reflectionField, repo);
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    private Class<?> getFirstNonSerializableSuperclass() {
        Class<?> initClass = getType();

        while ((initClass != null) && Serializable.class.isAssignableFrom(initClass)) {
            initClass = initClass.getSuperclass();
        }
        return initClass;
    }

    private boolean samePackage(Class<?> type, Class<?> initClass) {
        String pkg1 = getPackageName(type);
        String pkg2 = getPackageName(initClass);

        return pkg1.equals(pkg2);
    }

    private String getPackageName(Class<?> type) {
        String name = type.getName();
        int idx = name.lastIndexOf('.');
        return (idx == -1) ? "" : name.substring(0, idx);
    }

    private boolean genCustomMarshalled() {
        return isExternalizable() || getOptionalWriteObjectHandle().isPresent();
    }

    public boolean isCustomMarshalled() {
        return customMarshalledRef.get();
    }

    private boolean genChunked() {
        if (isCustomMarshalled()) return true;
        return Optional.ofNullable(getSuperDescriptor()).map(ValueDescriptor::isChunked).orElse(false);
    }

    public boolean isChunked() {
        return chunkedRef.get();
    }

    private Function<Serializable, Serializable> getWriteReplacer() {
        return writeReplacerRef.get();
    }

    private Function<Serializable, Serializable> getReadResolver() {
        return readResolverRef.get();
    }

    private MethodHandle getReadObjectHandle() {
        return readObjectHandleRef.get();
    }





    public Serializable writeReplace(Serializable val) {
        return getWriteReplacer().apply(val);
    }

    public Serializable readResolve(Serializable val) {
        return getReadResolver().apply(val);
    }

    public void writeValue(final OutputStream out, final Serializable value) {
        try {
            ObjectWriter writer = new CorbaObjectWriter(out, value);
            writeValue(writer, value);
        } catch (IOException ex) {
            throw as(MARSHAL::new, ex, ex.getMessage());
        }
    }

    protected void defaultWriteValue(ObjectWriter writer, Serializable val) throws IOException {
        MARSHAL_OUT_LOG.finer(() -> "writing fields for " + getType());

        List<FieldDescriptor> fields = getFields();

        if (fields.isEmpty()) return;

        for (FieldDescriptor field : fields) {
            MARSHAL_OUT_LOG.finer(() -> "writing field " + field.java_name);
            field.write(writer, val);
        }
    }

    private ValueWriter genValueWriter() {
        return new ValueWriterBuilder().build();
    }

    /**
     * Builder class for creating ValueWriter instances.
     * Encapsulates the logic for determining the appropriate writer strategy
     * based on the value descriptor's characteristics.
     */
    private class ValueWriterBuilder {
        private final LazyReference<ValueWriter> superWriterRef = new LazyReference<>(this::genSuperWriter);

        ValueWriter build() {
            if (isExternalizable()) {
                return buildExternalizableWriter();
            }

            return getOptionalWriteObjectHandle()
                    .map(this::buildCustomWriter)
                    .orElseGet(this::buildDefaultWriter);
        }

        private ValueWriter buildExternalizableWriter() {
            return (writer, val) -> {
                try {
                    writer.invokeWriteExternal((Externalizable) val);
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            };
        }

        private ValueWriter buildDefaultWriter() {
            return (writer, val) -> {
                try {
                    getSuperWriter().accept(writer, val);
                    defaultWriteValue(writer, val);
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            };
        }

        private ValueWriter buildCustomWriter(MethodHandle writeObjectHandle) {
            return (writer, val) -> {
                try {
                    getSuperWriter().accept(writer, val);
                    writer.invokeWriteObject(ValueDescriptor.this, val, writeObjectHandle);
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            };
        }

        private ValueWriter getSuperWriter() {
            return superWriterRef.get();
        }

        private ValueWriter genSuperWriter() {
            ValueDescriptor superDesc = getSuperDescriptor();
            return (superDesc == null)
                    ? (writer, val) -> {} // no-op if no super descriptor
                    : (writer, val) -> {
                try {
                    superDesc.writeValue(writer, val);
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            };
        }
    }

    @FunctionalInterface
    private interface ValueWriter extends BiConsumer<ObjectWriter,Serializable> {}

    ValueReader genValueReader() {
        return new ValueReaderBuilder().build();
    }

    /**
     * Builder class for creating ValueReader instances.
     * Encapsulates the logic for determining the appropriate reader strategy
     * based on the value descriptor's characteristics.
     */
    private class ValueReaderBuilder {
        private final LazyReference<ValueReader> superReaderRef = new LazyReference<>(ValueDescriptor.this::genSuperReader);

        ValueReader build() {
            return isExternalizable() ? buildExternalizableReader()
                    : isCustomMarshalled() ? buildCustomMarshalReader()
                    : buildSimpleReader();
        }

        private ValueReader buildExternalizableReader() {
            return (reader, value) -> {
                try {
                    reader.readExternal((Externalizable) value);
                    return value;
                } catch (ClassNotFoundException e) {
                    throw new UncheckedIOException(new IOException("cannot instantiate class", e));
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            };
        }

        private ValueReader buildSimpleReader() {
            return getOptionalReadObjectHandle()
                    .map(this::buildReader)
                    .orElseGet(this::buildDefaultReader);
        }

        private ValueReader buildReader(MethodHandle readObjectHandle) {
            return (reader, value) -> {
                Serializable val = getSuperReader().apply(reader, value);
                try {
                    reader.setCurrentValueDescriptor(ValueDescriptor.this);
                    readObjectHandle.invoke(val, reader);
                    reader.setCurrentValueDescriptor(null);
                    return val;
                } catch (Error | RuntimeException e) {
                    throw e;
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                } catch (Throwable t) {
                    throw as(UnknownException::new, t, t);
                }
            };
        }

        private ValueReader buildDefaultReader() {
            return (reader, value) -> {
                Serializable val = getSuperReader().apply(reader, value);
                try {
                    defaultReadValue(reader, val);
                    return val;
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            };
        }

        private ValueReader buildCustomMarshalReader() {
            return getOptionalReadObjectHandle()
                    .map(this::buildCustomMarshalReaderWithReadObject)
                    .orElseGet(this::buildCustomMarshalReaderWithoutReadObject);
        }

        private ValueReader buildCustomMarshalReaderWithReadObject(MethodHandle readObjectHandle) {
            return (reader, value) -> {
                Serializable val = getSuperReader().apply(reader, value);
                try {
                    byte cmsfVersion = reader.readByte();
                    boolean dwoCalled = reader.readBoolean();
                    MARSHAL_IN_LOG.log(Level.FINE, "Reading value in streamFormatVersion=" + cmsfVersion + " defaultWriteObject=" + dwoCalled);

                    ObjectReader wrappedReader = wrapIfNeeded(reader, cmsfVersion);
                    wrappedReader.setCurrentValueDescriptor(ValueDescriptor.this);
                    readObjectHandle.invoke(val, wrappedReader);
                    wrappedReader.setCurrentValueDescriptor(null);
                    if (wrappedReader != reader) {
                        wrappedReader.close();
                    }
                    return val;
                } catch (Error | RuntimeException e) {
                    throw e;
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                } catch (Throwable t) {
                    throw as(UnknownException::new, t, t);
                }
            };
        }

        private ValueReader buildCustomMarshalReaderWithoutReadObject() {
            return (reader, value) -> {
                Serializable val = getSuperReader().apply(reader, value);
                try {
                    byte cmsfVersion = reader.readByte();
                    boolean dwoCalled = reader.readBoolean();
                    MARSHAL_IN_LOG.log(Level.FINE, "Reading value in streamFormatVersion=" + cmsfVersion + " defaultWriteObject=" + dwoCalled);

                    ObjectReader wrappedReader = wrapIfNeeded(reader, cmsfVersion);
                    defaultReadValue(reader, val);
                    if (wrappedReader != reader) {
                        wrappedReader.close();
                    }
                    return val;
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            };
        }

        private ValueReader getSuperReader() {
            return superReaderRef.get();
        }

        private ObjectReader wrapIfNeeded(ObjectReader reader, byte cmsfVersion) throws IOException {
            return (cmsfVersion == 2) ? CustomMarshaledObjectReader.wrap(reader) : reader;
        }
    }

    private ValueWriter getValueWriter() {
        return valueWriterRef.get();
    }

    protected void writeValue(ObjectWriter writer, Serializable val) throws IOException {
        try {
            getValueWriter().accept(writer, val);
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    Serializable createBlankInstance() {
        return blankInstanceSupplierRef.get().get();
    }

    final CorbaObjectReader makeCorbaObjectReader(final InputStream in, final Map<Integer, Serializable> offsetMap, final Serializable obj)
            throws IOException {
        return new CorbaObjectReader(in, offsetMap, obj);
    }

    public Serializable readValue(final InputStream in, final Map<Integer, Serializable> offsetMap, final Integer offset) {
        final Serializable value = createBlankInstance();

        if (null != value) offsetMap.put(offset, value);

        try {
            ObjectReader reader = makeCorbaObjectReader(in, offsetMap, value);

            final Serializable resolved = readResolve(readValue(reader, value));
            if (value != resolved) {
                offsetMap.put(offset, resolved);
            }
            return resolved;
        } catch (IOException ex) {
            throw as(MARSHAL::new, ex, ex.getMessage());
        }
    }

    void print(PrintWriter pw, Map<Object, Integer> recurse, Object val) {
        if (val == null) {
            pw.print("null");
        }

        Integer old = recurse.get(val);
        if (old != null) {
            pw.print("^" + old);
        } else {
            int key = System.identityHashCode(val);
            recurse.put(val, key);

            pw.println(getType().getName() + "@" + Integer.toHexString(key) + "[");

            printFields(pw, recurse, val);

            pw.println("]");
        }
    }

    void printFields(PrintWriter pw, Map<Object, Integer> recurse, Object val) {
        pw.print("(" + getClass().getName() + ")");

        ValueDescriptor superDesc = getSuperDescriptor();

        if (superDesc != null) {
            superDesc.printFields(pw, recurse, val);
        }

        List<FieldDescriptor> fields = getFields();

        if (fields.isEmpty()) return;

        for (int i = 0; i < fields.size(); i++) {
            if (i != 0) pw.print("; ");
            fields.get(i).print(pw, recurse, val);
        }

    }

    void defaultReadValue(ObjectReader reader, Serializable value) throws IOException {
        List<FieldDescriptor> fields = getFields();
        if (fields.isEmpty()) return;

        MARSHAL_IN_LOG.fine(() -> "reading fields for " + getType().getName());

        for (FieldDescriptor field : fields) {
            MARSHAL_IN_LOG.fine(() -> "reading field " + field.java_name + " of type " + field.getType().getName() + " using " + field.getClass().getName());

            try {
                field.read(reader, value);
            } catch (MARSHAL ex) {
                if (ex.getMessage() != null)
                    throw ex;

                String msg = String.format("%s, while reading %s.%s", ex, java_name, field.java_name);
                throw as(MARSHAL::new, ex, msg, ex.minor, ex.completed);
            }
        }
    }

    Map<String, Object> readFields(ObjectReader reader) throws IOException {
        List<FieldDescriptor> fields = getFields();
        if (fields.isEmpty()) {
            return emptyMap();
        }

        MARSHAL_IN_LOG.finer(() -> "reading fields for " + getType().getName());

        Map<String, Object> map = new HashMap<>();

        for (FieldDescriptor _field : fields) {
            MARSHAL_IN_LOG.finer(() -> "reading field " + _field.java_name);
            _field.readFieldIntoMap(reader, map);
        }

        return map;
    }

    void writeFields(ObjectWriter writer, Map<String, Object> fieldMap) throws IOException {
        List<FieldDescriptor> fields = getFields();
        if (fields.isEmpty()) {
            return;
        }

        MARSHAL_OUT_LOG.finer(() -> "writing fields for " + getType().getName());

        for (FieldDescriptor _field : fields) {
            MARSHAL_OUT_LOG.finer(() -> "writing field " + _field.java_name);
            _field.writeFieldFromMap(writer, fieldMap);
        }

    }

    /**
     * This method reads the fields of a single class slice.
     */
    private ValueReader getValueReader() {
        return valueReaderRef.get();
    }

    /**
     * This method reads the fields of a single class slice.
     */
    private Serializable readValue(ObjectReader reader, Serializable value) throws IOException {
        try {
            return getValueReader().apply(reader, value);
        } catch (UncheckedIOException ex) {
            throw ex.getCause();
        }
    }

    long genClassHash() {
        return new ClassHashBuilder().build();
    }

    /**
     * Builder class for computing the RMI class hash.
     * Encapsulates the logic for generating a hash code based on class structure,
     * following the RMI serialization specification.
     */
    private class ClassHashBuilder {
        long build() {
            if (isExternalizable()) return 1L;
            if (!isSerializable()) return 0L;

            try {
                return computeHash();
            } catch (NoSuchAlgorithmException | IOException ex) {
                throw new RuntimeException("cannot compute RMI hash code", ex);
            } catch (UncheckedIOException ex) {
                throw new RuntimeException("cannot compute RMI hash code", ex.getCause());
            }
        }

        private long computeHash() throws NoSuchAlgorithmException, IOException {
            MessageDigest digest = MessageDigest.getInstance("SHA");
            try (DataOutputStream out = new DataOutputStream(
                    new DigestOutputStream(new ByteArrayOutputStream(512), digest))) {

                writeSuperClassHash(out);
                writeCustomMarshalFlag(out);
                writeFieldSignatures(out);

                out.flush();
            }

            byte[] data = digest.digest();
            return createReadBuffer(data.length < 8 ? Arrays.copyOf(data, 8) : data).readLong_LE();
        }

        private void writeSuperClassHash(DataOutputStream out) {
            Optional.ofNullable(getType().getSuperclass())
                    .map(repo::getDescriptor)
                    .map(TypeDescriptor::getClassHash)
                    .ifPresent(hash -> {
                        try {
                            out.writeLong(hash);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
        }

        private void writeCustomMarshalFlag(DataOutputStream out) throws IOException {
            out.writeInt(isCustomMarshalled() ? 2 : 1);
        }

        private void writeFieldSignatures(DataOutputStream out) {
            getFields().stream()
                    .sorted(compareByName)
                    .forEach(field -> {
                        try {
                            out.writeUTF(field.java_name);
                            out.writeUTF(makeSignature(field.getType()));
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
        }
    }

    private static final Comparator<FieldDescriptor> compareByName = comparing(f -> f.java_name);

    private final LazyReference<List<ValueMember>> valueMembersRef = new LazyReference<>(this::genValueMembers);

    protected List<ValueMember> genValueMembers() {
        return getFields().stream()
                .map(FieldDescriptor::getValueMember)
                .collect(collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
    }

    private static final ValueMember[] EMPTY_VALUE_MEMBERS = {};
    final ValueMember[] getValueMembers() {
        getTypeCode(); // ensure recursion through typecode
        return valueMembersRef.get().toArray(EMPTY_VALUE_MEMBERS);
    }

    @Override
    TypeCode genTypeCode() {
        ORB orb = ORB.init();
        TypeCode _base = Optional.ofNullable(getSuperDescriptor()).map(ValueDescriptor::getTypeCode).orElse(null);

        Class<?> type = getType();
        TypeCode tc;
        if (type.isArray()) {
            TypeDescriptor desc = repo.getDescriptor(type.getComponentType());
            tc = desc.getTypeCode();
            tc = orb.create_sequence_tc(0, tc);
            tc = orb.create_value_box_tc(getRepositoryID(), "Sequence", tc);
        } else {
            tc = orb.create_value_tc(getRepositoryID(), type.getSimpleName(), VM_NONE.value, _base, getValueMembers());
        }

        return tc;
    }

    private static final OperationDescription[] ZERO_OPERATIONS = {};
    private static final AttributeDescription[] ZERO_ATTRIBUTES = {};
    private static final Initializer[] ZERO_INITIALIZERS = {};
    private static final String[] ZERO_STRINGS = {};


    /**
     * Creates a defensive copy of a FullValueDescription.
     * This ensures that modifications to the returned object or its array fields
     * do not affect the original.
     *
     * @param original the FullValueDescription to copy
     * @return a defensive copy of the FullValueDescription
     */
    static FullValueDescription copyOf(FullValueDescription original) {
        if (original == null) {
            return null;
        }

        FullValueDescription copy = new FullValueDescription();
        copy.name = original.name;
        copy.id = original.id;
        copy.is_abstract = original.is_abstract;
        copy.is_custom = original.is_custom;
        copy.defined_in = original.defined_in;
        copy.version = original.version;

        // Deep copy arrays
        copy.operations = original.operations == null ? null :
            Arrays.copyOf(original.operations, original.operations.length);
        copy.attributes = original.attributes == null ? null :
            Arrays.copyOf(original.attributes, original.attributes.length);
        copy.members = original.members == null ? null :
            Arrays.copyOf(original.members, original.members.length);
        copy.initializers = original.initializers == null ? null :
            Arrays.copyOf(original.initializers, original.initializers.length);
        copy.supported_interfaces = original.supported_interfaces == null ? null :
            Arrays.copyOf(original.supported_interfaces, original.supported_interfaces.length);
        copy.abstract_base_values = original.abstract_base_values == null ? null :
            Arrays.copyOf(original.abstract_base_values, original.abstract_base_values.length);

        copy.is_truncatable = original.is_truncatable;
        copy.base_value = original.base_value;
        copy.type = original.type;

        return copy;
    }


    FullValueDescription getFullValueDescription() {
        FullValueDescription fvd = new FullValueDescription();
        fvd.name = getType().getName();
        fvd.id = getRepositoryID();
        fvd.is_abstract = false;
        fvd.is_custom = isCustomMarshalled();
        fvd.defined_in = "";
        fvd.version = "1.0";
        fvd.operations = ZERO_OPERATIONS;
        fvd.attributes = ZERO_ATTRIBUTES;
        fvd.members = getValueMembers();
        fvd.initializers = ZERO_INITIALIZERS;
        fvd.supported_interfaces = ZERO_STRINGS;
        fvd.abstract_base_values = ZERO_STRINGS;
        fvd.is_truncatable = false;
        fvd.base_value = Optional.ofNullable(getSuperDescriptor()).map(ValueDescriptor::getRepositoryID).orElse("");
        fvd.type = getTypeCode();
        return fvd;
    }



    public boolean copyWithinState() {
        return !(isImmutableValue() | isRmiStub());
    }

    Object copyObject(Object orig, CopyState state) {

        if (isImmutableValue() || isRmiStub()) {
            return orig;
        }

        Serializable oorig = (Serializable) orig;

        MARSHAL_OUT_LOG.finer(() -> "copying " + orig);

        oorig = writeReplace(oorig);

        ValueDescriptor wdesc;
        if (oorig == orig) {
            wdesc = this;
        } else {
            wdesc = (ValueDescriptor) repo.getDescriptor(oorig.getClass());

            MARSHAL_OUT_LOG.finer(() -> "writeReplace -> " + getType().getName());
        }

        return wdesc.copyObject2(oorig, state);
    }

    /**
     * this is called after write-replace on the type descriptor of the correct
     * type for writing
     */
    private Serializable copyObject2(Serializable oorig, CopyState state) {

        // create instance of copied object, and register
        Serializable copy = createBlankInstance();
        state.put(oorig, copy);

        // write original object
        ObjectWriter writer = writeObject(oorig, state);

        // read into copy
        return readObject(writer, copy);
    }

    private ObjectWriter writeObject(Serializable oorig, CopyState state) {
        try {
            ObjectWriter writer = state.createObjectWriter(oorig);
            writeValue(writer, oorig);
            return writer;
        } catch (IOException ex) {
            String msg = String.format("%s writing %s", ex, getType().getName());
            throw (MARSHAL) new MARSHAL(msg).initCause(ex);
        }
    }

    private Serializable readObject(ObjectWriter writer, Serializable copy) {
        try {
            ObjectReader reader = writer.getObjectReader(copy);
            readValue(reader, copy);
            return readResolve(copy);
        } catch (IOException ex) {
            String msg = String.format("%s reading instance of %s", ex, getType().getName());
            throw (MARSHAL) new MARSHAL(msg).initCause(ex);
        }
    }

    void writeMarshalValue(PrintWriter pw, String outName, String paramName) {
        pw.print(outName);
        pw.print('.');
        pw.print("write_value");

        // this ValueDescriptor could represent an Abstract Value,
        // in which case we need to cast the first argument.
        // We'll just always do that, because most of the time
        // HotSpot will remove this cast anyway.

        pw.print("((java.io.Serializable)");

        pw.print(paramName);
        pw.print(',');
        MethodDescriptor.writeJavaType(pw, getType());
        pw.print(".class)");
    }

    void writeUnmarshalValue(PrintWriter pw, String inName) {
        pw.print(inName);
        pw.print('.');
        pw.print("read_value");
        pw.print('(');
        MethodDescriptor.writeJavaType(pw, getType());
        pw.print(".class)");
    }

    @Override
    void addDependencies(Set<Class<?>> classes) {
        Class<?> c = getType();

        if ((c == Object.class) || classes.contains(c))
            return;

        classes.add(c);

        if (c.getSuperclass() != null) {
            TypeDescriptor desc = repo.getDescriptor(c.getSuperclass());
            desc.addDependencies(classes);
        }

        Class<?>[] ifaces = c.getInterfaces();
        for (Class<?> iface : ifaces) {
            TypeDescriptor desc = repo.getDescriptor(iface);
            desc.addDependencies(classes);
        }

        for (FieldDescriptor field : getFields()) {
            if (field.isPrimitive())
                continue;

            TypeDescriptor desc = repo.getDescriptor(field.getType());
            desc.addDependencies(classes);
        }
    }
}
