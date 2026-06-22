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

import org.apache.yoko.rmi.util.ClientUtil;
import org.omg.CORBA.BAD_INV_ORDER;
import org.omg.CORBA.BAD_OPERATION;
import org.omg.CORBA.portable.Delegate;
import org.omg.CORBA.portable.IDLEntity;
import org.omg.CORBA.portable.ObjectImpl;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAPackage.ServantAlreadyActive;
import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;

import javax.rmi.CORBA.PortableRemoteObjectDelegate;
import javax.rmi.CORBA.Stub;
import javax.rmi.CORBA.Tie;
import javax.rmi.CORBA.Util;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.security.PrivilegedActionException;
import java.util.logging.Logger;

import static java.security.AccessController.doPrivileged;
import static org.apache.yoko.logging.VerboseLogging.wrapped;
import static org.apache.yoko.util.Exceptions.as;
import static org.apache.yoko.util.PrivilegedActions.getClassLoader;
import static org.apache.yoko.util.PrivilegedActions.getDeclaredMethod;
import static org.apache.yoko.util.PrivilegedActions.getMethod;


public class PortableRemoteObjectImpl implements PortableRemoteObjectDelegate {
    static final Logger LOGGER = Logger.getLogger(PortableRemoteObjectImpl.class.getName());

    enum StubWriteReplaceMethodHolder {
        ;
        static final Method STUB_WRITE_REPLACE_METHOD;
        static {
            try {
                STUB_WRITE_REPLACE_METHOD = doPrivileged(getDeclaredMethod(RMIStub.class, "writeReplace"));
            } catch (PrivilegedActionException pae) {
                Throwable t = pae.getCause();
                throw wrapped(LOGGER, t, "cannot initialize: \n" + t.getMessage(), e -> new Error(t));
            }
        }
    }


    private static final ClassValue<MethodHandle> HELPER_NARROW_CACHE = new ClassValue<MethodHandle>() {
        @Override
        protected MethodHandle computeValue(Class<?> helperClass) {
            try {
                final Method helperNarrow = doPrivileged(getMethod(helperClass, "narrow", org.omg.CORBA.Object.class));
                return MethodHandles.lookup().unreflect(helperNarrow);
            } catch (Exception e) {
                throw as(ClassCastException::new, e, helperClass.getName());
            }
        }
    };

    public void connect(Remote target, Remote source) throws RemoteException {
        source = toStub(source);

        ObjectImpl obj;
        if (target instanceof ObjectImpl) {
            obj = (ObjectImpl) target;
        } else {
            try {
                exportObject(target);
            } catch (RemoteException ignored) {}
            try {
                obj = (ObjectImpl) toStub(target);
            } catch (NoSuchObjectException ex) {
                throw as(RemoteException::new, ex, "cannot convert to stub!");
            }
        }

        try {
            ((Stub) source).connect(obj._orb());
        } catch (BAD_OPERATION bad_operation) {
            throw as(RemoteException::new, bad_operation, bad_operation.getMessage());
        }
    }

    private Object narrowRMI(ObjectImpl narrowFrom, Class<?> narrowTo) {
        LOGGER.fine(() -> String.format("RMI narrowing %s => %s", narrowFrom.getClass().getName(), narrowTo.getName()));

        RMIState state = RMIState.current();

        Stub stub;
        try {
            stub = createStub(state, narrowTo);
        } catch (NoClassDefFoundError ex) {
            throw as(ClassCastException::new, ex, narrowTo.getName());
        }

        Delegate delegate;
        try {
            // let the stub adopt narrowFrom's identity
            delegate = narrowFrom._get_delegate();

        } catch (BAD_OPERATION ex) {
            // ignore
            delegate = null;
        }

        stub._set_delegate(delegate);

        return stub;
    }

    private Object narrowIDL(ObjectImpl narrowFrom, Class<?> narrowTo) {
        LOGGER.fine(() -> String.format("IDL narrowing %s => %s", narrowFrom.getClass().getName(), narrowTo.getName()));
        try {
            final ClassLoader idlClassLoader = doPrivileged(getClassLoader(narrowTo));
            final String helperClassName = narrowTo.getName() + "Helper";
            final Class<?> helperClass = Util.loadClass(helperClassName, null, idlClassLoader);
            MethodHandle narrowHandle = HELPER_NARROW_CACHE.get(helperClass);
            return narrowHandle.invoke(narrowFrom);
        } catch (Error e) {
            throw e;
        } catch (Throwable t) {
            throw as(ClassCastException::new, t, narrowTo.getName());
        }
    }

    public Object narrow(Object narrowFrom, @SuppressWarnings("rawtypes") Class narrowTo)
            throws ClassCastException {
        if (narrowFrom == null)
            return null;

        if (narrowTo.isInstance(narrowFrom))
            return narrowFrom;

        final String fromClassName = narrowFrom.getClass().getName();
        final String toClassName = narrowTo.getName();
        LOGGER.finer(() -> String.format("narrow %s => %s", fromClassName, toClassName));

        if (!(narrowFrom instanceof ObjectImpl))
            throw new ClassCastException(String.format(
                    "object to narrow (runtime type %s) is not an instance of %s",
                    fromClassName, ObjectImpl.class.getName()));
        if (!narrowTo.isInterface())
            throw new ClassCastException(String.format("%s is not an interface", toClassName));

        final boolean isRemote = Remote.class.isAssignableFrom(narrowTo);
        final boolean isIDLEntity = IDLEntity.class.isAssignableFrom(narrowTo);

        if (isRemote && isIDLEntity)
            throw new ClassCastException(String.format(
                    "%s invalidly extends both %s and %s",
                    toClassName, Remote.class.getName(), IDLEntity.class.getName()));
        if (isRemote)
            return narrowRMI((ObjectImpl) narrowFrom, narrowTo);
        if (isIDLEntity)
            return narrowIDL((ObjectImpl) narrowFrom, narrowTo);

        throw new ClassCastException(String.format(
                "%s extends neither %s nor %s",
                toClassName, Remote.class.getName(), IDLEntity.class.getName()));
    }

    static Remote narrow1(RMIState state, ObjectImpl object, Class<?> narrowTo) throws ClassCastException {
        Stub stub;

        try {
            stub = createStub(state, narrowTo);
        } catch (NoClassDefFoundError ex) {
            throw as(ClassCastException::new, ex, narrowTo.getName());
        }

        Delegate delegate;
        try {
            // let the stub adopt narrowFrom's identity
            delegate = object._get_delegate();

        } catch (BAD_OPERATION ex) {
            // ignore
            delegate = null;
        }

        stub._set_delegate(delegate);

        return (Remote) stub;
    }

    private static Stub createStub(RMIState state, Class<?> type) {
        if (Remote.class == type) {
            return new RMIRemoteStub();
        }

        if (ClientUtil.isRunningAsClientContainer()) {
            Stub stub = state.getStaticStub(null, type);
            if (stub != null) {
                return stub;
            }
        }

        return createRMIStub(state, type);
    }

    static Stub createRMIStub(RMIState state, Class<?> type) {
        if (!type.isInterface()) {
            throw new RuntimeException("non-interfaces not supported");
        }

        LOGGER.fine(() -> "Creating RMI stub for class " + type.getName());

        Constructor<? extends Stub> cons = getRMIStubClassConstructor(state, type);

        try {
            return cons.newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException ex) {
            throw new RuntimeException("internal problem: cannot instantiate stub", ex);
        }
    }

    static Constructor<? extends Stub> getRMIStubClassConstructor(RMIState state, Class<?> type) {
        LOGGER.fine(() -> "Requesting stub constructor of class " + type.getName());
        return state.stubConstructors.get(type);
    }

    public Remote toStub(Remote value) throws NoSuchObjectException {
        if (value instanceof Stub)
            return value;

        Tie tie = Util.getTie(value);
        if (tie == null) {
            throw new NoSuchObjectException("object not exported");
        }

        RMIServant servant = (RMIServant) tie;

        try {
            POA poa = servant.getRMIState().getPOA();
            org.omg.CORBA.Object ref = poa.servant_to_reference(servant);
            return (Remote) narrow(ref, servant.getJavaClass());
        } catch (ServantNotActive|WrongPolicy ex) {
            throw new RuntimeException("internal error: " + ex.getMessage(), ex);
        }
    }

    public void exportObject(Remote obj) throws RemoteException {
        RMIState state = RMIState.current();

        try {
            state.checkShutDown();
        } catch (BAD_INV_ORDER ex) {
            throw new RemoteException("RMIState is deactivated", ex);
        }

        Tie tie = Util.getTie(obj);

        if (tie != null)
            throw new RemoteException("object already exported");

        RMIServant servant = new RMIServant(state);
        Util.registerTarget(servant, obj);

        LOGGER.finer(() -> "exporting instance of " + obj.getClass().getName()
                + " in " + state.getName());

        try {
            servant._id = state.getPOA().activate_object(servant);
        } catch (ServantAlreadyActive | WrongPolicy ex) {
            throw new RemoteException("internal error: " + ex.getMessage(), ex);
        }
    }

    public void unexportObject(Remote obj) throws NoSuchObjectException {
        javax.rmi.CORBA.Util.unexportObject(obj);
    }

}
