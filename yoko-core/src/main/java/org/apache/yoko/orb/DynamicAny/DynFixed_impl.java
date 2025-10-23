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

import static java.math.BigDecimal.ROUND_DOWN;

import java.math.BigDecimal;

import org.apache.yoko.orb.CORBA.AnyImpl;
import org.apache.yoko.orb.CORBA.YokoInputStream;
import org.apache.yoko.orb.CORBA.YokoOutputStream;
import org.apache.yoko.orb.OB.ORBInstance;
import org.apache.yoko.util.Assert;
import org.omg.CORBA.BAD_OPERATION;
import org.omg.CORBA.OBJECT_NOT_EXIST;
import org.omg.CORBA.TypeCode;
import org.omg.CORBA.TypeCodePackage.BadKind;
import org.omg.DynamicAny.DynAny;
import org.omg.DynamicAny.DynAnyFactory;
import org.omg.DynamicAny.DynFixed;
import org.omg.DynamicAny.DynAnyPackage.InvalidValue;
import org.omg.DynamicAny.DynAnyPackage.TypeMismatch;

final class DynFixed_impl extends DynAny_impl implements
        DynFixed {
    private BigDecimal value_;

    DynFixed_impl(DynAnyFactory factory,
            ORBInstance orbInstance,
            TypeCode type) {
        super(factory, orbInstance, type);
        value_ = new BigDecimal(0);
    }

    // ------------------------------------------------------------------
    // Standard IDL to Java Mapping
    // ------------------------------------------------------------------

    public synchronized void assign(DynAny dyn_any)
            throws TypeMismatch {
        if (destroyed_)
            throw new OBJECT_NOT_EXIST();

        if (this == dyn_any)
            return;

        if (!dyn_any.type().equivalent(type_))
            throw new TypeMismatch();

        DynFixed_impl impl = (DynFixed_impl) dyn_any;
        value_ = impl.value_;

        notifyParent();
    }

    public synchronized void from_any(org.omg.CORBA.Any value)
            throws TypeMismatch,
            InvalidValue {
        if (destroyed_)
            throw new OBJECT_NOT_EXIST();

        //
        // Convert value to an ORBacus Any - the JDK implementation
        // of TypeCode.equivalent() raises NO_IMPLEMENT
        //
        AnyImpl val = null;
        try {
            val = (AnyImpl) value;
        } catch (ClassCastException ex) {
            try {
                val = new AnyImpl(value);
            } catch (NullPointerException e) {
                throw (InvalidValue)new 
                    InvalidValue().initCause(e);
            }
        }

        if (!val._OB_type().equivalent(type_))
            throw new TypeMismatch();

        try {
            java.math.BigDecimal f = val.extract_fixed();

            if (f == null || f.scale() > origType_.fixed_scale())
                throw new InvalidValue();
            value_ = f;
        } catch (BadKind ex) {
            throw Assert.fail(ex);
        } catch (BAD_OPERATION ex) {
            throw (InvalidValue)new 
                InvalidValue().initCause(ex);
        }

        notifyParent();
    }

    public synchronized org.omg.CORBA.Any to_any() {
        if (destroyed_)
            throw new OBJECT_NOT_EXIST();

        return new AnyImpl(orbInstance_, type_, value_);
    }

    public synchronized org.omg.CORBA.Any to_any(DynValueWriter dynValueWriter) {
        return to_any();
    }

    public synchronized boolean equal(DynAny dyn_any) {
        if (destroyed_)
            throw new OBJECT_NOT_EXIST();

        if (this == dyn_any)
            return true;

        if (!dyn_any.type().equivalent(type_))
            return false;

        DynFixed_impl impl = (DynFixed_impl) dyn_any;
        return value_.equals(impl.value_);
    }

    public synchronized DynAny copy() {
        if (destroyed_)
            throw new OBJECT_NOT_EXIST();

        DynFixed_impl result = new DynFixed_impl(factory_, orbInstance_, type_);
        result.value_ = value_;
        return result;
    }

    public boolean seek(int index) {
        return false;
    }

    public void rewind() {
        // do nothing
    }

    public boolean next() {
        return false;
    }

    public int component_count() {
        return 0;
    }

    public DynAny current_component()
            throws TypeMismatch {
        if (destroyed_)
            throw new OBJECT_NOT_EXIST();

        throw new TypeMismatch();
    }

    public synchronized String get_value() {
        return value_.toString();
    }

    public synchronized boolean set_value(String val)
            throws TypeMismatch,
            InvalidValue {
        String s = val.trim().toLowerCase();
        if (s.endsWith("d"))
            s = s.substring(0, s.length() - 1);
        if (s.length() == 0)
            throw new InvalidValue();

        java.math.BigDecimal f = null;

        try {
            f = new java.math.BigDecimal(s);
        } catch (NumberFormatException ex) {
            throw (TypeMismatch)new 
                TypeMismatch().initCause(ex);
        }

        int origDigits = 0, origScale = 0;
        try {
            origDigits = origType_.fixed_digits();
            origScale = origType_.fixed_scale();
        } catch (BadKind ex) {
            throw Assert.fail(ex);
        }

        int fDigits = 0, fScale = f.scale();
        if (fScale > 0)
            fDigits = f.movePointRight(fScale).abs().toString().length();
        else
            fDigits = f.abs().toString().length();

        //
        // Raise InvalidValue if this DynFixed is incapable of
        // representing the value (even with a loss of precision)
        //
        if ((fDigits - fScale) > (origDigits - origScale))
            throw new InvalidValue();

        //
        // Return true if there was no loss of precision, otherwise
        // truncate and return false
        //
        boolean result = true;
        if (fScale > origScale) {
            value_ = f.setScale(origScale, ROUND_DOWN);
            result = false;
        } else
            value_ = f.setScale(origScale);

        notifyParent();

        return result;
    }

    // ------------------------------------------------------------------
    // Internal member implementations
    // ------------------------------------------------------------------

    synchronized void _OB_marshal(YokoOutputStream out) {
        try {
            out.write_fixed(value_.movePointRight(origType_.fixed_scale()));
        } catch (BadKind ex) {
            throw Assert.fail(ex);
        }
    }

    synchronized void _OB_marshal(YokoOutputStream out,
                                  DynValueWriter dynValueWriter) {
        _OB_marshal(out);
    }

    synchronized void _OB_unmarshal(YokoInputStream in) {
        try {
            value_ = in.read_fixed().movePointLeft(origType_.fixed_scale());
        } catch (BadKind ex) {
            throw Assert.fail(ex);
        }

        notifyParent();
    }

    AnyImpl _OB_currentAny() {
        if (destroyed_)
            throw new OBJECT_NOT_EXIST();

        return null;
    }

    AnyImpl _OB_currentAnyValue() {
        return null;
    }
}
