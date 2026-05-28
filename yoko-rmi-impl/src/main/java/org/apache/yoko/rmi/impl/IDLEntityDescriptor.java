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
import org.omg.CORBA.portable.ObjectImpl;
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
    private final boolean isCorba;
    private final Class helperType;

    IDLEntityDescriptor(Class type, TypeRepository repository) {
        super(type, repository);

        isCorba = org.omg.CORBA.Object.class.isAssignableFrom(type);
        try {
            final String helperName = type.getName() + "Helper";
            helperType = Util.loadClass(helperName, null, doPrivileged(getClassLoader(type)));
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException("cannot load IDL Helper class for "
                    + type, ex);
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
    private interface ObjectReader extends Function<InputStream, Object> {}

    private final LazyReference<ObjectReader> objectReader = new LazyReference<>(this::genObjectReader);

    ObjectReader genObjectReader() {
        // Capture the logic at build time
        if (isCorba) {
            return in -> ((org.omg.CORBA_2_3.portable.InputStream) in).read_Object(getType());
        } else {
            final String repositoryID = getRepositoryID();
            return in -> ((org.omg.CORBA_2_3.portable.InputStream) in).read_value(repositoryID);
        }
    }

    private ObjectReader getObjectReader() {
        return objectReader.get();
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



    /** Read an instance of this value from a CDR stream */
    @Override
    public Object read(InputStream in) {
        return getObjectReader().apply(in);
    }

    @Override
    public Serializable readValue(final InputStream in, final Map<Integer, Serializable> offsetMap, final Integer offset) {
        Serializable value = getReader().apply(in);
        offsetMap.put(offset, value);
        return value;
    }

    /** Write an instance of this value to a CDR stream */
    @Override
    public void write(OutputStream out, Object val) {
        org.omg.CORBA_2_3.portable.OutputStream _out = (org.omg.CORBA_2_3.portable.OutputStream) out;

        // there are two ways we need to deal with IDLEntity classes.  Ones that also implement
        // the CORBA Object interface are actual corba objects, and must be handled that way.
        // Other IDLEntity classes are just transmitted by value.
        if (val instanceof ObjectImpl) {
            _out.write_Object((org.omg.CORBA.Object)val);
        } else {
            // we directly call write_value() on the stream here, with the explicitly specified
            // repository ID.  the output stream will handle writing the value tag for us, and eventually
            // will call our writeValue() method to serialize the object.
            _out.write_value((Serializable)val, getRepositoryID());
        }
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
