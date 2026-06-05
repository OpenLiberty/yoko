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

import java.io.IOException;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.util.Map;

import static org.apache.yoko.util.Arrays.emptyArray;

class EnumSubclassDescriptor extends UncustomizableValueDescriptor {

    EnumSubclassDescriptor(Class<?> type, TypeRepository repository) {
        super(type, repository);
    }

    static Class<?> getEnumType(Class<?> type) {
        if (!Enum.class.isAssignableFrom(type)) throw new IllegalArgumentException(type.getName() + " is not an Enum");
        while (!type.isEnum()) type = type.getSuperclass();
        return type;
    }

    @Override
    final long genSerialVersionUid() {
        return 0L;
    }

    @Override
    protected final boolean isEnum() { return true; }

    @Override
    ValueReader genValueReader() {
        return genValueReader(this.getSuperDescriptor(), this.getType());
    }

    static ValueReader genValueReader(ValueDescriptor superDesc, Class<?> type) {
        return (reader, ignored) -> readValue(superDesc, type, reader);
    }

    private static Serializable readValue(ValueDescriptor superDesc, Class<?> type, ObjectReader reader) {
        try {
            Map<String, Object> fields = superDesc.readFields(reader);
            @SuppressWarnings({"unchecked", "rawtypes"})
            Serializable result = Enum.valueOf((Class)type, (String) fields.get("name"));
            return result;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    protected final void writeValue(ObjectWriter writer, Serializable val) throws IOException {
        // Don't write out any fields in the Enum subclass
        getSuperDescriptor().writeValue(writer, val);
    }

    @Override
    public final boolean isChunked() {
        // Always do chunking for subclasses of Enum - like it's custom marshalled
        return true;
    }

    @Override
    ObjectStreamField[] findSerialPersistentFields() {
        // Enum subclasses have no fields of their own - they delegate to the parent enum
        return emptyArray(ObjectStreamField.class);
    }
}