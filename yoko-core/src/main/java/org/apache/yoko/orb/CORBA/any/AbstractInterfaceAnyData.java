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
import static org.omg.CORBA.TCKind.tk_abstract_interface;

/**
 * Immutable typed Any implementation for CORBA abstract interface values with lazy extraction.
 * Abstract interfaces can hold either a CORBA Object reference or a value type.
 * The value is extracted from the input stream only when needed.
 */
public final class AbstractInterfaceAnyData extends LazyAnyData<Object> {

    // Constructor for of() - value provided, stream created lazily
    private AbstractInterfaceAnyData(TypeCode typeCode, Object abstractValue, Supplier<OutputStream> outputStreamSupplier) {
        super(typeCode, abstractValue, true, val -> {
            YokoOutputStream os = (YokoOutputStream) outputStreamSupplier.get();
            os.write_abstract_interface(val);
            return os.create_input_stream();
        });
    }

    // Constructor for from() - stream provided, value extracted lazily
    private AbstractInterfaceAnyData(TypeCode typeCode, YokoInputStream stream) {
        super(typeCode, stream, is -> {
            boolean isObject = is.read_boolean();
            if (isObject) {
                return is.read_Object();
            } else {
                return is.read_value();
            }
        });
    }

    public static AbstractInterfaceAnyData of(Object value, TypeCode typeCode, Supplier<OutputStream> outputStreamSupplier) {
        if (typeCode.kind() != tk_abstract_interface) {
            throw newMismatchBadOp();
        }
        return new AbstractInterfaceAnyData(typeCode, value, outputStreamSupplier);
    }

    public static AbstractInterfaceAnyData from(InputStream is, TypeCode t) {
        if (t.kind() != tk_abstract_interface) {
            throw newMismatchBadOp();
        }
        if (!(is instanceof YokoInputStream)) {
            throw new BAD_OPERATION("AbstractInterfaceAnyData requires YokoInputStream");
        }
        return new AbstractInterfaceAnyData(t, (YokoInputStream) is);
    }

    @Override
    protected void writeValueFallback(OutputStream os) {
        Object val = extract();
        if (val instanceof org.omg.CORBA.Object) {
            os.write_boolean(true);
            os.write_Object((org.omg.CORBA.Object) val);
        } else {
            os.write_boolean(false);
            os.write_value((Serializable) val);
        }
    }

    @Override
    public org.omg.CORBA.Object extract_Object() {
        Object val = extract();
        if (val instanceof org.omg.CORBA.Object) {
            return (org.omg.CORBA.Object) val;
        }
        throw newMismatchBadOp();
    }

    @Override
    public Serializable extract_Value() {
        Object val = extract();
        if (val instanceof org.omg.CORBA.Object) {
            throw newMismatchBadOp();
        }
        return (Serializable) val;
    }
}
