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
import static org.apache.yoko.orb.OB.TypeCodeFactory.createPrimitiveTC;
import static org.omg.CORBA.TCKind.tk_ulong;

/**
 * Immutable typed Any implementation for CORBA ulong values.
 * Always contains a valid unsigned long value (stored as int).
 */
public final class ULongAnyData implements YokoAnyData<Integer> {
    private static final TypeCode TYPE_CODE = createPrimitiveTC(tk_ulong);

    private final int value;

    private ULongAnyData(int value) {
        this.value = value;
    }

    public static ULongAnyData of(int value) {
        return new ULongAnyData(value);
    }

    public static ULongAnyData from(InputStream is, TypeCode t) {
        if (!TYPE_CODE.equal(t)) {
            throw newMismatchBadOp();
        }
        return new ULongAnyData(is.read_ulong());
    }

    @Override
    public TypeCode type() {
        return TYPE_CODE;
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
