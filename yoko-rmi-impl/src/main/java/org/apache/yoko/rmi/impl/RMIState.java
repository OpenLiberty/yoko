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

import org.apache.yoko.rmi.api.PortableRemoteObjectExt;
import org.apache.yoko.rmi.api.PortableRemoteObjectState;
import org.apache.yoko.rmi.util.stub.MethodRef;
import org.apache.yoko.rmi.util.stub.StubClass;
import org.omg.CORBA.BAD_INV_ORDER;
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.ORB;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.CORBA.Policy;
import org.omg.CORBA.portable.Delegate;
import org.omg.CORBA.portable.ObjectImpl;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAManagerPackage.AdapterInactive;
import org.omg.PortableServer.POAPackage.AdapterAlreadyExists;
import org.omg.PortableServer.POAPackage.InvalidPolicy;

import javax.rmi.CORBA.Stub;
import javax.rmi.CORBA.Tie;
import javax.rmi.CORBA.Util;
import javax.rmi.PortableRemoteObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.security.PrivilegedActionException;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static java.security.AccessController.doPrivileged;
import static java.util.Arrays.stream;
import static java.util.Collections.synchronizedMap;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.FINER;
import static java.util.logging.Level.WARNING;
import static java.util.stream.Stream.concat;
import static org.apache.yoko.util.Arrays.emptyArray;
import static org.apache.yoko.util.Exceptions.as;
import static org.apache.yoko.util.PrivilegedActions.GET_CONTEXT_CLASS_LOADER;
import static org.apache.yoko.util.PrivilegedActions.getClassLoader;
import static org.apache.yoko.util.PrivilegedActions.getDeclaredMethod;
import static org.apache.yoko.util.PrivilegedActions.getNoArgConstructor;

public class RMIState implements PortableRemoteObjectState {
    static final Logger logger = Logger.getLogger(RMIState.class.getName());

    private boolean isShutdown;

    final private ORB _orb;

    private final String _name;

    final TypeRepository repo = TypeRepository.get();

    private POA poa;

    POA getPOA() {
	return poa;
    }

    RMIState(ORB orb, String name) {
        Objects.requireNonNull(orb, "ORB is null");

        try {
            POA rootPoa = (POA) orb.resolve_initial_references("RootPOA");
            poa = rootPoa.create_POA(name, null, emptyArray(Policy.class));
            poa.the_POAManager().activate();
        } catch (AdapterAlreadyExists e) {
            logger.log(WARNING, e, () -> "Adapter already exists");
        } catch (InvalidPolicy e) {
            logger.log(WARNING, e, () -> "Invalid policy");
        } catch (InvalidName e) {
            logger.log(WARNING, e, () -> "Invalid name");
        } catch (AdapterInactive e) {
            logger.log(WARNING, e, () -> "Adapter inactive");
        }

        _orb = orb;
        _name = name;
    }

    void checkShutDown() {
        if (isShutdown) {
            BAD_INV_ORDER ex = new BAD_INV_ORDER("RMIState has already been shut down");
            logger.fine(() -> "RMIState has already been shut down " + ex);
            throw ex;
        }
    }

    public void shutdown() {
        logger.finer(() -> "RMIState shutdown requested; name = " + _name);
        checkShutDown();
        isShutdown = true;
    }

    public ORB getORB() {
        return _orb;
    }

    Delegate createDelegate(RMIServant servant) {
        checkShutDown();

        byte[] id = servant._id;
        RemoteDescriptor desc = servant._descriptor;
        final String repid = desc.getRepositoryID();
        final POA poa = getPOA();
        try {
            final ObjectImpl ref = (ObjectImpl) poa.create_reference_with_id(id, repid);
            return ref._get_delegate();
        } catch (BAD_PARAM ex) {
            throw as(InternalError::new, ex, "wrong policy: " + ex.getMessage());
        }

    }

    static RMIState current() {
        return (RMIState) PortableRemoteObjectExt.getState();
    }

    /**
     * data for use in PortableRemoteObjectImpl
     */
    final ClassValue<Constructor<? extends Stub>> stubConstructors = new ClassValue<Constructor<? extends Stub>>() {
        @Override
        protected Constructor<? extends Stub> computeValue(Class<?> type) {
            return computeRMIStubConstructor(type);
        }
    };

    /**
     * data for use in UtilImpl
     */
    final Map<Remote, Tie> tie_map = synchronizedMap(new IdentityHashMap<>());

    private final ClassValue<Optional<Constructor<? extends Stub>>> staticStubConstructors = new ClassValue<Optional<Constructor<? extends Stub>>>() {
        @Override
        protected Optional<Constructor<? extends Stub>> computeValue(Class<?> type) {
            String stubClassName = getStubClassName(type);
            Constructor<? extends Stub> cons = getStubConstructor(stubClassName);
            return Optional.ofNullable(null == cons ? getStubConstructor(getOldStubClassName(stubClassName)) : cons);
        }
    };

    private Constructor<? extends Stub> computeRMIStubConstructor(Class<?> type) {
        if (!type.isInterface()) {
            throw new RuntimeException("non-interfaces not supported");
        }

        logger.fine(() -> "Computing RMI stub constructor for class " + type.getName());

        final ClassLoader loader = doPrivileged(getClassLoader(type));
        final ClassLoader contextLoader = doPrivileged(GET_CONTEXT_CLASS_LOADER);

        logger.finer(() -> "TYPE ----> " + type);
        logger.finer(() -> "LOADER --> " + loader);
        logger.finer(() -> "CONTEXT -> " + contextLoader);

        final RemoteDescriptor desc = repo.getRemoteInterface(type);
        final MethodDescriptor[] descriptors = desc.getMethods();

        final Stream<Method> methodStream = stream(descriptors)
                .peek((m) -> logger.finer("Method ----> " + m))
                .map(MethodDescriptor::getReflectedMethod);

        // Get STUB_WRITE_REPLACE_METHOD from RMIStub
        final Method stubWriteReplaceMethod;
        try {
            stubWriteReplaceMethod = doPrivileged(getDeclaredMethod(RMIStub.class, "writeReplace"));
        } catch (PrivilegedActionException pae) {
            throw new RuntimeException("Cannot access writeReplace method", pae.getCause());
        }

        final MethodRef[] methods = concat(methodStream, Stream.of(stubWriteReplaceMethod))
                .map(MethodRef::new)
                .toArray(MethodRef[]::new);

        Optional<Class<? extends Stub>> stubClass;
        try {
            stubClass = Optional.ofNullable(StubClass.make(type, descriptors, methods, loader));
        } catch (NoClassDefFoundError ex) {
            try {
                stubClass = Optional.ofNullable(StubClass.make(type, descriptors, methods, contextLoader));
            } catch (NoClassDefFoundError e) {
                e.addSuppressed(ex);
                throw e;
            }
        }

        return stubClass.map(clazz -> {
            try {
                return clazz.getConstructor();
            } catch (NoSuchMethodException e) {
                logger.log(FINER, e, () -> "constructed stub has no default constructor");
                return null;
            }
        }).orElse(null);
    }

    public Stub getStaticStub(String ignored, Class<?> type) {
        Optional<Constructor<? extends Stub>> entry = staticStubConstructors.get(type);
        return entry.map(this::createStub).orElse(null);
    }

    private Stub createStub(Constructor<? extends Stub> constructor) {
        try {
            return constructor.newInstance();
        } catch (ClassCastException ex) {
            logger.log(FINE, ex, () -> "loaded class " + constructor.getDeclaringClass() + " is not a proper stub");
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException ex) {
            logger.log(FINE, ex, () -> "cannot instantiate stub class for " + constructor.getDeclaringClass() + " :: " + ex.getMessage());
        }
        return null;
    }

    private Constructor<? extends Stub> getStubConstructor(String stubClassName) {
        Constructor<? extends Stub> cons = findConstructor(stubClassName);
        if (cons == null || Stub.class.isAssignableFrom(cons.getDeclaringClass())) return cons;
        logger.fine(() -> "class " + cons.getDeclaringClass() + " is not a javax.rmi.CORBA.Stub");
        return null;
    }

    private Constructor<? extends Stub> findConstructor(String stubName) {
        try {
            @SuppressWarnings("unchecked") Class<? extends Stub> stubClass = (Class<? extends Stub>) Util.loadClass(stubName, null, doPrivileged(GET_CONTEXT_CLASS_LOADER));
            return doPrivileged(getNoArgConstructor(stubClass));
        } catch (ClassNotFoundException ex) {
            logger.log(FINE, ex, () -> "failed to load remote class " + stubName + " from " + null);
        } catch (PrivilegedActionException e) {
            logger.log(WARNING, e, () -> "stub class " + stubName + " has no default constructor");
        }
        return null;
    }

    private String getStubClassName(Class<?> c) {
        String cname = c.getName();
        int idx = cname.lastIndexOf('.');
        if (idx == -1) return String.format("org.omg.stub._%s_Stub", cname);
        return String.format("org.omg.stub.%s_%s_Stub", cname.substring(0, idx + 1), cname.substring(idx + 1));
    }

    private String getOldStubClassName(String stubClassName) {
        return stubClassName.substring("org.omg.stub.".length());
    }

    public void exportObject(Remote remote) throws RemoteException {
        PortableRemoteObject.exportObject(remote);
    }

    public void unexportObject(Remote remote) throws RemoteException {
        PortableRemoteObject.unexportObject(remote);
    }

    public String getName() {
        return _name;
    }
}
