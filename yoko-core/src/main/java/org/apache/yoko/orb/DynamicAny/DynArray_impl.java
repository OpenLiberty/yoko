/*
 * Copyright 2025 IBM Corporation and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an \"AS IS\" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.apache.yoko.orb.DynamicAny;

import org.apache.yoko.orb.CORBA.TypeCodeImpl;
import org.apache.yoko.orb.OB.ORBInstance;
import org.omg.CORBA.Any;
import org.omg.DynamicAny.DynAny;
import org.omg.DynamicAny.DynAnyFactory;
import org.omg.DynamicAny.DynArray;
import org.omg.DynamicAny.DynAnyPackage.InvalidValue;
import org.omg.DynamicAny.DynAnyPackage.TypeMismatch;

final class DynArray_impl extends DynSeqBase_impl implements
        DynArray {
    DynArray_impl(DynAnyFactory factory,
            ORBInstance orbInstance,
            org.omg.CORBA.TypeCode type) {
        super(factory, orbInstance, type);
    }

    DynArray_impl(DynAnyFactory factory,
            ORBInstance orbInstance,
            org.omg.CORBA.TypeCode type, DynValueReader dynValueReader) {
        super(factory, orbInstance, type, dynValueReader);
    }

    // ------------------------------------------------------------------
    // Standard IDL to Java Mapping
    // ------------------------------------------------------------------

    public synchronized Any[] get_elements() {
        return getElements();
    }

    public synchronized void set_elements(Any[] value)
            throws TypeMismatch,
            InvalidValue {
        for (int i = 0; i < value.length; i++) {
            org.omg.CORBA.TypeCode origTC = TypeCodeImpl._OB_getOrigType(value[i]
                    .type());
            if (origTC.kind() != contentKind_)
                throw new TypeMismatch();
        }

        if (value.length != length_)
            throw new InvalidValue();

        for (int i = 0; i < value.length; i++)
            setValue(i, value[i]);

        index_ = 0;

        notifyParent();
    }

    public synchronized DynAny[] get_elements_as_dyn_any() {
        return getElementsAsDynAny();
    }

    public synchronized void set_elements_as_dyn_any(
            DynAny[] value)
            throws TypeMismatch,
            InvalidValue {
        for (int i = 0; i < value.length; i++) {
            org.omg.CORBA.TypeCode origTC = TypeCodeImpl._OB_getOrigType(value[i]
                    .type());
            if (origTC.kind() != contentKind_)
                throw new TypeMismatch();
        }

        if (value.length != length_)
            throw new InvalidValue();

        for (int i = 0; i < value.length; i++)
            setValue(i, value[i]);

        index_ = 0;

        notifyParent();
    }
}
