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

    private static final BitSet PRIMITIVE_KINDS = new BitSet();
    static {
        PRIMITIVE_KINDS.set(_tk_boolean);
        PRIMITIVE_KINDS.set(_tk_char);
        PRIMITIVE_KINDS.set(_tk_wchar);
        PRIMITIVE_KINDS.set(_tk_octet);
        PRIMITIVE_KINDS.set(_tk_short);
        PRIMITIVE_KINDS.set(_tk_ushort);
        PRIMITIVE_KINDS.set(_tk_long);
        PRIMITIVE_KINDS.set(_tk_ulong);
        PRIMITIVE_KINDS.set(_tk_longlong);
        PRIMITIVE_KINDS.set(_tk_ulonglong);
        PRIMITIVE_KINDS.set(_tk_float);
        PRIMITIVE_KINDS.set(_tk_double);
        PRIMITIVE_KINDS.set(_tk_longdouble);
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
        return PRIMITIVE_KINDS.get(kind.value());
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
        switch (kind.value()) {
            case _tk_boolean:
                return reader.readBoolean();
            case _tk_char:
            case _tk_wchar:
                return reader.readChar();
            case _tk_octet:
                return reader.readByte();
            case _tk_short:
            case _tk_ushort:
                return reader.readShort();
            case _tk_long:
            case _tk_ulong:
                return reader.readInt();
            case _tk_longlong:
            case _tk_ulonglong:
                return reader.readLong();
            case _tk_float:
                return reader.readFloat();
            case _tk_double:
            case _tk_longdouble:
                return reader.readDouble();
            case _tk_string:
            case _tk_wstring:
                return reader.readValueObject();
            case _tk_any:
            case _tk_abstract_interface:
                return reader.readAny();
            case _tk_objref:
                return reader.readCorbaObject(null);
            case _tk_value:
            case _tk_value_box:
            case _tk_sequence:
            case _tk_array:
            case _tk_struct:
            case _tk_alias:
                return reader.readValueObject();
            case _tk_TypeCode:
            case _tk_Principal:
            case _tk_native:
            case _tk_local_interface:
                return reader.readAbstractObject();
            default:
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
