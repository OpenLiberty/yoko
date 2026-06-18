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


import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;

import static org.apache.yoko.util.Exceptions.as;

class CorbaObjectFieldDescriptor extends FieldDescriptor {

    protected CorbaObjectFieldDescriptor(Class<?> owner, Class<?> type, String name, Field f, TypeRepository repository) {
        super(owner, type, name, f, repository);
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

    void read(ObjectReader reader, Object obj) throws IOException {
        Object value = reader.readCorbaObject(type);
        setFieldContents(obj, value);
    }

    void readFieldIntoMap(ObjectReader reader, Map<String, Object> map) {
        Object value = reader.readCorbaObject(type);
        map.put(java_name, value);

    }

    void write(ObjectWriter writer, Object obj) throws IOException {
        writer.writeCorbaObject(getFieldContents(obj));
    }

    void writeFieldFromMap(ObjectWriter writer, Map<String, Object> map) throws IOException {
        org.omg.CORBA.Object value = (org.omg.CORBA.Object) map.get(java_name);
        writer.writeCorbaObject(value);
    }
}
