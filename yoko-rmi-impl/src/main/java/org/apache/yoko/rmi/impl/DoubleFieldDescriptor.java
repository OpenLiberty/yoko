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
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.Map;

import static org.apache.yoko.util.Exceptions.as;

class DoubleFieldDescriptor extends FieldDescriptor {
    DoubleFieldDescriptor(Class<?> owner, Class<?> type, String name, Field f, TypeRepository repository) {
        super(owner, type, name, f, repository);
    }

    public void read(ObjectReader reader, Object obj) throws IOException {
        double value = reader.readDouble();
        setFieldContents(obj, value);
    }

    public void write(ObjectWriter writer, Object obj) throws IOException {
        writer.writeDouble((Double) getFieldContents(obj));
    }

    void copyState(Object orig, Object copy, CopyState state) {
        try {
            setFieldContents(copy, getFieldContents(orig));
        } catch (IOException ex) {
            throw as(InternalError::new, ex, ex.getMessage());
        }
    }

    void print(PrintWriter pw, Map<Object, Integer> recurse, Object val) {
        try {
            pw.print(java_name);
            pw.print("=");
            pw.print(getFieldContents(val));
        } catch (IllegalStateException | IOException ex) {
            pw.print("<non-local>");
        }
    }

    /**
     * @see FieldDescriptor#readFieldIntoMap(ObjectReader, Map)
     */
    void readFieldIntoMap(ObjectReader reader, Map<String, Object> map) throws IOException {
        Double value = reader.readDouble();
        map.put(java_name, value);
    }

    /**
     * @see FieldDescriptor#writeFieldFromMap(ObjectWriter, Map)
     */
    void writeFieldFromMap(ObjectWriter writer, Map<String, Object> map) throws IOException {
        Double value = (Double) map.get(java_name);
        if (value == null) {
            writer.writeDouble(0.0D);
        } else {
            writer.writeDouble(value);
        }
    }

}
