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
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.rmi.Remote;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;

import static java.security.AccessController.doPrivileged;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableSet;
import static java.util.Comparator.comparing;
import static java.util.function.Function.identity;
import static java.util.logging.Level.WARNING;
import static org.apache.yoko.io.Buffer.createReadBuffer;
import static org.apache.yoko.logging.VerboseLogging.MARSHAL_IN_LOG;
import static org.apache.yoko.logging.VerboseLogging.MARSHAL_LOG;
import static org.apache.yoko.logging.VerboseLogging.MARSHAL_OUT_LOG;
import static org.apache.yoko.rmi.impl.FieldDescriptor.getForSerialPersistentField;
import static org.apache.yoko.rmi.util.StringUtil.convertToValidIDLNames;
import static org.apache.yoko.util.Exceptions.as;
import static sun.reflect.ReflectionFactory.getReflectionFactory;

class ValueDescriptor extends TypeDescriptor {
    private final LazyReference<Function<Serializable, Serializable>> writeReplacerRef = new LazyReference<>(this::genWriteReplacer);

    private final LazyReference<Function<Serializable, Serializable>> readResolverRef = new LazyReference<>(this::genReadResolver);

    private final LazyReference<Supplier<Serializable>> blankInstanceSupplierRef = new LazyReference<>(this::genBlankInstanceSupplier);

    private final LazyReference<Method> writeObjectMethodRef = new LazyReference<>(this::findWriteObjectMethod);

    private final LazyReference<Method> readObjectMethodRef = new LazyReference<>(this::findReadObjectMethod);

    private final LazyReference<Long> serialVersionUidRef = new LazyReference<>(this::genSerialVersionUid);

    private final LazyReference<ValueWriter> valueWriterRef = new LazyReference<>(this::genValueWriter);
    private final LazyReference<ValueReader> valueReaderRef = new LazyReference<>(this::genValueReader);

    @FunctionalInterface
    private interface ValueReader extends BiFunction<ObjectReader, Serializable, Serializable> {}

    private final LazyReference<ValueDescriptor> superDescriptorRef = new LazyReference<>(this::genSuperDescriptor);

    protected final LazyReference<FieldDescriptor[]> fieldsRef = new LazyReference<>(this::genFields);

    private final LazyReference<Boolean> immutableValueRef = new LazyReference<>(this::genImmutableValue);

    private final LazyReference<String> customRepIdRef = new LazyReference<>(this::genCustomRepId);

    private static final Set<? extends Class<? extends Serializable>> IMMUTABLE_VALUE_CLASSES = unmodifiableSet(new HashSet<>(asList(Integer.class,
            Character.class, Boolean.class, Byte.class, Long.class, Float.class, Double.class, Short.class)));

    ValueDescriptor(Class<?> type, TypeRepository repository) {
        super(type, repository);
    }

    protected boolean isEnum() { return false; }

    boolean isExternalizable() { return Externalizable.class.isAssignableFrom(getType()); }

    boolean isSerializable() { return Serializable.class.isAssignableFrom(getType()); }

    boolean isRmiStub() { return RMIStub.class.isAssignableFrom(getType()); }

    boolean isImmutableValue() { return immutableValueRef.get(); }

    private boolean genImmutableValue() {
        return IMMUTABLE_VALUE_CLASSES.contains(getType());
    }

    @Override
    protected final RemoteInterfaceDescriptor genRemoteInterface() {
        Class<?> type = getType();
        return Remote.class.isAssignableFrom(type) ?
                RemoteDescriptor.genMostSpecificRemoteInterface(type, repo) :
                super.genRemoteInterface();
    }

    @Override
    String genRepId() {
        return genRepId(getClassHash());
    }

    private String genRepId(long hashCode) {
        return String.format("RMI:%s:%016X:%016X", convertToValidIDLNames(getType().getName()), hashCode, getSerialVersionUid());
    }

    String genCustomRepId() {
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
                throw as(UncheckedIOException::new, ex);
            }
        };
    }


    private Function<Serializable, Serializable> genWriteReplacer() {
        Optional<Method> methodOpt = doPrivileged((PrivilegedAction<Optional<Method>>) () -> {
            for (Class<?> curr = getType(); curr != null; curr = curr.getSuperclass()) {
                try {
                    Method method = curr.getDeclaredMethod("writeReplace");
                    method.setAccessible(true);
                    return Optional.of(method);
                } catch (NoSuchMethodException ignored) {
                }
            }
            return Optional.empty();
        });
        
        return methodOpt.map(method -> (Function<Serializable, Serializable>) val -> {
            try {
                return (Serializable) method.invoke(val);
            } catch (IllegalAccessException ex) {
                throw as(MARSHAL::new, ex, "cannot call " + method);
            } catch (IllegalArgumentException ex) {
                throw as(MARSHAL::new, ex, ex.getMessage());
            } catch (InvocationTargetException ex) {
                final Throwable t = ex.getTargetException();
                throw as(UnknownException::new, t, t);
            }
        }).orElse(identity());
    }

    private Function<Serializable, Serializable> genReadResolver() {
        Optional<Method> methodOpt = doPrivileged((PrivilegedAction<Optional<Method>>) () -> {
            try {
                Method method = getType().getDeclaredMethod("readResolve");
                method.setAccessible(true);
                return Optional.of(method);
            } catch (NoSuchMethodException ignored) {
                return Optional.empty();
            }
        });
        
        return methodOpt.map(method -> (Function<Serializable, Serializable>) val -> {
            try {
                return (Serializable) method.invoke(val);
            } catch (IllegalAccessException ex) {
                throw as(MARSHAL::new, ex, "cannot call " + method);
            } catch (IllegalArgumentException ex) {
                throw as(MARSHAL::new, ex, ex.getMessage());
            } catch (InvocationTargetException ex) {
                final Throwable t = ex.getTargetException();
                throw as(UnknownException::new, t, t);
            }
        }).orElse(identity());
    }

    private Method findReadObjectMethod() {
        return doPrivileged((PrivilegedAction<Method>) () -> {
            try {
                Method method = getType().getDeclaredMethod("readObject", ObjectInputStream.class);
                
                // Validate the method
                int modifiers = method.getModifiers();
                if (!Modifier.isPrivate(modifiers) || Modifier.isStatic(modifiers)) {
                    return null;
                }
                
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
                return null;
            }
        });
    }

    private Method findWriteObjectMethod() {
        Class<?> type = getType();
        return doPrivileged((PrivilegedAction<Method>) () -> {
            try {
                Method method = type.getDeclaredMethod("writeObject", ObjectOutputStream.class);
                
                // Validate the method
                int modifiers = method.getModifiers();
                if (!Modifier.isPrivate(modifiers) 
                        || Modifier.isStatic(modifiers) 
                        || method.getDeclaringClass() != type) {
                    return null;
                }
                
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
                return null;
            }
        });
    }

    private Field findSerialVersionUIDField() {
        return doPrivileged((PrivilegedAction<Field>) () -> {
            try {
                Field field = getType().getDeclaredField("serialVersionUID");
                if (Modifier.isStatic(field.getModifiers())) {
                    field.setAccessible(true);
                    return field;
                }
                return null;
            } catch (NoSuchFieldException ignored) {
                return null;
            }
        });
    }

    ObjectStreamField[] findSerialPersistentFields() {
        try {
            Field field = getType().getDeclaredField("serialPersistentFields");
            field.setAccessible(true);
            return (ObjectStreamField[]) field.get(null);
        } catch (IllegalAccessException | NoSuchFieldException ignored) {
            return null;
        }
    }

    Supplier<Serializable> genBlankInstanceSupplier() {
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

    private Optional<Constructor> findConstructor() {
        return doPrivileged((PrivilegedAction<Optional<Constructor>>) () -> {
            if (isExternalizable()) {
                return findExternalizableConstructor();
            } else if (isSerializable() && !getType().isInterface()) {
                return findSerializableConstructor();
            }
            return Optional.empty();
        });
    }

    private Optional<Constructor> findExternalizableConstructor() {
        Class<?> type = getType();
        try {
            Constructor<?> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return Optional.of(constructor);
        } catch (NoSuchMethodException ex) {
            MARSHAL_LOG.log(WARNING, ex, () -> "Class " + type.getName()
                    + " is not properly externalizable. It has no default constructor.");
            return Optional.empty();
        }
    }

    private Optional<Constructor> findSerializableConstructor() {
        Class<?> initClass = getFirstNonSerializableSuperclass();
        Class<?> type = getType();

        if (initClass == null) {
            MARSHAL_LOG.warning(() -> "Class " + type.getName() 
                    + " is not properly serializable. It has no non-serializable super-class");
            return Optional.empty();
        }

        try {
            Constructor<?> initConstructor = initClass.getDeclaredConstructor();

            if (!isConstructorAccessible(initConstructor, initClass)) {
                return Optional.empty();
            }

            Constructor<?> constructor = getReflectionFactory().newConstructorForSerialization(type, initConstructor);

            if (constructor == null) {
                MARSHAL_LOG.warning(() -> "Unable to get constructor for serialization for class " + java_name);
                return Optional.empty();
            }
            
            constructor.setAccessible(true);
            return Optional.of(constructor);

        } catch (NoSuchMethodException ex) {
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

    final FieldDescriptor[] getFields() {
        return fieldsRef.get();
    }

    FieldDescriptor[] genFields() {
        return doPrivileged((PrivilegedAction<FieldDescriptor[]>) this::buildFieldDescriptors);
    }

    private FieldDescriptor[] buildFieldDescriptors() {
        if (!isSerializable()) return FieldDescriptor.EMPTY_ARRAY;

        return Optional.ofNullable(findSerialPersistentFields())
                .map(this::buildFieldDescriptorsFromSerialPersistentFields)
                .orElseGet(this::buildFieldDescriptorsFromDeclaredFields);
    }

    private FieldDescriptor[] buildFieldDescriptorsFromDeclaredFields() {
        return Arrays.stream(getType().getDeclaredFields())
                .filter(this::isSerializableField)
                .peek(f -> f.setAccessible(true))
                .map(f -> FieldDescriptor.get(f, repo))
                .sorted()
                .toArray(FieldDescriptor[]::new);
    }

    private FieldDescriptor[] buildFieldDescriptorsFromSerialPersistentFields(ObjectStreamField[] serialPersistentFields) {
        return Arrays.stream(serialPersistentFields)
                .map(streamField -> Optional.ofNullable(findMatchingField(streamField))
                        .orElseGet(() -> getForSerialPersistentField(getType(), streamField, repo)))
                .sorted()
                .toArray(FieldDescriptor[]::new);
    }

    private FieldDescriptor findMatchingField(ObjectStreamField streamField) {
        try {
            Field reflectionField = getType().getField(streamField.getName());
            reflectionField.setAccessible(true);

            if (reflectionField.getType() == streamField.getType()) {
                return FieldDescriptor.get(reflectionField, repo);
            }
        } catch (SecurityException | NoSuchFieldException ignored) {
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

    /** Read an instance of this value from a CDR stream */
    public Object read(org.omg.CORBA.portable.InputStream in) {
        return ((org.omg.CORBA_2_3.portable.InputStream) in).read_value();
    }

    /** Write an instance of this value to a CDR stream */
    public void write(OutputStream out, Object value) {
        ((org.omg.CORBA_2_3.portable.OutputStream) out).write_value((Serializable) value);
    }

    public boolean isCustomMarshalled() {
        return (isExternalizable() || getWriteObjectMethod().isPresent());
    }

    public boolean isChunked() {
        if (isCustomMarshalled()) return true;
        return Optional.ofNullable(getSuperDescriptor()).map(ValueDescriptor::isChunked).orElse(false);
    }

    Function<Serializable, Serializable> getWriteReplacer() {
        return writeReplacerRef.get();
    }

    Function<Serializable, Serializable> getReadResolver() {
        return readResolverRef.get();
    }

    Optional<Method> getReadObjectMethod() {
        return Optional.ofNullable(readObjectMethodRef.get());
    }

    Optional<Method> getWriteObjectMethod() {
        return Optional.ofNullable(writeObjectMethodRef.get());
    }





    public Serializable writeReplace(Serializable val) {
        return getWriteReplacer().apply(val);
    }

    public Serializable readResolve(Serializable val) {
        return getReadResolver().apply(val);
    }

    public void writeValue(final OutputStream out, final Serializable value) {
        try {
            ObjectWriter writer = doPrivileged((PrivilegedAction<ObjectWriter>) () -> {
                try {
                    return new CorbaObjectWriter(out, value);
                } catch (IOException ex) {
                    throw as(MARSHAL::new, ex, ex.getMessage());
                }
            });

            writeValue(writer, value);
        } catch (IOException ex) {
            throw as(MARSHAL::new, ex, ex.getMessage());
        }
    }

    protected void defaultWriteValue(ObjectWriter writer, Serializable val) throws IOException {
        MARSHAL_OUT_LOG.finer(() -> "writing fields for " + getType());

        FieldDescriptor[] fields = getFields();

        if (fields == null) return;

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
            
            return getWriteObjectMethod()
                    .map(this::buildCustomWriter)
                    .orElseGet(this::buildDefaultWriter);
        }

        private ValueWriter buildExternalizableWriter() {
            return (writer, val) -> {
                try {
                    writer.invokeWriteExternal((Externalizable) val);
                } catch (IOException ex) {
                    throw as(UncheckedIOException::new, ex);
                }
            };
        }

        private ValueWriter buildDefaultWriter() {
            return (writer, val) -> {
                try {
                    getSuperWriter().accept(writer, val);
                    defaultWriteValue(writer, val);
                } catch (IOException ex) {
                    throw as(UncheckedIOException::new, ex);
                }
            };
        }

        private ValueWriter buildCustomWriter(Method writeObjectMethod) {
            return (writer, val) -> {
                try {
                    getSuperWriter().accept(writer, val);
                    writer.invokeWriteObject(ValueDescriptor.this, val, writeObjectMethod);
                } catch (IllegalAccessException | IllegalArgumentException ex) {
                    throw as(MARSHAL::new, ex, ex.getMessage());
                } catch (InvocationTargetException ex) {
                    final Throwable t = ex.getTargetException();
                    throw (t instanceof IOException)
                            ? as(UncheckedIOException::new, t)
                            : as(UnknownException::new, t, t);
                } catch (IOException ex) {
                    throw as(UncheckedIOException::new, ex);
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
                    throw as(UncheckedIOException::new, ex);
                }
            };
        }
    }

    @FunctionalInterface
    private interface ValueWriter extends BiConsumer<ObjectWriter,Serializable> {}

    private static class UncheckedIOException extends RuntimeException {
        @Override
        public IOException getCause() { return (IOException)super.getCause(); }
    }


    private ValueReader genValueReader() {
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
            if (isExternalizable()) {
                return buildExternalizableReader();
            }
            
            return getWriteObjectMethod()
                    .map(this::buildCustomMarshalReader)
                    .orElseGet(this::buildSimpleReader);
        }

        private ValueReader buildExternalizableReader() {
            return (reader, value) -> {
                try {
                    reader.readExternal((Externalizable) value);
                    return value;
                } catch (ClassNotFoundException e) {
                    throw as(UncheckedIOException::new, new IOException("cannot instantiate class", e));
                } catch (IOException ex) {
                    throw as(UncheckedIOException::new, ex);
                }
            };
        }

        private ValueReader buildSimpleReader() {
            return getReadObjectMethod()
                    .map(this::buildReader)
                    .orElseGet(this::buildDefaultReader);
        }

        private ValueReader buildReader(Method readObjectMethod) {
            return (reader, value) -> {
                Serializable val = getSuperReader().apply(reader, value);
                try {
                    reader.setCurrentValueDescriptor(ValueDescriptor.this);
                    readObjectMethod.invoke(val, reader);
                    reader.setCurrentValueDescriptor(null);
                    return val;
                } catch (IllegalAccessException | IllegalArgumentException ex) {
                    throw as(MARSHAL::new, ex, ex.getMessage());
                } catch (InvocationTargetException ex) {
                    final Throwable t = ex.getTargetException();
                    throw (t instanceof IOException)
                            ? as(UncheckedIOException::new, t)
                            : as(UnknownException::new, t, t);
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
                    throw as(UncheckedIOException::new, ex);
                }
            };
        }

        private ValueReader buildCustomMarshalReader(Method ignored) {
            return getReadObjectMethod()
                    .map(this::buildCustomMarshalReaderWithReadObject)
                    .orElseGet(this::buildCustomMarshalReaderWithoutReadObject);
        }

        private ValueReader buildCustomMarshalReaderWithReadObject(Method readObjectMethod) {
            return (reader, value) -> {
                Serializable val = getSuperReader().apply(reader, value);
                try {
                    byte cmsfVersion = reader.readByte();
                    boolean dwoCalled = reader.readBoolean();
                    MARSHAL_IN_LOG.log(Level.FINE, "Reading value in streamFormatVersion=" + cmsfVersion + " defaultWriteObject=" + dwoCalled);

                    ObjectReader wrappedReader = wrapIfNeeded(reader, cmsfVersion);
                    wrappedReader.setCurrentValueDescriptor(ValueDescriptor.this);
                    readObjectMethod.invoke(val, wrappedReader);
                    wrappedReader.setCurrentValueDescriptor(null);
                    if (wrappedReader != reader) {
                        wrappedReader.close();
                    }
                    return val;
                } catch (IllegalAccessException | IllegalArgumentException ex) {
                    throw as(MARSHAL::new, ex, ex.getMessage());
                } catch (InvocationTargetException ex) {
                    final Throwable t = ex.getTargetException();
                    throw (t instanceof IOException)
                            ? as(UncheckedIOException::new, t)
                            : as(UnknownException::new, t, t);
                } catch (IOException ex) {
                    throw as(UncheckedIOException::new, ex);
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
                    throw as(UncheckedIOException::new, ex);
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

    public Serializable readValue(final InputStream in, final Map<Integer, Serializable> offsetMap, final Integer offset) {
        final Serializable value = createBlankInstance();

        if (null != value) offsetMap.put(offset, value);

        try {
            ObjectReader reader = doPrivileged((PrivilegedAction<ObjectReader>) () -> {
                try {
                    return new CorbaObjectReader(in, offsetMap, value);
                } catch (IOException ex) {
                    throw as(MARSHAL::new, ex, ex.getMessage());
                }
            });

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

    void printFields(PrintWriter pw, Map recurse, Object val) {
        pw.print("(" + getClass().getName() + ")");

        ValueDescriptor superDesc = getSuperDescriptor();

        if (superDesc != null) {
            superDesc.printFields(pw, recurse, val);
        }

        FieldDescriptor[] fields = getFields();

        if (fields == null)
            return;

        for (int i = 0; i < fields.length; i++) {
            if (i != 0) {
                pw.print("; ");
            }

            fields[i].print(pw, recurse, val);
        }

    }

    void defaultReadValue(ObjectReader reader, Serializable value) throws IOException {
        FieldDescriptor[] fields = getFields();
        if (null == fields) return;

        MARSHAL_IN_LOG.fine(() -> "reading fields for " + getType().getName());

        for (FieldDescriptor _field : fields) {
            MARSHAL_IN_LOG.fine(() -> "reading field " + _field.java_name + " of type " + _field.getType().getName() + " using " + _field.getClass().getName());

            try {
                _field.read(reader, value);
            } catch (MARSHAL ex) {
                if (ex.getMessage() != null)
                    throw ex;

                String msg = String.format("%s, while reading %s.%s", ex, java_name, _field.java_name);
                throw as(MARSHAL::new, ex, msg, ex.minor, ex.completed);
            }
        }
    }

    Map<String, Object> readFields(ObjectReader reader) throws IOException {
        FieldDescriptor[] fields = getFields();
        if ((fields == null) || (fields.length == 0)) {
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
        FieldDescriptor[] fields = getFields();
        if ((fields == null) || (fields.length == 0)) {
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
    protected Serializable readValue(ObjectReader reader, Serializable value) throws IOException {
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
                            throw as(UncheckedIOException::new, e);
                        }
                    });
        }

        private void writeCustomMarshalFlag(DataOutputStream out) throws IOException {
            out.writeInt(getWriteObjectMethod().isPresent() ? 2 : 1);
        }

        private void writeFieldSignatures(DataOutputStream out) {
            Arrays.stream(getFields())
                    .sorted(compareByName)
                    .forEach(field -> {
                        try {
                            out.writeUTF(field.java_name);
                            out.writeUTF(makeSignature(field.getType()));
                        } catch (IOException e) {
                            throw as(UncheckedIOException::new, e);
                        }
                    });
        }
    }

    private static final Comparator<FieldDescriptor> compareByName = comparing(f -> f.java_name);

    private final LazyReference<ValueMember[]> valueMembersRef = new LazyReference<>(this::genValueMembers);
    
    protected ValueMember[] genValueMembers() {
        return Arrays.stream(getFields())
                .map(FieldDescriptor::getValueMember)
                .toArray(ValueMember[]::new);
    }
    
    final ValueMember[] getValueMembers() {
        getTypeCode(); // ensure recursion through typecode
        return valueMembersRef.get();
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

        FieldDescriptor[] fields = getFields();

        if (fields != null) {
            for (FieldDescriptor _field : fields) {
                if (_field.isPrimitive())
                    continue;

                TypeDescriptor desc = repo.getDescriptor(_field.getType());
                desc.addDependencies(classes);
            }
        }
    }
}
