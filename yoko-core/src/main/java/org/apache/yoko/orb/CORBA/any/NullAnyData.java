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
import static org.omg.CORBA.TCKind.tk_null;
import static org.omg.CORBA.TCKind.tk_void;

/**
 * Immutable typed Any implementation for CORBA null and void marker types.
 * These types have no value and always return null.
 */
public enum NullAnyData implements YokoAnyData<Object> {
    NULL(getPrimitive(tk_null)),
    VOID(getPrimitive(tk_void));

    private final TypeCode typeCode;

    NullAnyData(TypeCode typeCode) {
        this.typeCode = typeCode;
    }

    public static NullAnyData of(TypeCode typeCode) {
        if (typeCode.kind() == tk_null) {
            return NULL;
        } else if (typeCode.kind() == tk_void) {
            return VOID;
        }
        throw newMismatchBadOp();
    }

    public static NullAnyData from(InputStream ignored, TypeCode t) {
        return of(t);
    }

    @Override
    public TypeCode type() {
        return typeCode;
    }

    @Override
    public Object extract() {
        return null;
    }

    @Override
    public void write(OutputStream os) {
        // Nothing to write for null/void types
    }
}
