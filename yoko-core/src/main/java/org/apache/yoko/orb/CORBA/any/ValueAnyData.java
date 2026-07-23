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

import org.omg.CORBA.BAD_OPERATION;
import org.omg.CORBA.TypeCode;
import org.omg.CORBA_2_3.portable.InputStream;
import org.omg.CORBA_2_3.portable.OutputStream;

import java.io.Serializable;
import java.util.function.Supplier;

import static org.apache.yoko.orb.CORBA.any.YokoAnyData.newMismatchBadOp;
import static org.omg.CORBA.TCKind.tk_value;
import static org.omg.CORBA.TCKind.tk_value_box;

/**
 * Immutable typed Any implementation for CORBA value types with lazy extraction.
 * The value is extracted from the input stream only when needed.
 */
public final class ValueAnyData extends LazyAnyData<Serializable> {

    // Constructor for of() - value provided, stream created lazily
    private ValueAnyData(TypeCode typeCode, Serializable valueObject, Supplier<OutputStream> outputStreamSupplier) {
        super(typeCode, valueObject, true, val -> {
            YokoOutputStream os = (YokoOutputStream) outputStreamSupplier.get();
            os.write_value(val);
            return os.create_input_stream();
        });
    }

    // Constructor for from() - stream provided, value extracted lazily
    private ValueAnyData(TypeCode typeCode, YokoInputStream stream) {
        super(typeCode, stream, YokoInputStream::read_value);
    }

    public static ValueAnyData of(Serializable value, TypeCode typeCode, Supplier<OutputStream> outputStreamSupplier) {
        if (typeCode.kind() != tk_value && typeCode.kind() != tk_value_box) {
            throw newMismatchBadOp();
        }
        return new ValueAnyData(typeCode, value, outputStreamSupplier);
    }

    public static ValueAnyData from(InputStream is, TypeCode t) {
        if (t.kind() != tk_value && t.kind() != tk_value_box) {
            throw newMismatchBadOp();
        }
        if (!(is instanceof YokoInputStream)) {
            throw new BAD_OPERATION("ValueAnyData requires YokoInputStream");
        }
        return new ValueAnyData(t, (YokoInputStream) is);
    }

    @Override
    protected void writeValueFallback(OutputStream os) {
        os.write_value(extract());
    }

    @Override
    public Serializable extract_Value() {
        return extract();
    }
}
