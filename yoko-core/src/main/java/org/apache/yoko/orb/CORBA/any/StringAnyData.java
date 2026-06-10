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
import static org.apache.yoko.orb.CORBA.typecode.YokoTypeCode.getPrimitive;
import static org.omg.CORBA.TCKind.tk_string;

/**
 * Immutable typed Any implementation for CORBA string values.
 * Always contains a valid String value.
 */
public final class StringAnyData implements YokoAnyData<String> {
    private static final TypeCode TYPE_CODE = getPrimitive(tk_string);

    private final String value;

    private StringAnyData(String value) {
        this.value = value;
    }

    public static StringAnyData of(String value) {
        return new StringAnyData(value);
    }

    public static StringAnyData from(InputStream is, TypeCode t) {
        if (!TYPE_CODE.equal(t)) {
            throw newMismatchBadOp();
        }
        return new StringAnyData(is.read_string());
    }

    @Override
    public TypeCode type() {
        return TYPE_CODE;
    }

    @Override
    public String extract() {
        return value;
    }


    @Override
    public void write(OutputStream os) {
        os.write_string(value);
    }

    @Override
    public String extract_string() {
        return extract();
    }
}
