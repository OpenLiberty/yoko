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
import org.omg.CORBA.SystemException;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Map;

import static org.apache.yoko.util.Exceptions.as;
import static org.apache.yoko.util.yasf.Yasf.NON_SERIALIZABLE_FIELD_IS_ABSTRACT_VALUE;

class ValueFieldDescriptor extends FieldDescriptor {
    ValueFieldDescriptor(Class<?> owner, Class<?> type, String name, Field f, TypeRepository repository) {
        super(owner, type, name, f, repository);
    }

    public void read(ObjectReader reader, Object obj) throws IOException {
        try {
            Object value;
            if (NON_SERIALIZABLE_FIELD_IS_ABSTRACT_VALUE.isSupported()
                    || type.isInterface()
                    || Serializable.class.isAssignableFrom(type)) {
                value = reader.readValueObject(getType());
            } else {
                // older versions of Yoko treat non-serializable classes as abstract objects
                value = reader.readAbstractObject();
            }
            setFieldContents(obj, value);
        } catch (IllegalStateException ex) {
            throw as(MARSHAL::new, ex, ex.getMessage());
        }
    }

    public void write(ObjectWriter writer, Object obj) throws IOException {
        if (NON_SERIALIZABLE_FIELD_IS_ABSTRACT_VALUE.isSupported()
                || type.isInterface()
                || Serializable.class.isAssignableFrom(type)) {
            try {
                writer.writeValueObject(getFieldContents(obj));
            } catch (SystemException e) {
                throw e;
            } catch (Exception e) {
                throw as(MARSHAL::new, e, "Object of class " + obj.getClass().getName() + " is not a valuetype");
            }
        } else {
            // older versions of Yoko treat non-serializable classes as abstract objects
            writer.writeObject(getFieldContents(obj));
        }
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
        Serializable value = (Serializable) reader
                .readValueObject();
        map.put(java_name, value);
    }

    /**
     * @see FieldDescriptor#writeFieldFromMap(ObjectWriter, Map)
     */
    void writeFieldFromMap(ObjectWriter writer, Map<String, Object> map) throws IOException {
        Serializable value = (Serializable) map
                .get(java_name);
        writer.writeValueObject(value);
    }
}
