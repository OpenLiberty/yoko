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

import org.apache.yoko.rmi.impl.TypeDescriptor.FullKey;
import org.apache.yoko.rmi.impl.TypeDescriptor.SimpleKey;
import org.apache.yoko.rmi.util.Key;
import org.apache.yoko.rmi.util.SearchKey;
import org.apache.yoko.rmi.util.WeakKey;
import org.apache.yoko.util.concurrent.LazyReference;
import org.apache.yoko.util.yasf.Yasf;
import org.omg.CORBA.MARSHAL;
import org.omg.CORBA.ValueDefPackage.FullValueDescription;
import org.omg.CORBA.portable.IDLEntity;
import org.omg.SendingContext.CodeBase;
import org.omg.SendingContext.CodeBaseHelper;
import org.omg.SendingContext.RunTime;

import javax.rmi.CORBA.ClassDesc;
import java.io.Externalizable;
import java.io.Serializable;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;

import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;

public class TypeRepository {
    static final Logger logger = Logger.getLogger(TypeRepository.class.getName());

    private static final class TypeDescriptorCache {
        private final ConcurrentMap<Key<? extends SimpleKey>, WeakReference<TypeDescriptor>> map = new ConcurrentHashMap<>();
        private final ReferenceQueue<FullKey> staleKeys = new ReferenceQueue<>();

        public TypeDescriptor get(String repid) {
            cleanStaleKeys();
            WeakReference<TypeDescriptor> ref = map.get(new SearchKey<>(new SimpleKey(repid)));
            return (null == ref) ? null : ref.get();
        }

        public TypeDescriptor get(String repid, Class<?> localType) {
            cleanStaleKeys();
            WeakReference<TypeDescriptor> ref = map.get(new SearchKey<>(new FullKey(repid, localType)));
            return (null == ref) ? null : ref.get();
        }

        public void put(TypeDescriptor typeDesc) {
            cleanStaleKeys();
            final WeakReference<TypeDescriptor> value = new WeakReference<>(typeDesc);
            map.putIfAbsent(new WeakKey<>(typeDesc.getKey(), staleKeys), value);
        }

        private void cleanStaleKeys() {
            for (Reference<?> staleKey = staleKeys.poll(); staleKey != null; staleKey = staleKeys.poll()) {
                map.remove((WeakKey<?>)staleKey);
            }
        }
    }

    private static final class LocalDescriptors extends ClassValue<TypeDescriptor> {
        private static final class Raw extends ClassValue<TypeDescriptor> {

            private final TypeRepository repo;
            private final Map<Class<?>, Supplier<TypeDescriptor>> simpleFactories;

            Raw(TypeRepository repo) {
                this.repo = repo;
                this.simpleFactories = genSimpleFactories();
            }

            private Map<Class<?>, Supplier<TypeDescriptor>> genSimpleFactories() {
                Map<Class<?>, Supplier<TypeDescriptor>> map = new HashMap<>();
                // Primitive types
                map.put(boolean.class, () -> new PrimitiveDescriptor(boolean.class, repo, "boolean", org.omg.CORBA.TCKind.tk_boolean, org.omg.CORBA.portable.InputStream::read_boolean, (out, val) -> out.write_boolean((Boolean)val)));
                map.put(byte.class, () -> new PrimitiveDescriptor(byte.class, repo, "octet", org.omg.CORBA.TCKind.tk_octet, org.omg.CORBA.portable.InputStream::read_octet, (out, val) -> out.write_octet((Byte)val)));
                map.put(short.class, () -> new PrimitiveDescriptor(short.class, repo, "short", org.omg.CORBA.TCKind.tk_short, org.omg.CORBA.portable.InputStream::read_short, (out, val) -> out.write_short((Short)val)));
                map.put(char.class, () -> new PrimitiveDescriptor(char.class, repo, "wchar", org.omg.CORBA.TCKind.tk_wchar, org.omg.CORBA.portable.InputStream::read_wchar, (out, val) -> out.write_wchar((Character)val)));
                map.put(int.class, () -> new PrimitiveDescriptor(int.class, repo, "long", org.omg.CORBA.TCKind.tk_long, org.omg.CORBA.portable.InputStream::read_long, (out, val) -> out.write_long((Integer)val)));
                map.put(long.class, () -> new PrimitiveDescriptor(long.class, repo, "long_long", org.omg.CORBA.TCKind.tk_longlong, org.omg.CORBA.portable.InputStream::read_longlong, (out, val) -> out.write_longlong((Long)val)));
                map.put(float.class, () -> new PrimitiveDescriptor(float.class, repo, "float", org.omg.CORBA.TCKind.tk_float, org.omg.CORBA.portable.InputStream::read_float, (out, val) -> out.write_float((Float)val)));
                map.put(double.class, () -> new PrimitiveDescriptor(double.class, repo, "double", org.omg.CORBA.TCKind.tk_double, org.omg.CORBA.portable.InputStream::read_double, (out, val) -> out.write_double((Double)val)));
                map.put(void.class, () -> new PrimitiveDescriptor(void.class, repo, "void", org.omg.CORBA.TCKind.tk_void, in -> null, (out, val) -> {}));
                // Other simple types
                map.put(String.class, () -> new StringDescriptor(repo));
                map.put(Class.class, () -> new ClassDescriptor(repo));
                map.put(ClassDesc.class, () -> new ClassDescDescriptor(repo));
                map.put(Date.class, () -> new DateValueDescriptor(repo));
                map.put(Enum.class, () -> new EnumDescriptor(Enum.class, repo));
                // Static any types
                map.put(Object.class, () -> new AnyDescriptor(Object.class, repo));
                map.put(Externalizable.class, () -> new AnyDescriptor(Externalizable.class, repo));
                map.put(Serializable.class, () -> new AnyDescriptor(Serializable.class, repo));
                map.put(Remote.class, () -> new AnyDescriptor(Remote.class, repo));
                return unmodifiableMap(map);
            }

            @Override
            protected TypeDescriptor computeValue(Class<?> type) {
                // Check for exact type matches in simple factories map
                Supplier<TypeDescriptor> factory = simpleFactories.get(type);
                if (factory != null) {
                    return factory.get();
                }

                // Handle types requiring assignability checks
                if ((IDLEntity.class.isAssignableFrom(type)) && isIDLEntity(type)) {
                    return new IDLEntityDescriptor(type, repo);
                } else if (Throwable.class.isAssignableFrom(type)) {
                    return new ExceptionDescriptor(type, repo);
                } else if (Enum.class.isAssignableFrom(type)) {
                    Class<?> enumType = EnumSubclassDescriptor.getEnumType(type);
                    return ((enumType == type) ? new EnumSubclassDescriptor(type, repo) : get(enumType));
                } else if (type.isArray()) {
                    return ArrayDescriptor.get(type, repo);
                } else if ((!type.isInterface()) && Serializable.class.isAssignableFrom(type)) {
                    return new ValueDescriptor(type, repo);
                } else if (Remote.class.isAssignableFrom(type)) {
                    if (type.isInterface()) {
                        return new RemoteInterfaceDescriptor(type, repo);
                    } else {
                        return new RemoteClassDescriptor(type, repo);
                    }
                } else if (Object.class.isAssignableFrom(type)) {
                    if (isAbstractInterface(type)) {
                        logger.finer(() -> "encoding " + type + " as abstract interface");
                        return new AbstractObjectDescriptor(type, repo);
                    } else {
                        logger.finer(() -> "encoding " + type + " as a abstract value");
                        return new ValueDescriptor(type, repo);
                    }
                } else {
                    throw new RuntimeException("cannot handle class " + type.getName());
                }
            }

            private static boolean isIDLEntity(Class<?> type) {
                for (Class<?> intf : type.getInterfaces()) {
                    if (intf.equals(IDLEntity.class)) return true;
                }
                return false;
            }

            private static boolean isAbstractInterface(Class<?> type) {
                if (!type.isInterface()) return false;

                for (Class<?> intf : type.getInterfaces()) {
                    if (!isAbstractInterface(intf)) return false;
                }

                for (Method method : type.getDeclaredMethods()) {
                    if (!isRemoteMethod(method)) return false;
                }

                return true;
            }

            private static boolean isRemoteMethod(java.lang.reflect.Method m) {
                for (Class<?> exceptionType : m.getExceptionTypes()) {
                    if (exceptionType.isAssignableFrom(RemoteException.class)) return true;
                }

                return false;
            }

        }

        private final Raw rawValues;

        LocalDescriptors(TypeRepository repo) {
            rawValues = new Raw(repo);
        }
        @Override
        protected synchronized TypeDescriptor computeValue(Class<?> type) {
            return rawValues.get(type).getInitialized();
        }

    }

    private static final class FvdRepIdDescriptorMaps extends ClassValue<ConcurrentMap<String,ValueDescriptor>> {

        @Override
        protected ConcurrentMap<String,ValueDescriptor> computeValue(
                Class<?> type) {
            return new ConcurrentHashMap<>(1);
        }
    }

    private final TypeDescriptorCache repIdDescriptors;
    private final LocalDescriptors localDescriptors;
    private final FvdRepIdDescriptorMaps fvdDescMaps = new FvdRepIdDescriptorMaps();
    private final ConcurrentMap<String,ValueDescriptor> noTypeDescMap = new ConcurrentHashMap<>();

    private static final Set<Class<?>> initTypes;

    static {
        initTypes = createClassSet(Object.class, String.class, ClassDesc.class, Date.class,
                Externalizable.class, Serializable.class, Remote.class);
    }

    private static Set<Class<?>> createClassSet(Class<?>...types) {
        return unmodifiableSet(new HashSet<>(Arrays.asList(types)));
    }

    private TypeRepository() {
        repIdDescriptors = new TypeDescriptorCache();
        addToRepIdDescriptors = repIdDescriptors::put;
        localDescriptors = new LocalDescriptors(this);

        for (Class<?> type: initTypes) localDescriptors.get(type);
    }

    private static final LazyReference<TypeRepository> INSTANCE = new LazyReference<>(TypeRepository::new);

    final Consumer<TypeDescriptor> addToRepIdDescriptors;
    public static TypeRepository get() {
        return INSTANCE.get();
    }

    public String getRepositoryID(Class<?> type) {
        return getDescriptor(type).getRepositoryID();
    }

    RemoteInterfaceDescriptor getRemoteInterface(Class<?> type) {
        return getDescriptor(type).getRemoteInterface();
    }

    TypeDescriptor getDescriptor(Class<?> type) {
        logger.fine(() -> String.format("Requesting type descriptor for class \"%s\"", type.getName()));
        final TypeDescriptor desc = localDescriptors.get(type);
        logger.fine(() -> String.format("Class \"%s\" resolves to %s", type.getName(), desc));
        return desc;
    }

    TypeDescriptor getDescriptor(String repId) {
        logger.fine(() -> String.format("Requesting type descriptor for repId \"%s\"", repId));
        final TypeDescriptor desc = repIdDescriptors.get(repId);
        logger.fine(() -> String.format("RepId \"%s\" resolves to %s", repId, desc));
        return desc;
    }

    /**
     * @param clz (local) class we are interested in
     * @param repid  repository id from GIOP input for the remote class
     * @param runtime way to look up the complete remote descriptor
     * @return ValueDescriptor
     * @throws ClassNotFoundException  something might go wrong.
     */
    ValueDescriptor getDescriptor(Class<?> clz, String repid, RunTime runtime) throws ClassNotFoundException {
        if (repid == null) {
            return (ValueDescriptor) getDescriptor(clz);
        }

        ValueDescriptor clzdesc = (ValueDescriptor) repIdDescriptors.get(repid, clz);
        if (clzdesc != null) {
            return clzdesc;
        }

        if (clz != null) {
            logger.fine(() -> "Requesting type descriptor for class " + clz.getName() + " with repid " + repid);
            // special handling for array value types.
            if (clz.isArray()) {
                //TODO don't we need to look up the FVD for the array element?
                return (ValueDescriptor) localDescriptors.get(clz);
            }
            clzdesc = (ValueDescriptor) getDescriptor(clz);
            if (clzdesc.isEnum() && Yasf.ENUM_FIX_1.isSupported() && !Yasf.ENUM_TRUE_HASH_AND_FVD.isSupported()) {
                // The FVD from the other end say it sends 'name' and 'ordinal', but will _actually_
                // only send 'name'.
                // So just use the local descriptor, which only expects to read 'name'.
                return clzdesc;
            }
            String localID = clzdesc.getRepositoryID();

            if (repid.equals(localID)) {
                return clzdesc;
            }
            //One might think that java serialization compatibility (same SerialVersionUID) would mean corba
            //serialization compatibility.  However, one implementation might have a writeObject method and the
            //other implementation not.  This is recorded only in the isCustomMarshall of the source value
            //descriptor, so we have to fetch it to find out.  A custom marshall value has a couple extra bytes
            // and padding and these can't be reliably identified without this remote info.  cf YOKO-434.
        }

        logger.fine(() -> "Requesting type descriptor for repid " + repid);
        CodeBase codebase = CodeBaseHelper.narrow(runtime);
        if (codebase == null) {
            throw new MARSHAL("cannot locate RunTime CodeBase");
        }

        FullValueDescription fvd = codebase.meta(repid);

        final ValueDescriptor superDesc = "".equals(fvd.base_value) ? null :
            getDescriptor(null == clz ? null : clz.getSuperclass(), fvd.base_value, codebase);

        final ValueDescriptor newDesc = FVDValueDescriptor.create(fvd, clz, this, repid, superDesc);

        ConcurrentMap<String, ValueDescriptor> remoteDescMap = (null == clz) ? noTypeDescMap : fvdDescMaps.get(clz);
        clzdesc = remoteDescMap.putIfAbsent(newDesc.getRepositoryID(), newDesc);
        if (null == clzdesc) {
            clzdesc = newDesc;
            repIdDescriptors.put(clzdesc);
        }

        return clzdesc;
    }
}
