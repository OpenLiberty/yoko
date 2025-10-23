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

import org.apache.yoko.orb.CORBA.AnyImpl;
import org.apache.yoko.orb.CORBA.YokoInputStream;
import org.apache.yoko.orb.CORBA.YokoOutputStream;
import org.apache.yoko.orb.OB.ORBInstance;
import org.apache.yoko.util.Assert;
import org.omg.CORBA.OBJECT_NOT_EXIST;
import org.omg.CORBA.TypeCode;
import org.omg.CORBA.TypeCodePackage.BadKind;
import org.omg.CORBA.TypeCodePackage.Bounds;
import org.omg.DynamicAny.DynAny;
import org.omg.DynamicAny.DynAnyFactory;
import org.omg.DynamicAny.DynEnum;
import org.omg.DynamicAny.DynAnyPackage.InvalidValue;
import org.omg.DynamicAny.DynAnyPackage.TypeMismatch;

final class DynEnum_impl extends DynAny_impl implements
        DynEnum {
    private int value_;

    DynEnum_impl(DynAnyFactory factory,
            ORBInstance orbInstance,
            TypeCode type) {
        super(factory, orbInstance, type);
        value_ = 0;
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

        DynEnum dyn_enum = (DynEnum) dyn_any;
        value_ = dyn_enum.get_as_ulong();

        notifyParent();
    }

    public synchronized void from_any(org.omg.CORBA.Any value)
            throws TypeMismatch,
            InvalidValue {
        if (destroyed_)
            throw new OBJECT_NOT_EXIST();

        AnyImpl val = null;
        try {
            val = (AnyImpl) value;
        } catch (ClassCastException ex) {
            val = new AnyImpl(value);
        }

        if (val.value() == null)
            throw new InvalidValue();

        if (!val._OB_type().equivalent(type_))
            throw new TypeMismatch();

        value_ = ((Integer) val.value()).intValue();

        notifyParent();
    }

    public synchronized org.omg.CORBA.Any to_any() {
        if (destroyed_)
            throw new OBJECT_NOT_EXIST();

        return new AnyImpl(orbInstance_, type_, Integer.valueOf(value_));
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

        DynEnum dyn_enum = (DynEnum) dyn_any;
        return (value_ == dyn_enum.get_as_ulong());
    }

    public synchronized DynAny copy() {
        if (destroyed_)
            throw new OBJECT_NOT_EXIST();

        DynEnum_impl result = new DynEnum_impl(factory_, orbInstance_, type_);
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

    public synchronized String get_as_string() {
        String result = null;

        try {
            result = origType_.member_name(value_);
        } catch (BadKind ex) {
            throw Assert.fail(ex);
        } catch (Bounds ex) {
            throw Assert.fail(ex);
        }

        return result;
    }

    public synchronized void set_as_string(String value)
            throws InvalidValue {
        try {
            int count = origType_.member_count();
            for (int i = 0; i < count; i++) {
                if (value.equals(origType_.member_name(i))) {
                    value_ = i;
                    notifyParent();
                    return;
                }
            }

            throw new InvalidValue();
        } catch (BadKind ex) {
            throw Assert.fail(ex);
        } catch (Bounds ex) {
            throw Assert.fail(ex);
        }
    }

    public synchronized int get_as_ulong() {
        return value_;
    }

    public synchronized void set_as_ulong(int value)
            throws InvalidValue {
        try {
            if (value < 0 || value >= origType_.member_count())
                throw new InvalidValue();

            value_ = value;

            notifyParent();
        } catch (BadKind ex) {
            throw Assert.fail(ex);
        }
    }

    // ------------------------------------------------------------------
    // Internal member implementations
    // ------------------------------------------------------------------

    synchronized void _OB_marshal(YokoOutputStream out) {
        out.write_ulong(value_);
    }

    synchronized void _OB_marshal(YokoOutputStream out,
                                  DynValueWriter dynValueWriter) {
        _OB_marshal(out);
    }

    synchronized void _OB_unmarshal(YokoInputStream in) {
        value_ = in.read_ulong();

        notifyParent();
    }

    synchronized AnyImpl _OB_currentAny() {
        if (destroyed_)
            throw new OBJECT_NOT_EXIST();

        return null;
    }

    synchronized AnyImpl _OB_currentAnyValue() {
        return null;
    }
}
