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

import org.apache.yoko.orb.CORBA.YokoInputStream;
import org.apache.yoko.orb.CORBA.YokoOutputStream;
import org.apache.yoko.orb.CORBA.TypeCodeImpl;

import org.omg.CORBA.Any;
import org.omg.CORBA.BAD_OPERATION;
import org.omg.CORBA.TypeCode;
import org.omg.CORBA_2_3.portable.InputStream;
import org.omg.CORBA_2_3.portable.OutputStream;

import java.util.function.Supplier;

import static org.apache.yoko.orb.CORBA.any.YokoAnyData.newMismatchBadOp;
import static org.apache.yoko.orb.CORBA.typecode.YokoTypeCode.getPrimitive;
import static org.omg.CORBA.TCKind.tk_any;

/**
 * Immutable typed Any implementation for nested CORBA Any values with lazy extraction.
 * The value is extracted from the input stream only when needed.
 */
public final class AnyAnyData extends LazyAnyData<Any> {
    private static final TypeCode TYPE_CODE = getPrimitive(tk_any);

    // Constructor for of() - value provided, stream created lazily
    private AnyAnyData(Any anyValue, Supplier<OutputStream> outputStreamSupplier) {
        super(TYPE_CODE, anyValue, true, val -> {
            YokoOutputStream os = (YokoOutputStream) outputStreamSupplier.get();
            os.write_any(val);
            return os.create_input_stream();
        });
    }

    // Constructor for from() - stream provided, value extracted lazily
    private AnyAnyData(YokoInputStream stream) {
        super(TYPE_CODE, stream, YokoInputStream::read_any);
    }

    public static AnyAnyData of(Any value, Supplier<OutputStream> outputStreamSupplier) {
        return new AnyAnyData(value, outputStreamSupplier);
    }

    public static AnyAnyData from(InputStream is, TypeCode t) {
        if (!TYPE_CODE.equal(t)) {
            throw newMismatchBadOp();
        }
        if (!(is instanceof YokoInputStream)) {
            throw new BAD_OPERATION("AnyAnyData requires YokoInputStream");
        }
        return new AnyAnyData((YokoInputStream) is);
    }

    @Override
    protected void writeValueFallback(OutputStream os) {
        os.write_any(extract());
    }

    @Override
    public Any extract_any() {
        return extract();
    }
}
