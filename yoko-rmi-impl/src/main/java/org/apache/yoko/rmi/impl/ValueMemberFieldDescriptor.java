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
import org.omg.CORBA.TypeCode;
import org.omg.CORBA.ValueMember;

import java.io.IOException;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Map;
import java.util.logging.Logger;

import static org.omg.CORBA.TCKind._tk_abstract_interface;
import static org.omg.CORBA.TCKind._tk_alias;
import static org.omg.CORBA.TCKind._tk_any;
import static org.omg.CORBA.TCKind._tk_array;
import static org.omg.CORBA.TCKind._tk_boolean;
import static org.omg.CORBA.TCKind._tk_char;
import static org.omg.CORBA.TCKind._tk_double;
import static org.omg.CORBA.TCKind._tk_float;
import static org.omg.CORBA.TCKind._tk_long;
import static org.omg.CORBA.TCKind._tk_longdouble;
import static org.omg.CORBA.TCKind._tk_longlong;
import static org.omg.CORBA.TCKind._tk_native;
import static org.omg.CORBA.TCKind._tk_objref;
import static org.omg.CORBA.TCKind._tk_octet;
import static org.omg.CORBA.TCKind._tk_Principal;
import static org.omg.CORBA.TCKind._tk_sequence;
import static org.omg.CORBA.TCKind._tk_short;
import static org.omg.CORBA.TCKind._tk_string;
import static org.omg.CORBA.TCKind._tk_struct;
import static org.omg.CORBA.TCKind._tk_TypeCode;
import static org.omg.CORBA.TCKind._tk_ulong;
import static org.omg.CORBA.TCKind._tk_ulonglong;
import static org.omg.CORBA.TCKind._tk_ushort;
import static org.omg.CORBA.TCKind._tk_value;
import static org.omg.CORBA.TCKind._tk_value_box;
import static org.omg.CORBA.TCKind._tk_wchar;
import static org.omg.CORBA.TCKind._tk_wstring;
import static org.omg.CORBA_2_4.TCKind._tk_local_interface;

/**
 * A FieldDescriptor that is created from a ValueMember in a FullValueDescription (FVD).
 * This is used when there is no corresponding local Java field for the ValueMember.
 * It can read values from the stream based on the TypeCode, but does not write to any local field.
 */
class ValueMemberFieldDescriptor extends FieldDescriptor {
    private static final Logger logger = Logger.getLogger(ValueMemberFieldDescriptor.class.getName());

    private enum KindValueUtil {
        ;
        private static final BitSet PRIMITIVES = new BitSet(_tk_longdouble + 1);
        static {
            PRIMITIVES.set(_tk_boolean);
            PRIMITIVES.set(_tk_char);
            PRIMITIVES.set(_tk_wchar);
            PRIMITIVES.set(_tk_octet);
            PRIMITIVES.set(_tk_short);
            PRIMITIVES.set(_tk_ushort);
            PRIMITIVES.set(_tk_long);
            PRIMITIVES.set(_tk_ulong);
            PRIMITIVES.set(_tk_longlong);
            PRIMITIVES.set(_tk_ulonglong);
            PRIMITIVES.set(_tk_float);
            PRIMITIVES.set(_tk_double);
            PRIMITIVES.set(_tk_longdouble);
        }

        static boolean isPrimitive(int kind) {
            return PRIMITIVES.get(kind);
        }
    }

    @FunctionalInterface
    private interface ReaderOperation {
        Object read(ObjectReader reader) throws IOException;
    }

    private enum TypeReaders {
        ;
        private static final ReaderOperation[] READERS = new ReaderOperation[_tk_local_interface + 1];

        private static final ReaderOperation UNSUPPORTED = reader -> {
            throw new IllegalArgumentException("Unsupported TypeCode kind");
        };

        static {
            // Fill array with default error-throwing operation
            Arrays.fill(READERS, UNSUPPORTED);

            // Set supported operations
            READERS[_tk_boolean] = ObjectReader::readBoolean;
            READERS[_tk_char] = ObjectReader::readChar;
            READERS[_tk_wchar] = ObjectReader::readChar;
            READERS[_tk_octet] = ObjectReader::readByte;
            READERS[_tk_short] = ObjectReader::readShort;
            READERS[_tk_ushort] = ObjectReader::readShort;
            READERS[_tk_long] = ObjectReader::readInt;
            READERS[_tk_ulong] = ObjectReader::readInt;
            READERS[_tk_longlong] = ObjectReader::readLong;
            READERS[_tk_ulonglong] = ObjectReader::readLong;
            READERS[_tk_float] = ObjectReader::readFloat;
            READERS[_tk_double] = ObjectReader::readDouble;
            READERS[_tk_longdouble] = ObjectReader::readDouble;
            READERS[_tk_string] = ObjectReader::readValueObject;
            READERS[_tk_wstring] = ObjectReader::readValueObject;
            READERS[_tk_any] = ObjectReader::readAny;
            READERS[_tk_abstract_interface] = ObjectReader::readAny;
            READERS[_tk_objref] = reader -> reader.readCorbaObject(null);
            READERS[_tk_value] = ObjectReader::readValueObject;
            READERS[_tk_value_box] = ObjectReader::readValueObject;
            READERS[_tk_sequence] = ObjectReader::readValueObject;
            READERS[_tk_array] = ObjectReader::readValueObject;
            READERS[_tk_struct] = ObjectReader::readValueObject;
            READERS[_tk_alias] = ObjectReader::readValueObject;
            READERS[_tk_TypeCode] = ObjectReader::readAbstractObject;
            READERS[_tk_Principal] = ObjectReader::readAbstractObject;
            READERS[_tk_native] = ObjectReader::readAbstractObject;
            READERS[_tk_local_interface] = ObjectReader::readAbstractObject;
        }

        static ReaderOperation get(int kind) {
            return READERS[kind];
        }
    }

    private final ValueMember valueMember;
    private final TypeCode typeCode;
    private final TCKind kind;

    /**
     * Creates a ValueMemberFieldDescriptor from a ValueMember.
     *
     * @param owner The declaring class (from the local type hierarchy)
     * @param valueMember The ValueMember from the FVD
     * @param repository The type repository
     */
    ValueMemberFieldDescriptor(Class<?> owner, ValueMember valueMember, TypeRepository repository) {
        super(owner, Object.class, valueMember.name, null, repository);
        this.valueMember = valueMember;
        this.typeCode = valueMember.type;
        this.kind = typeCode.kind();
    }

    @Override
    public boolean isPrimitive() {
        return isPrimitiveKind(kind);
    }

    private static boolean isPrimitiveKind(TCKind kind) {
        return KindValueUtil.isPrimitive(kind.value());
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
    void readFieldIntoMap(ObjectReader reader, Map map) throws IOException {
        Object value = readValueByTypeCode(reader);
        map.put(java_name, value);
    }

    @Override
    void writeFieldFromMap(ObjectWriter writer, Map map) throws IOException {
        // Cannot write a value that doesn't exist locally
        throw new IOException("Cannot write field '" + java_name +
                "' from map - no local field equivalent exists for ValueMember from FVD");
    }

    @Override
    void copyState(Object orig, Object copy, CopyState state) {
        // No local field to copy
        logger.finest(() -> "Skipping copyState for virtual field: " + java_name);
    }

    /**
     * Reads a value from the stream based on the TypeCode kind.
     */
    private Object readValueByTypeCode(ObjectReader reader) throws IOException {
        try {
            return TypeReaders.get(kind.value()).read(reader);
        } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
            logger.warning(() -> "Unsupported TypeCode kind for field '" + java_name +
                    "': " + kind.value() + " - reading as abstract object");
            return reader.readAbstractObject();
        }
    }

    @Override
    ValueMember getValueMember(TypeRepository rep) {
        return valueMember;
    }
}
