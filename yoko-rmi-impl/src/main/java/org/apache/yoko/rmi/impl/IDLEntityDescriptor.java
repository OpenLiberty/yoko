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
import org.omg.CORBA.TypeCode;
import org.omg.CORBA.portable.InputStream;
import org.omg.CORBA.portable.OutputStream;

import javax.rmi.CORBA.Util;
import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.security.PrivilegedActionException;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static java.security.AccessController.doPrivileged;
import static org.apache.yoko.util.Exceptions.as;
import static org.apache.yoko.util.PrivilegedActions.getClassLoader;
import static org.apache.yoko.util.PrivilegedActions.getDeclaredMethod;

class IDLEntityDescriptor extends ValueDescriptor {
    private final Class<?> helperType;

    private static ReadFn createReader(Class<?> type) {
        boolean isCorbaObj = org.omg.CORBA.Object.class.isAssignableFrom(type);
        if (isCorbaObj) return InputStream::read_Object;
        String repId = String.format("RMI:%s:%016X", type.getName(), 0);
        return in -> ((org.omg.CORBA_2_3.portable.InputStream) in).read_value(repId);
    }

    private static WriteFn createWriter(Class<?> type) {
        boolean isCorbaObj = org.omg.CORBA.Object.class.isAssignableFrom(type);
        if (isCorbaObj) return (out, val) -> out.write_Object((org.omg.CORBA.Object) val);
        String repId = String.format("RMI:%s:%016X", type.getName(), 0);
        return (out, val) -> ((org.omg.CORBA_2_3.portable.OutputStream) out).write_value((Serializable) val, repId);
    }

    IDLEntityDescriptor(Class<?> type, TypeRepository repository) {
        super(type, repository, createReader(type), createWriter(type));

        try {
            final String helperName = type.getName() + "Helper";
            helperType = Util.loadClass(helperName, null, doPrivileged(getClassLoader(type)));
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException("cannot load IDL Helper class for " + type, ex);
        }
    }

    @Override
    final String genIDLName() {
        return "org_omg_boxedIDL_" + super.genIDLName();
    }

    private MethodHandle findMethodHandle(String methodName, Class<?>... parameterTypes) {
        try {
            Method method = doPrivileged(getDeclaredMethod(helperType, methodName, parameterTypes));
            return MethodHandles.lookup().unreflect(method);
        } catch (PrivilegedActionException | IllegalAccessException ex) {
            throw new RuntimeException("Unable to find " + methodName + " method for " + helperType.getName(), ex);
        }
    }

    @FunctionalInterface
    private interface Reader extends Function<InputStream, Serializable> {}

    private final LazyReference<Reader> reader = new LazyReference<>(this::genReader);

    private Reader genReader() {
        MethodHandle readHandle = findMethodHandle("read", InputStream.class);
        return in -> {
            try {
                return (Serializable) readHandle.invoke(in);
            } catch (Error | RuntimeException e) {
                throw e;
            } catch (Throwable t) {
                throw as(MARSHAL::new, t);
            }
        };
    }

    private Reader getReader() {
        return reader.get();
    }

    @FunctionalInterface
    private interface Writer extends BiConsumer<OutputStream, Serializable> {}

    private final LazyReference<Writer> writer = new LazyReference<>(this::genWriter);

    private Writer genWriter() {
        MethodHandle writeHandle = findMethodHandle("write", OutputStream.class, getType());
        return (out, val) -> {
            try {
                writeHandle.invoke(out, val);
            } catch (Error | RuntimeException e) {
                throw e;
            } catch (Throwable t) {
                throw as(MARSHAL::new, t);
            }
        };
    }

    private Writer getWriter() {
        return writer.get();
    }



    @Override
    public Serializable readValue(final InputStream in, final Map<Integer, Serializable> offsetMap, final Integer offset) {
        Serializable value = getReader().apply(in);
        offsetMap.put(offset, value);
        return value;
    }

    @Override
    public void writeValue(OutputStream out, Serializable val) {
        getWriter().accept(out, val);
    }

    @Override
    protected TypeCode genTypeCode() {
        MethodHandle typeHandle = findMethodHandle("type");
        try {
            return (TypeCode) typeHandle.invoke();
        } catch (Error | RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw as(MARSHAL::new, t);
        }
    }
}
