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

import org.omg.CORBA.TCKind;
import org.omg.CORBA.ValueMember;

import java.io.IOException;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static org.omg.CORBA.TCKind.tk_Principal;
import static org.omg.CORBA.TCKind.tk_TypeCode;
import static org.omg.CORBA.TCKind.tk_abstract_interface;
import static org.omg.CORBA.TCKind.tk_alias;
import static org.omg.CORBA.TCKind.tk_any;
import static org.omg.CORBA.TCKind.tk_array;
import static org.omg.CORBA.TCKind.tk_boolean;
import static org.omg.CORBA.TCKind.tk_char;
import static org.omg.CORBA.TCKind.tk_double;
import static org.omg.CORBA.TCKind.tk_float;
import static org.omg.CORBA.TCKind.tk_long;
import static org.omg.CORBA.TCKind.tk_longdouble;
import static org.omg.CORBA.TCKind.tk_longlong;
import static org.omg.CORBA.TCKind.tk_native;
import static org.omg.CORBA.TCKind.tk_objref;
import static org.omg.CORBA.TCKind.tk_octet;
import static org.omg.CORBA.TCKind.tk_sequence;
import static org.omg.CORBA.TCKind.tk_short;
import static org.omg.CORBA.TCKind.tk_string;
import static org.omg.CORBA.TCKind.tk_struct;
import static org.omg.CORBA.TCKind.tk_ulong;
import static org.omg.CORBA.TCKind.tk_ulonglong;
import static org.omg.CORBA.TCKind.tk_ushort;
import static org.omg.CORBA.TCKind.tk_value;
import static org.omg.CORBA.TCKind.tk_value_box;
import static org.omg.CORBA.TCKind.tk_wchar;
import static org.omg.CORBA.TCKind.tk_wstring;
import static org.omg.CORBA_2_4.TCKind.tk_local_interface;

/**
 * A FieldDescriptor that is created from a ValueMember in a FullValueDescription (FVD).
 * This is used when there is no corresponding local Java field for the ValueMember.
 * It can read values from the stream based on the TypeCode, but does not write to any local field.
 */
class ValueMemberFieldDescriptor extends FieldDescriptor {
    private static final Logger logger = Logger.getLogger(ValueMemberFieldDescriptor.class.getName());

    private enum KindValueUtil {
        ;
        private static final BitSet PRIMITIVES = new BitSet(tk_longdouble.value() + 1);
        static {
            PRIMITIVES.set(tk_boolean.value());
            PRIMITIVES.set(tk_char.value());
            PRIMITIVES.set(tk_wchar.value());
            PRIMITIVES.set(tk_octet.value());
            PRIMITIVES.set(tk_short.value());
            PRIMITIVES.set(tk_ushort.value());
            PRIMITIVES.set(tk_long.value());
            PRIMITIVES.set(tk_ulong.value());
            PRIMITIVES.set(tk_longlong.value());
            PRIMITIVES.set(tk_ulonglong.value());
            PRIMITIVES.set(tk_float.value());
            PRIMITIVES.set(tk_double.value());
            PRIMITIVES.set(tk_longdouble.value());
        }

        static boolean isPrimitive(TCKind kind) {
            return PRIMITIVES.get(kind.value());
        }
    }

    @FunctionalInterface
    private interface ReaderOperation {
        Object read(ObjectReader reader) throws IOException;
    }

    private enum TypeReaders {
        ;
        private static final ReaderOperation UNSUPPORTED = reader -> {
            throw new IllegalArgumentException("Unsupported TypeCode kind");
        };

        private static final Map<TCKind, ReaderOperation> READERS;

        static {
            Map<TCKind, ReaderOperation> readers = new HashMap<>();
            readers.put(tk_boolean, ObjectReader::readBoolean);
            readers.put(tk_char, ObjectReader::readChar);
            readers.put(tk_wchar, ObjectReader::readChar);
            readers.put(tk_octet, ObjectReader::readByte);
            readers.put(tk_short, ObjectReader::readShort);
            readers.put(tk_ushort, ObjectReader::readShort);
            readers.put(tk_long, ObjectReader::readInt);
            readers.put(tk_ulong, ObjectReader::readInt);
            readers.put(tk_longlong, ObjectReader::readLong);
            readers.put(tk_ulonglong, ObjectReader::readLong);
            readers.put(tk_float, ObjectReader::readFloat);
            readers.put(tk_double, ObjectReader::readDouble);
            readers.put(tk_longdouble, ObjectReader::readDouble);
            readers.put(tk_string, ObjectReader::readValueObject);
            readers.put(tk_wstring, ObjectReader::readValueObject);
            readers.put(tk_any, ObjectReader::readAny);
            readers.put(tk_abstract_interface, ObjectReader::readAny);
            readers.put(tk_objref, ObjectReader::readCorbaObject);
            readers.put(tk_value, ObjectReader::readValueObject);
            readers.put(tk_value_box, ObjectReader::readValueObject);
            readers.put(tk_sequence, ObjectReader::readValueObject);
            readers.put(tk_array, ObjectReader::readValueObject);
            readers.put(tk_struct, ObjectReader::readValueObject);
            readers.put(tk_alias, ObjectReader::readValueObject);
            readers.put(tk_TypeCode, ObjectReader::readAbstractObject);
            readers.put(tk_Principal, ObjectReader::readAbstractObject);
            readers.put(tk_native, ObjectReader::readAbstractObject);
            readers.put(tk_local_interface, ObjectReader::readAbstractObject);
            READERS = Collections.unmodifiableMap(readers);
        }

        static ReaderOperation get(TCKind kind) {
            ReaderOperation operation = READERS.get(kind);
            return operation == null ? UNSUPPORTED : operation;
        }
    }

    private final ValueMember valueMember;
    private final TCKind kind;

    /**
     * Creates a ValueMemberFieldDescriptor from a ValueMember.
     *
     * @param owner The declaring class (from the local type hierarchy)
     * @param valueMember The ValueMember from the FVD
     * @param repository The type repository
     */
    ValueMemberFieldDescriptor(Class<?> owner, ValueMember valueMember, TypeRepository repository) {
        super(owner, Object.class, valueMember.name, repository);
        this.valueMember = valueMember;
        this.kind = valueMember.type.kind();
    }

    @Override
    public boolean isPrimitive() {
        return isPrimitiveKind(kind);
    }

    private static boolean isPrimitiveKind(TCKind kind) {
        return KindValueUtil.isPrimitive(kind);
    }

    @Override
    void read(ObjectReader reader, Object obj) throws IOException {
        // Read the value from the stream based on TypeCode, but don't assign it anywhere
        // since there's no local field
        readValueByTypeCode(reader);
    }

    @Override
    void write(ObjectWriter writer, Object obj) throws IOException {
        // Cannot write a value that doesn't exist locally
        throw new IOException("Cannot write field '" + java_name +
                "' - no local field equivalent exists for ValueMember from FVD");
    }

    @Override
    void readFieldIntoMap(ObjectReader reader, Map<String,Object> ignored) throws IOException {
        // Read the value from the stream based on TypeCode, but don't add it to the Map
        // as it wasn't defined in serialPersistentFields
        readValueByTypeCode(reader);
    }

    @Override
    void writeFieldFromMap(ObjectWriter writer, Map<String,Object> ignored) throws IOException {
        // Cannot write a value that doesn't exist locally
        throw new IOException("Cannot write field '" + java_name +
                "' from map - no local field equivalent exists for ValueMember from FVD");
    }

    @Override
    void copyState(Object orig, Object copy, CopyState state) {
        // No local field to copy
        logger.finest(() -> "Skipping copyState for virtual field: " + java_name);
    }

    @Override
    void setFieldContents(Object o, Object value) throws IOException {
        throw new IOException("Cannot setFieldContents field '" + java_name +
                "' - no local field equivalent exists for ValueMember from FVD");
    }

    @Override
    Object getFieldContents(Object o) throws IOException {
        throw new IOException("Cannot get field '" + java_name +
                "' - no local field equivalent exists for ValueMember from FVD");
    }

    /**
     * Reads a value from the stream based on the TypeCode kind.
     */
    @SuppressWarnings("UnusedReturnValue")
    private Object readValueByTypeCode(ObjectReader reader) throws IOException {
        try {
            return TypeReaders.get(kind).read(reader);
        } catch (IllegalArgumentException e) {
            logger.warning(() -> "Unsupported TypeCode kind for field '" + java_name +
                    "': " + kind.value() + " - reading as abstract object");
            return reader.readAbstractObject();
        }
    }

    @Override
    ValueMember genValueMember() { return valueMember; }
}
