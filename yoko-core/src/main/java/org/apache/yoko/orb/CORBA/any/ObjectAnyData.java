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

import java.util.function.Supplier;

import static org.apache.yoko.orb.CORBA.any.YokoAnyData.newMismatchBadOp;
import static org.apache.yoko.orb.OB.TypeCodeFactory.createPrimitiveTC;
import static org.omg.CORBA.TCKind.tk_objref;

/**
 * Immutable typed Any implementation for CORBA Object references with lazy extraction.
 * The value is extracted from the input stream only when needed.
 */
public final class ObjectAnyData extends LazyAnyData<org.omg.CORBA.Object> {

    // Constructor for of() - value provided, stream created lazily
    private ObjectAnyData(TypeCode typeCode, org.omg.CORBA.Object objectRef, Supplier<OutputStream> outputStreamSupplier) {
        super(typeCode, objectRef, true, val -> {
            YokoOutputStream os = (YokoOutputStream) outputStreamSupplier.get();
            os.write_Object(val);
            return os.create_input_stream();
        });
    }

    // Constructor for from() - stream provided, value extracted lazily
    private ObjectAnyData(TypeCode typeCode, YokoInputStream stream) {
        super(typeCode, stream, YokoInputStream::read_Object);
    }

    public static ObjectAnyData of(org.omg.CORBA.Object value, Supplier<OutputStream> outputStreamSupplier) {
        return new ObjectAnyData(createPrimitiveTC(tk_objref), value, outputStreamSupplier);
    }

    public static ObjectAnyData of(org.omg.CORBA.Object value, TypeCode typeCode, Supplier<OutputStream> outputStreamSupplier) {
        if (typeCode.kind() != tk_objref) {
            throw newMismatchBadOp();
        }
        return new ObjectAnyData(typeCode, value, outputStreamSupplier);
    }

    public static ObjectAnyData from(InputStream is, TypeCode t) {
        if (t.kind() != tk_objref) {
            throw newMismatchBadOp();
        }
        if (!(is instanceof YokoInputStream)) {
            throw new BAD_OPERATION("ObjectAnyData requires YokoInputStream");
        }
        return new ObjectAnyData(t, (YokoInputStream) is);
    }

    @Override
    protected void writeValueFallback(OutputStream os) {
        os.write_Object(extract());
    }

    @Override
    public org.omg.CORBA.Object extract_Object() {
        return extract();
    }
}
