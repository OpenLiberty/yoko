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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static java.security.AccessController.doPrivileged;
import static org.apache.yoko.util.Exceptions.as;
import static org.apache.yoko.util.PrivilegedActions.getClassLoader;

class IDLEntityDescriptor extends ValueDescriptor {
    private final Class helperType;

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

    IDLEntityDescriptor(Class type, TypeRepository repository) {
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

    private Method findMethod(String methodName, Class<?>... parameterTypes) {
        try {
            return doPrivileged((PrivilegedExceptionAction<Method>) () ->
                helperType.getDeclaredMethod(methodName, parameterTypes)
            );
        } catch (PrivilegedActionException ex) {
            throw new RuntimeException("Unable to find " + methodName + " method for " + helperType.getName(), ex.getCause());
        }
    }

    @FunctionalInterface
    private interface Reader extends Function<InputStream, Serializable> {}

    private final LazyReference<Reader> reader = new LazyReference<>(this::genReader);

    private Reader genReader() {
        Method readMethod = findMethod("read", InputStream.class);
        return in -> {
            try {
                return (Serializable) readMethod.invoke(null, in);
            } catch (InvocationTargetException | IllegalAccessException ex) {
                throw as(MARSHAL::new, ex);
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
        Method writeMethod = findMethod("write", OutputStream.class, getType());
        return (out, val) -> {
            try {
                writeMethod.invoke(null, out, val);
            } catch (InvocationTargetException | IllegalAccessException ex) {
                throw as(MARSHAL::new, ex);
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
        Method typeMethod = findMethod("type");
        try {
            return (TypeCode) typeMethod.invoke(null);
        } catch (InvocationTargetException | IllegalAccessException ex) {
            throw as(MARSHAL::new, ex);
        }
    }
}
