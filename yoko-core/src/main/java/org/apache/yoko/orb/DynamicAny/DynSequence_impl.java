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
import org.omg.DynamicAny.DynAny;
import org.omg.DynamicAny.DynAnyFactory;
import org.omg.DynamicAny.DynSequence;
import org.omg.DynamicAny.DynAnyPackage.InvalidValue;
import org.omg.DynamicAny.DynAnyPackage.TypeMismatch;

final class DynSequence_impl extends DynSeqBase_impl implements
        DynSequence {
    DynSequence_impl(DynAnyFactory factory,
            ORBInstance orbInstance,
            org.omg.CORBA.TypeCode type) {
        super(factory, orbInstance, type);
    }

    DynSequence_impl(DynAnyFactory factory,
            ORBInstance orbInstance,
            org.omg.CORBA.TypeCode type, DynValueReader dynValueReader) {
        super(factory, orbInstance, type, dynValueReader);
    }

    // ------------------------------------------------------------------
    // Standard IDL to Java Mapping
    // ------------------------------------------------------------------

    public synchronized int get_length() {
        return length_;
    }

    public synchronized void set_length(int len)
            throws InvalidValue {
        //
        // If bounded sequence, ensure new length is not greater
        // than the bounds
        //
        if (max_ > 0 && len > max_)
            throw new InvalidValue();

        resize(len, true);

        notifyParent();
    }

    public synchronized org.omg.CORBA.Any[] get_elements() {
        return getElements();
    }

    public synchronized void set_elements(org.omg.CORBA.Any[] value)
            throws TypeMismatch,
            InvalidValue {
        for (int i = 0; i < value.length; i++) {
            org.omg.CORBA.TypeCode tc = value[i].type();
            org.omg.CORBA.TypeCode origTC = TypeCodeImpl._OB_getOrigType(tc);
            if (origTC.kind() != contentKind_)
                throw new TypeMismatch();
        }

        resize(value.length, false);

        for (int i = 0; i < value.length; i++)
            setValue(i, value[i]);

        if (value.length == 0)
            index_ = -1;
        else
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
            org.omg.CORBA.TypeCode tc = value[i].type();
            org.omg.CORBA.TypeCode origTC = TypeCodeImpl._OB_getOrigType(tc);
            if (origTC.kind() != contentKind_)
                throw new TypeMismatch();
        }

        resize(value.length, false);

        for (int i = 0; i < value.length; i++)
            setValue(i, value[i]);

        if (value.length == 0)
            index_ = -1;
        else
            index_ = 0;

        notifyParent();
    }
}
