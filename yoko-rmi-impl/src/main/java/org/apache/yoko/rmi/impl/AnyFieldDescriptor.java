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
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.apache.yoko.rmi.impl;


import org.omg.CORBA.MARSHAL;

import javax.rmi.PortableRemoteObject;
import java.io.IOException;
import java.lang.reflect.Field;
import java.rmi.Remote;
import java.util.Map;
import java.util.logging.Logger;

import static org.apache.yoko.util.Exceptions.as;

class AnyFieldDescriptor extends FieldDescriptor {
    static final Logger logger = Logger.getLogger(AnyFieldDescriptor.class
            .getName());

    boolean narrowValue;

    AnyFieldDescriptor(Class<?> owner, Class<?> type, String name, Field f, TypeRepository repository) {
        super(owner, type, name, f, repository);
        narrowValue = Remote.class.isAssignableFrom(type);
    }

    public void read(ObjectReader reader, Object obj)
            throws IOException {
        try {
            Object val = reader.readAny();
            if (narrowValue && val != null && !type.isInstance(val)) {
                try {
                    val = PortableRemoteObject.narrow(val, this.type);
                } catch (SecurityException ex) {
                    logger.finer(() -> "Narrow failed" + "\n" + ex);
                    throw ex;
                }
            } else if (val != null && !type.isInstance(val)) {
                throw new MARSHAL("value is instance of "
                        + val.getClass().getName() + " -- should be: "
                        + type.getName());
            }

            setFieldContents(obj, val);
        } catch (IllegalStateException ex) {
            throw as(MARSHAL::new, ex, ex.getMessage());
        }
    }

    public void write(ObjectWriter writer, Object obj)
            throws IOException {
        writer.writeAny(getFieldContents(obj));
    }

    void copyState(final Object orig, final Object copy, CopyState state) {
        try {
            setFieldContents(copy, state.copy(getFieldContents(orig)));
        } catch (CopyRecursionException e) {
            state.registerRecursion(new CopyRecursionResolver(orig) {
                public void resolve(Object value) {
                    try {
                        setFieldContents(copy, value);
                    } catch (IOException ex) {
                        throw as(InternalError::new, ex, ex.getMessage());
                    }
                }
            });
        } catch (IOException ex) {
            throw as(InternalError::new, ex, ex.getMessage());
        }
    }

    /**
     * @see FieldDescriptor#readFieldIntoMap(ObjectReader, Map)
     */
    void readFieldIntoMap(ObjectReader reader, Map<String, Object> map) {
        Object value = reader.readAny();
        map.put(java_name, value);
    }

    /**
     * @see FieldDescriptor#writeFieldFromMap(ObjectWriter, Map)
     */
    void writeFieldFromMap(ObjectWriter writer, Map<String, Object> map) throws IOException {
        Object value = map.get(java_name);
        writer.writeAny(value);
    }
}
