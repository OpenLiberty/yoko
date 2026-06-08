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
package org.apache.yoko.orb.CORBA.any;

import org.omg.CORBA.TypeCode;
import org.omg.CORBA_2_3.portable.InputStream;
import org.omg.CORBA_2_3.portable.OutputStream;

import static org.apache.yoko.orb.CORBA.any.YokoAnyData.newMismatchBadOp;
import static org.omg.CORBA.TCKind._tk_enum;

/**
 * Immutable typed Any implementation for CORBA enum values.
 * Enums are represented as unsigned longs but preserve their enum TypeCode.
 */
public final class EnumAnyData implements YokoAnyData<Integer> {
    private final TypeCode typeCode;
    private final int value;

    private EnumAnyData(TypeCode typeCode, int value) {
        this.typeCode = typeCode;
        this.value = value;
    }

    public static EnumAnyData of(int value, TypeCode typeCode) {
        return new EnumAnyData(typeCode, value);
    }

    public static EnumAnyData from(InputStream is, TypeCode t) {
        if (t.kind().value() != _tk_enum) {
            throw newMismatchBadOp();
        }
        return new EnumAnyData(t, is.read_ulong());
    }

    @Override
    public TypeCode type() {
        return typeCode;
    }

    @Override
    public Integer extract() {
        return value;
    }


    @Override
    public void write(OutputStream os) {
        os.write_ulong(value);
    }

    @Override
    public int extract_ulong() {
        return extract();
    }
}
