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
import org.omg.CORBA.INTERNAL;
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
import sun.reflect.ReflectionFactory;

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
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;

import static java.security.AccessController.doPrivileged;
import static java.util.Arrays.asList;
import static java.util.Collections.EMPTY_MAP;
import static java.util.Collections.unmodifiableSet;
import static java.util.logging.Level.WARNING;
import static org.apache.yoko.logging.VerboseLogging.MARSHAL_IN_LOG;
import static org.apache.yoko.logging.VerboseLogging.MARSHAL_LOG;
import static org.apache.yoko.logging.VerboseLogging.MARSHAL_OUT_LOG;
import static org.apache.yoko.rmi.util.StringUtil.convertToValidIDLNames;
import static org.apache.yoko.util.Exceptions.as;

class ValueDescriptor extends TypeDescriptor {
    private boolean _is_externalizable;

    private boolean _is_serializable;

    private final LazyReference<Optional<Method>> _write_replace_method = new LazyReference<>(this::findWriteReplaceMethod);

    private final LazyReference<Optional<Method>> _read_resolve_method = new LazyReference<>(this::findReadResolveMethod);

    private Optional<Constructor> _constructor;

    private final LazyReference<Optional<Method>> _write_object_method = new LazyReference<>(this::findWriteObjectMethod);

    private final LazyReference<Optional<Method>> _read_object_method = new LazyReference<>(this::findReadObjectMethod);

    private final LazyReference<Optional<Field>> _serial_version_uid_field = new LazyReference<>(this::findSerialVersionUIDField);

    protected ValueDescriptor _super_descriptor;

    protected FieldDescriptor[] _fields;



    private boolean _is_immutable_value;

    private boolean _is_rmi_stub;

    private final LazyReference<String> _custom_repid = new LazyReference<>(this::genCustomRepId);

    private static final Set<? extends Class<? extends Serializable>> _immutable_value_classes = unmodifiableSet(new HashSet<>(asList(Integer.class,
            Character.class, Boolean.class, Byte.class, Long.class, Float.class, Double.class, Short.class)));

    private long _hash_code;

    ValueDescriptor(Class type, TypeRepository repository) {
        super(type, repository);
    }

    protected boolean isEnum() { return false; }

    @Override
    protected final RemoteInterfaceDescriptor genRemoteInterface() {
        return Remote.class.isAssignableFrom(type) ?
                RemoteDescriptor.genMostSpecificRemoteInterface(type, repo) :
                super.genRemoteInterface();
    }

    @Override
    protected String genRepId() {
        return genRepId(_hash_code);
    }

    final String genRepId(long hashCode) {
        return String.format("RMI:%s:%016X:%016X", convertToValidIDLNames(type.getName()), hashCode, getSerialVersionUID());
    }

    private String genCustomRepId() {
        return String.format("RMI:org.omg.custom.%s", getRepositoryID().substring(4));
    }

    public final String getCustomRepositoryID() {
        return _custom_repid.get();
    }

    protected long getSerialVersionUID() {
        if (getSerialVersionUIDField().isPresent()) {
            try {
                return getSerialVersionUIDField().get().getLong(null);
            } catch (IllegalAccessException ex) {
                // skip //
            }
        }
        ObjectStreamClass serialForm = ObjectStreamClass.lookup(type);

        return (serialForm != null) ? serialForm.getSerialVersionUID() : 0L;
    }

    /**
     * Filters out static and transient fields. This is the base filter that always applies.
     */
    private final boolean isSerializableField(Field f) {
        int mod = f.getModifiers();
        return !Modifier.isStatic(mod) && !Modifier.isTransient(mod);
    }

    /**
     * Additional filter for fields. Subclasses can override to exclude specific fields
     * from _fields, FVD, and hash calculation (e.g., EnumDescriptor excludes the ordinal field).
     *
     * @param f the field to check
     * @return true if the field should be included, false otherwise
     */
    protected boolean includeField(Field f) {
        return true;
    }

    public void init() {
        try {
            init0();
            super.init();

            if (_fields == null) {
                throw new RuntimeException("fields==null after init!");
            }

        } catch (INTERNAL internal) {
            throw internal;
        } catch (RuntimeException | Error ex) {
            throw as(INTERNAL::new, ex);
        }
    }

    private void init0() {
        final Class<?> superClass = type.getSuperclass();

        _is_rmi_stub = RMIStub.class.isAssignableFrom(type);
        _is_externalizable = Externalizable.class.isAssignableFrom(type);
        _is_serializable = Serializable.class.isAssignableFrom(type);

        _is_immutable_value = _immutable_value_classes.contains(type);

        if ((superClass != null) && (superClass != Object.class)) {
            TypeDescriptor superDesc = repo.getDescriptor(superClass);

            if (superDesc instanceof ValueDescriptor) {
                _super_descriptor = (ValueDescriptor) superDesc;
            }

        }

        doPrivileged((PrivilegedAction<Object>) () -> {
            _constructor = findConstructor();
            _fields = buildFieldDescriptors();
            _hash_code = computeHashCode();
            return null;
        });
    }

    private Optional<Method> findWriteReplaceMethod() {
        return doPrivileged((PrivilegedAction<Optional<Method>>) () -> {
            for (Class<?> curr = type; curr != null; curr = curr.getSuperclass()) {
                try {
                    Method method = curr.getDeclaredMethod("writeReplace");
                    method.setAccessible(true);
                    return Optional.of(method);
                } catch (NoSuchMethodException ignored) {
                }
            }
            return Optional.empty();
        });
    }

    private Optional<Method> findReadResolveMethod() {
        return doPrivileged((PrivilegedAction<Optional<Method>>) () -> {
            try {
                Method method = type.getDeclaredMethod("readResolve");
                method.setAccessible(true);
                return Optional.of(method);
            } catch (NoSuchMethodException ignored) {
            }
            return Optional.empty();
        });
    }

    private Optional<Method> findReadObjectMethod() {
        return doPrivileged((PrivilegedAction<Optional<Method>>) () -> {
            try {
                Method method = type.getDeclaredMethod("readObject", ObjectInputStream.class);
                
                // Validate the method
                int modifiers = method.getModifiers();
                if (!Modifier.isPrivate(modifiers) || Modifier.isStatic(modifiers)) {
                    return Optional.empty();
                }
                
                method.setAccessible(true);
                return Optional.of(method);
            } catch (NoSuchMethodException ignored) {
            }
            return Optional.empty();
        });
    }

    private Optional<Method> findWriteObjectMethod() {
        return doPrivileged((PrivilegedAction<Optional<Method>>) () -> {
            try {
                Method method = type.getDeclaredMethod("writeObject", ObjectOutputStream.class);
                
                // Validate the method
                int modifiers = method.getModifiers();
                if (!Modifier.isPrivate(modifiers) 
                        || Modifier.isStatic(modifiers) 
                        || method.getDeclaringClass() != type) {
                    return Optional.empty();
                }
                
                method.setAccessible(true);
                return Optional.of(method);
            } catch (NoSuchMethodException ignored) {
            }
            return Optional.empty();
        });
    }

    private Optional<Field> findSerialVersionUIDField() {
        return doPrivileged((PrivilegedAction<Optional<Field>>) () -> {
            try {
                Field field = type.getDeclaredField("serialVersionUID");
                if (Modifier.isStatic(field.getModifiers())) {
                    field.setAccessible(true);
                    return Optional.of(field);
                }
            } catch (NoSuchFieldException ignored) {
            }
            return Optional.empty();
        });
    }

    private ObjectStreamField[] findSerialPersistentFields() {
        try {
            Field field = type.getDeclaredField("serialPersistentFields");
            field.setAccessible(true);
            return (ObjectStreamField[]) field.get(null);
        } catch (IllegalAccessException | NoSuchFieldException ignored) {
        }
        return null;
    }

    private Optional<Constructor> findConstructor() {
        if (_is_externalizable) {
            return findExternalizableConstructor();
        } else if (_is_serializable && !type.isInterface()) {
            return findSerializableConstructor();
        }
        return Optional.empty();
    }

    private Optional<Constructor> findExternalizableConstructor() {
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

            Constructor<?> constructor = ReflectionFactory.getReflectionFactory()
                    .newConstructorForSerialization(type, initConstructor);

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
            return null;
        }
    }

    private boolean isConstructorAccessible(Constructor<?> constructor, Class<?> initClass) {
        int modifiers = constructor.getModifiers();
        if (Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers)) {
            return true;
        }
        
        if (!samePackage(type, initClass)) {
            MARSHAL_LOG.warning(() -> "Class " + type.getName() 
                    + " is not properly serializable. The default constructor of its first "
                    + "non-serializable super-class (" + initClass.getName() + ") is not accessible.");
            return false;
        }
        
        return true;
    }

    private FieldDescriptor[] buildFieldDescriptors() {
        if (!_is_serializable) return FieldDescriptor.EMPTY_ARRAY;

        ObjectStreamField[] serialPersistentFields = isEnum() ? null : findSerialPersistentFields();
        if (serialPersistentFields == null) {
            return buildFieldDescriptorsFromDeclaredFields();
        } else {
            return buildFieldDescriptorsFromSerialPersistentFields(serialPersistentFields);
        }
    }

    private FieldDescriptor[] buildFieldDescriptorsFromDeclaredFields() {
        return Arrays.stream(type.getDeclaredFields())
                .filter(this::isSerializableField)
                .filter(this::includeField)
                .peek(f -> f.setAccessible(true))
                .map(f -> FieldDescriptor.get(f, repo))
                .sorted()
                .toArray(FieldDescriptor[]::new);
    }

    private FieldDescriptor[] buildFieldDescriptorsFromSerialPersistentFields(ObjectStreamField[] serialPersistentFields) {
        FieldDescriptor[] fields = new FieldDescriptor[serialPersistentFields.length];

        for (int i = 0; i < serialPersistentFields.length; i++) {
            ObjectStreamField streamField = serialPersistentFields[i];
            FieldDescriptor fieldDescriptor = findMatchingField(streamField);
            
            if (fieldDescriptor == null) {
                fieldDescriptor = FieldDescriptor.getForSerialPersistentField(type, streamField, repo);
            }
            
            fields[i] = fieldDescriptor;
        }
        
        Arrays.sort(fields);
        return fields;
    }

    private FieldDescriptor findMatchingField(ObjectStreamField streamField) {
        try {
            Field reflectionField = type.getField(streamField.getName());
            reflectionField.setAccessible(true);

            if (reflectionField.getType() == streamField.getType()) {
                return FieldDescriptor.get(reflectionField, repo);
            }
        } catch (SecurityException | NoSuchFieldException ignored) {
        }
        
        return null;
    }

    private Class<?> getFirstNonSerializableSuperclass() {
        Class<?> initClass = type;

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
        return (_is_externalizable || getWriteObjectMethod().isPresent());
    }

    public boolean isChunked() {
        if (isCustomMarshalled()) return true;
        return (_super_descriptor != null) && _super_descriptor.isChunked();
    }

    Optional<Method> getWriteReplaceMethod() {
        return _write_replace_method.get();
    }

    Optional<Method> getReadResolveMethod() {
        return _read_resolve_method.get();
    }

    Optional<Method> getReadObjectMethod() {
        return _read_object_method.get();
    }

    Optional<Method> getWriteObjectMethod() {
        return _write_object_method.get();
    }

    Optional<Field> getSerialVersionUIDField() {
        return _serial_version_uid_field.get();
    }

    public Serializable writeReplace(Serializable val) {
        return getWriteReplaceMethod().map(method -> {
            try {
                return (Serializable) method.invoke(val);
            } catch (IllegalAccessException ex) {
                throw (MARSHAL) new MARSHAL("cannot call " + method).initCause(ex);
            } catch (IllegalArgumentException ex) {
                throw (MARSHAL) new MARSHAL(ex.getMessage()).initCause(ex);
            } catch (InvocationTargetException ex) {
                throw (UnknownException) new UnknownException(ex.getTargetException()).initCause(ex.getTargetException());
            }
        }).orElse(val);
    }

    public Serializable readResolve(Serializable val) {
        return getReadResolveMethod().map(method -> {
            try {
                return (Serializable) method.invoke(val);
            } catch (IllegalAccessException ex) {
                throw (MARSHAL) new MARSHAL("cannot call " + method).initCause(ex);
            } catch (IllegalArgumentException ex) {
                throw (MARSHAL) new MARSHAL(ex.getMessage()).initCause(ex);
            } catch (InvocationTargetException ex) {
                throw (UnknownException) new UnknownException(ex.getTargetException()).initCause(ex.getTargetException());
            }
        }).orElse(val);
    }

    public void writeValue(final OutputStream out, final Serializable value) {
        try {

            ObjectWriter writer = doPrivileged((PrivilegedAction<ObjectWriter>) () -> {
                try {
                    return new CorbaObjectWriter(out, value);
                } catch (IOException ex) {
                    throw (MARSHAL) new MARSHAL(ex.getMessage()).initCause(ex);
                }
            });

            writeValue(writer, value);

        } catch (IOException ex) {
            throw (MARSHAL) new MARSHAL(ex.getMessage()).initCause(ex);
        }
    }

    protected void defaultWriteValue(ObjectWriter writer, Serializable val) throws IOException {
        MARSHAL_OUT_LOG.finer(() -> "writing fields for " + type);
        FieldDescriptor[] fields = _fields;

        if (fields == null) return;

        for (FieldDescriptor field : fields) {
            MARSHAL_OUT_LOG.finer(() -> "writing field " + field.java_name);
            field.write(writer, val);
        }
    }

    protected void writeValue(ObjectWriter writer, Serializable val) throws IOException {

        if (_is_externalizable) {
            writer.invokeWriteExternal((Externalizable) val);
            return;
        }

        if (_super_descriptor != null) {
            _super_descriptor.writeValue(writer, val);
        }

        if (getWriteObjectMethod().isPresent()) {

            try {
                writer.invokeWriteObject(this, val, getWriteObjectMethod().get());
            } catch (IllegalAccessException | IllegalArgumentException ex) {
                throw (MARSHAL) new MARSHAL(ex.getMessage()).initCause(ex);
            } catch (InvocationTargetException ex) {
                throw (UnknownException) new UnknownException(ex.getTargetException()).initCause(ex.getTargetException());
            }

        } else {
            defaultWriteValue(writer, val);
        }

    }

    private Serializable createBlankInstance() {
        if (_constructor.isPresent()) {
            Constructor constructor = _constructor.get();
            try {
                return (Serializable) constructor.newInstance();

            } catch (IllegalAccessException ex) {
                throw (MARSHAL) new MARSHAL("cannot call " + constructor).initCause(ex);

            } catch (IllegalArgumentException | InstantiationException ex) {
                throw (MARSHAL) new MARSHAL(ex.getMessage()).initCause(ex);

            } catch (InvocationTargetException ex) {
                throw (UnknownException) new UnknownException(ex.getTargetException()).initCause(ex.getTargetException());

            } catch (NullPointerException ex) {
                MARSHAL_IN_LOG.log(WARNING, ex, () -> "unable to create instance of " + type.getName());
                MARSHAL_IN_LOG.warning(() -> "constructor => " + constructor);

                throw ex;
            }

        } else {
            return null;
        }
    }

    public Serializable readValue(final InputStream in, final Map<Integer, Serializable> offsetMap, final Integer offset) {
        final Serializable value = createBlankInstance();

        offsetMap.put(offset, value);

        try {
            ObjectReader reader = doPrivileged((PrivilegedAction<ObjectReader>) () -> {
                try {
                    return new CorbaObjectReader(in, offsetMap, value);
                } catch (IOException ex) {
                    throw (MARSHAL) new MARSHAL(ex.getMessage()).initCause(ex);
                }
            });

            readValue(reader, value);

            final Serializable resolved = readResolve(value);
            if (value != resolved) {
                offsetMap.put(offset, resolved);
            }
            return resolved;

        } catch (IOException ex) {
            throw (MARSHAL) new MARSHAL(ex.getMessage()).initCause(ex);
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

            pw.println(type.getName() + "@" + Integer.toHexString(key) + "[");

            printFields(pw, recurse, val);

            pw.println("]");
        }
    }

    void printFields(PrintWriter pw, Map recurse, Object val) {
        pw.print("(" + getClass().getName() + ")");

        if (_super_descriptor != null) {
            _super_descriptor.printFields(pw, recurse, val);
        }

        if (_fields == null)
            return;

        for (int i = 0; i < _fields.length; i++) {
            if (i != 0) {
                pw.print("; ");
            }

            _fields[i].print(pw, recurse, val);
        }

    }

    void defaultReadValue(ObjectReader reader, Serializable value) throws IOException {
        if (null == _fields) return;

        MARSHAL_IN_LOG.fine(() -> "reading fields for " + type.getName());

        for (FieldDescriptor _field : _fields) {
            MARSHAL_IN_LOG.fine(() -> "reading field " + _field.java_name + " of type " + _field.getType().getName() + " using " + _field.getClass().getName());

            try {
                _field.read(reader, value);
            } catch (MARSHAL ex) {
                if (ex.getMessage() != null)
                    throw ex;

                String msg = String.format("%s, while reading %s.%s", ex, java_name, _field.java_name);
                throw (MARSHAL) new MARSHAL(msg, ex.minor, ex.completed).initCause(ex);
            }
        }
    }

    Map readFields(ObjectReader reader) throws IOException {
        if ((_fields == null) || (_fields.length == 0)) {
            return EMPTY_MAP;
        }

        MARSHAL_IN_LOG.finer(() -> "reading fields for " + type.getName());

        Map map = new HashMap();

        for (FieldDescriptor _field : _fields) {

            MARSHAL_IN_LOG.finer(() -> "reading field " + _field.java_name);

            _field.readFieldIntoMap(reader, map);
        }

        return map;
    }

    void writeFields(ObjectWriter writer, Map fieldMap) throws IOException {
        if ((_fields == null) || (_fields.length == 0)) {
            return;
        }

        MARSHAL_OUT_LOG.finer(() -> "writing fields for " + type.getName());

        for (FieldDescriptor _field : _fields) {

            MARSHAL_OUT_LOG.finer(() -> "writing field " + _field.java_name);

            _field.writeFieldFromMap(writer, fieldMap);
        }

    }

    /**
     * This method reads the fields of a single class slice.
     */
    protected void readValue(ObjectReader reader, Serializable value) throws IOException {
        if (_is_externalizable) {
            try {
                reader.readExternal((Externalizable) value);
            } catch (ClassNotFoundException e) {
                throw new IOException("cannot instantiate class", e);
            }
            return;
        }

        if (_super_descriptor != null) {
            _super_descriptor.readValue(reader, value);
        }

        // check whether the class (not its ancestors) does any custom marshalling
        if (getWriteObjectMethod().isPresent()) {
            // read custom marshalling value header
            byte cmsfVersion = reader.readByte(); // custom marshal stream format version
            boolean dwoCalled = reader.readBoolean(); // was defaultWriteObject() called?
            MARSHAL_IN_LOG.log(Level.FINE, "Reading value in streamFormatVersion=" + cmsfVersion + " defaultWriteObject=" + dwoCalled);

            if (cmsfVersion == 2) {
                // use a wrapped reader to open the secondary custom valuetype
                ObjectReader wrapper = CustomMarshaledObjectReader.wrap(reader);
                readSerializable(getReadObjectMethod().isPresent() ? wrapper : reader, value);
                // invoke close to skip to the end of the secondary custom valuetype
                wrapper.close();
                return;
            }
        }

        readSerializable(reader, value);

    }

    private void readSerializable(ObjectReader reader, Serializable value) throws IOException {
        if (getReadObjectMethod().isPresent()) {
            Method method = getReadObjectMethod().get();
            try {
                reader.setCurrentValueDescriptor(this);
                method.invoke(value, reader);
                reader.setCurrentValueDescriptor(null);
            } catch (IllegalAccessException | IllegalArgumentException ex) {
                throw (MARSHAL) new MARSHAL(ex.getMessage()).initCause(ex);
            } catch (InvocationTargetException ex) {
                throw (UnknownException) new UnknownException(ex.getTargetException()).initCause(ex.getTargetException());
            }
        } else {
            defaultReadValue(reader, value);
        }
    }

    protected long computeHashCode() {
        Class type = this.type;

        if (_is_externalizable) {
            return 1L;
        }

        if (!Serializable.class.isAssignableFrom(type)) {
            return 0;
        }

        long hash = 0L;
        try {
            ByteArrayOutputStream barr = new ByteArrayOutputStream(512);
            MessageDigest md = MessageDigest.getInstance("SHA");
            DigestOutputStream digestout = new DigestOutputStream(barr, md);
            DataOutputStream out = new DataOutputStream(digestout);

            Class superType = type.getSuperclass();
            if (superType != null) {
                TypeDescriptor desc = repo.getDescriptor(superType);
                out.writeLong(desc.getHashCode());
            }

            out.writeInt(getWriteObjectMethod().isPresent() ? 2 : 1);

            FieldDescriptor[] fds = new FieldDescriptor[_fields.length];
            System.arraycopy(_fields, 0, fds, 0, _fields.length);

            if (fds.length > 1)
                Arrays.sort(fds, compareByName);

            for (FieldDescriptor f : fds) {
                out.writeUTF(f.java_name);
                out.writeUTF(makeSignature(f.getType()));
            }

            /*
             * Field[] fields = type.getDeclaredFields (); if (fields.length >
             * 1) java.util.Arrays.sort (fields, compareByName); for(int i = 0;
             * i < fields.length; i++) { Field f = fields[i]; int mod =
             * f.getModifiers (); if (!Modifier.isTransient(mod) &&
             * !Modifier.isStatic (mod)) { out.writeUTF(f.getName());
             * out.writeUTF( makeSignature (f.getType ())); } }
             */

            out.flush();

            byte[] data = md.digest();
            int end = Math.min(8, data.length);
            for (int j = 0; j < end; j++) {
                hash += (long) (data[j] & 0xff) << (j * 8);
            }
        } catch (Exception ex) {
            throw new RuntimeException("cannot compute RMI hash code", ex);
        }

        return hash;
    }

    private static final Comparator<FieldDescriptor> compareByName = Comparator.comparing(f -> f.java_name);

    long getHashCode() {
        return _hash_code;
    }

    private final LazyReference<ValueMember[]> valueMembers = new LazyReference<>(this::genValueMembers);
    
    protected ValueMember[] genValueMembers() {
        return Arrays.stream(_fields)
                .map(field -> field.getValueMember(repo))
                .toArray(ValueMember[]::new);
    }
    
    final ValueMember[] getValueMembers() {
        getTypeCode(); // ensure recursion through typecode
        return valueMembers.get();
    }

    @Override
    protected TypeCode genTypeCode() {
        ORB orb = ORB.init();
        setTypeCode(orb.create_recursive_tc(getRepositoryID()));

        TypeCode _base = ((_super_descriptor == null) ? null : _super_descriptor.getTypeCode());

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
        fvd.name = type.getName();
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
        fvd.base_value = ((_super_descriptor == null) ? "" : _super_descriptor.getRepositoryID());
        fvd.type = getTypeCode();
        return fvd;
    }



    public boolean copyWithinState() {
        return !(_is_immutable_value | _is_rmi_stub);
    }

    Object copyObject(Object orig, CopyState state) {

        if (_is_immutable_value || _is_rmi_stub) {
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

            MARSHAL_OUT_LOG.finer(() -> "writeReplace -> " + type.getName());
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
            String msg = String.format("%s writing %s", ex, type.getName());
            throw (MARSHAL) new MARSHAL(msg).initCause(ex);
        }
    }

    private Serializable readObject(ObjectWriter writer, Serializable copy) {
        try {
            ObjectReader reader = writer.getObjectReader(copy);
            readValue(reader, copy);
            return readResolve(copy);
        } catch (IOException ex) {
            String msg = String.format("%s reading instance of %s", ex, type.getName());
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
        MethodDescriptor.writeJavaType(pw, type);
        pw.print(".class)");
    }

    void writeUnmarshalValue(PrintWriter pw, String inName) {
        pw.print(inName);
        pw.print('.');
        pw.print("read_value");
        pw.print('(');
        MethodDescriptor.writeJavaType(pw, type);
        pw.print(".class)");
    }

    @Override
    void addDependencies(Set<Class<?>> classes) {
        Class c = type;

        if ((c == Object.class) || classes.contains(c))
            return;

        classes.add(c);

        if (c.getSuperclass() != null) {
            TypeDescriptor desc = repo.getDescriptor(c.getSuperclass());
            desc.addDependencies(classes);
        }

        Class[] ifaces = c.getInterfaces();
        for (Class iface : ifaces) {
            TypeDescriptor desc = repo.getDescriptor(iface);
            desc.addDependencies(classes);
        }

        if (_fields != null) {
            for (FieldDescriptor _field : _fields) {
                if (_field.isPrimitive())
                    continue;

                TypeDescriptor desc = repo.getDescriptor(_field.type);
                desc.addDependencies(classes);
            }
        }
    }
}
