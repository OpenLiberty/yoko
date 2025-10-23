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

import static java.lang.Boolean.FALSE;
import static org.omg.CORBA.TCKind._tk_Principal;
import static org.omg.CORBA.TCKind._tk_TypeCode;
import static org.omg.CORBA.TCKind._tk_abstract_interface;
import static org.omg.CORBA.TCKind._tk_any;
import static org.omg.CORBA.TCKind._tk_boolean;
import static org.omg.CORBA.TCKind._tk_char;
import static org.omg.CORBA.TCKind._tk_double;
import static org.omg.CORBA.TCKind._tk_float;
import static org.omg.CORBA.TCKind._tk_long;
import static org.omg.CORBA.TCKind._tk_longlong;
import static org.omg.CORBA.TCKind._tk_null;
import static org.omg.CORBA.TCKind._tk_objref;
import static org.omg.CORBA.TCKind._tk_octet;
import static org.omg.CORBA.TCKind._tk_short;
import static org.omg.CORBA.TCKind._tk_string;
import static org.omg.CORBA.TCKind._tk_ulong;
import static org.omg.CORBA.TCKind._tk_ulonglong;
import static org.omg.CORBA.TCKind._tk_ushort;
import static org.omg.CORBA.TCKind._tk_value;
import static org.omg.CORBA.TCKind._tk_void;
import static org.omg.CORBA.TCKind._tk_wchar;
import static org.omg.CORBA.TCKind._tk_wstring;
import static org.omg.CORBA.TCKind.tk_any;
import static org.omg.CORBA.TCKind.tk_null;
import static org.omg.CORBA_2_4.TCKind._tk_local_interface;

import org.apache.yoko.orb.CORBA.AnyImpl;
import org.apache.yoko.orb.CORBA.YokoInputStream;
import org.apache.yoko.orb.CORBA.YokoOutputStream;
import org.apache.yoko.orb.CORBA.TypeCodeImpl;
import org.apache.yoko.orb.OB.ORBInstance;
import org.apache.yoko.orb.OB.TypeCodeFactory;
import org.apache.yoko.util.Assert;
import org.omg.CORBA.OBJECT_NOT_EXIST;
import org.omg.CORBA.VM_CUSTOM;
import org.omg.CORBA.TypeCodePackage.BadKind;
import org.omg.DynamicAny.DynAny;
import org.omg.DynamicAny.DynAnyFactory;
import org.omg.DynamicAny.DynAnyPackage.InvalidValue;
import org.omg.DynamicAny.DynAnyPackage.TypeMismatch;

final class DynBasic_impl extends DynAny_impl {
    private AnyImpl any_;

    //
    // This object needs a component when the type is tk_any
    //
    DynAny comp_;

    DynBasic_impl(DynAnyFactory factory,
            ORBInstance orbInstance,
            org.omg.CORBA.TypeCode type) {
        super(factory, orbInstance, type);

        org.omg.CORBA.TypeCode origTC = TypeCodeImpl._OB_getOrigType(type);
        switch (origTC.kind().value()) {
        case _tk_null:
        case _tk_void:
            any_ = new AnyImpl(orbInstance_, type, null);
            break;

        case _tk_short:
        case _tk_ushort:
        case _tk_long:
        case _tk_ulong:
            any_ = new AnyImpl(orbInstance_, type, Integer.valueOf(0));
            break;

        case _tk_longlong:
        case _tk_ulonglong:
            any_ = new AnyImpl(orbInstance_, type, Long.valueOf(0L));
            break;

        case _tk_float:
            any_ = new AnyImpl(orbInstance_, type, Float.valueOf(0F));
            break;

        case _tk_double:
            any_ = new AnyImpl(orbInstance_, type, Double.valueOf(0D));
            break;

        case _tk_boolean:
            any_ = new AnyImpl(orbInstance_, type, FALSE);
            break;

        case _tk_char:
        case _tk_wchar:
            any_ = new AnyImpl(orbInstance_, type, Character.valueOf((char) 0));
            break;

        case _tk_octet:
            any_ = new AnyImpl(orbInstance_, type, Byte.valueOf((byte) 0));
            break;

        case _tk_any:
            any_ = new AnyImpl(orbInstance_, type, new AnyImpl(orbInstance_));
            break;

        case _tk_TypeCode: {
            org.omg.CORBA.TypeCode nullTC = TypeCodeFactory
                    .createPrimitiveTC(tk_null);
            any_ = new AnyImpl(orbInstance_, type, nullTC);
            break;
        }

        case _tk_objref:
        case _tk_local_interface:
            any_ = new AnyImpl(orbInstance_, type, null);
            break;

        case _tk_string:
        case _tk_wstring:
            any_ = new AnyImpl(orbInstance_, type, new String(""));
            break;

        case _tk_abstract_interface:
            any_ = new AnyImpl(orbInstance_, type, null);
            break;

        case _tk_value:
            //
            // Only custom valuetypes are supported by DynBasic_impl
            //
            try {
                Assert
                        .ensure(origTC.type_modifier() == VM_CUSTOM.value);
            } catch (BadKind ex) {
                throw Assert.fail(ex);
            }
            any_ = new AnyImpl(orbInstance_, type, null);
            break;

        case _tk_Principal:
        default:
            throw Assert.fail("Unsupported type code");
        }
    }

    // ------------------------------------------------------------------
    // Private and protected member implementations
    // ------------------------------------------------------------------

    protected void notifyParent() {
        //
        // We have intercepted a change notification. If our type is tk_any,
        // then we release our component. It will be created again during
        // get_dyn_any().
        //
        if (origType_.kind() == tk_any) {
            if (comp_ != null)
                comp_ = null;
        }

        super.notifyParent();
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

        any_ = (AnyImpl) dyn_any.to_any();

        notifyParent();
    }

    public synchronized void from_any(org.omg.CORBA.Any value)
            throws TypeMismatch,
            InvalidValue {
        if (destroyed_)
            throw new org.omg.CORBA.OBJECT_NOT_EXIST();

        //
        // Convert value to an ORBacus Any - the JDK implementation
        // of TypeCode.equivalent() raises NO_IMPLEMENT
        //
        try {
            any_ = new AnyImpl(value);
        } catch (NullPointerException ex) {
            throw (InvalidValue)new 
                InvalidValue().initCause(ex);
        }

        org.omg.CORBA.TypeCode tc = any_._OB_type();
        org.omg.CORBA.TypeCode origTC = TypeCodeImpl._OB_getOrigType(tc);

        if (!tc.equivalent(type_))
            throw new TypeMismatch();

        //
        // Check for an invalid value
        //
        if (any_.value() == null) {
            switch (origTC.kind().value()) {
            case _tk_null:
            case _tk_void:
            case _tk_TypeCode:
            case _tk_objref:
            case _tk_value:
            case _tk_abstract_interface:
            case _tk_local_interface:
                //
                // Some types can legally have a null value
                //
                break;

            default:
                throw new InvalidValue();
            }
        }

        notifyParent();
    }

    public synchronized org.omg.CORBA.Any to_any() {
        if (destroyed_)
            throw new OBJECT_NOT_EXIST();

        return new AnyImpl(any_);
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

        //
        // Don't use Any.equal() here because it calls equal() on
        // the TypeCodes, and the semantics of DynAny.equal()
        // only require TypeCodes to be equivalent.
        //

        java.lang.Object v1 = any_.value();
        java.lang.Object v2 = ((DynBasic_impl) dyn_any).any_.value();

        switch (origType_.kind().value()) {
        case _tk_null:
        case _tk_void:
            return true;

        case _tk_short:
        case _tk_ushort:
        case _tk_long:
        case _tk_ulong:
        case _tk_longlong:
        case _tk_ulonglong:
        case _tk_float:
        case _tk_double:
        case _tk_boolean:
        case _tk_char:
        case _tk_wchar:
        case _tk_octet:
        case _tk_string:
        case _tk_wstring:
            return v1.equals(v2);

        case _tk_any:
            return ((org.omg.CORBA.Any) v1).equal((org.omg.CORBA.Any) v2);

        case _tk_TypeCode:
            if (v1 == null && v2 == null)
                return true;
            else if (v1 == null || v2 == null)
                return false;
            else
                return ((org.omg.CORBA.TypeCode) v1)
                        .equal((org.omg.CORBA.TypeCode) v2);

        case _tk_objref:
        case _tk_local_interface:
            if (v1 == null && v2 == null)
                return true;
            else if (v1 == null || v2 == null)
                return false;
            else
                return ((org.omg.CORBA.Object) v1)
                        ._is_equivalent((org.omg.CORBA.Object) v2);

        case _tk_value:
            if (v1 == v2)
                return true;
            else if (v1 == null || v2 == null)
                return false;
            else {
                //
                // Currently, it's not possible for a custom valuetype
                // to be represented as an InputStream in an any
                //
                throw Assert.fail("Unable to compare value types");
            }

        case _tk_abstract_interface:
            if (v1 == v2)
                return true;
            else if (v1 == null || v2 == null)
                return false;
            else if (v1 instanceof org.omg.CORBA.Object
                    && v2 instanceof org.omg.CORBA.Object) {
                return ((org.omg.CORBA.Object) v1)
                        ._is_equivalent((org.omg.CORBA.Object) v2);
            } else {
                //
                // Currently, it's not possible for an abstract interface
                // to be represented as an InputStream in an any
                //
                throw Assert.fail("Unable to compare abstract_interface types");
            }

        case _tk_Principal:
        default:
            throw Assert.fail("Unsupported type code");
        }
    }

    public synchronized DynAny copy() {
        if (destroyed_)
            throw new OBJECT_NOT_EXIST();

        DynBasic_impl result = new DynBasic_impl(factory_, orbInstance_, type_);
        result.any_ = new AnyImpl(any_);
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

    // ------------------------------------------------------------------
    // Internal member implementations
    // ------------------------------------------------------------------

    synchronized void _OB_marshal(YokoOutputStream out) {
        any_.write_value(out);
    }

    synchronized void _OB_marshal(YokoOutputStream out,
                                  DynValueWriter dynValueWriter) {
        _OB_marshal(out);
    }

    synchronized void _OB_unmarshal(YokoInputStream in) {
        any_.read_value(in, type_);

        if (comp_ != null)
            comp_ = null;

        notifyParent();
    }

    synchronized AnyImpl _OB_currentAny() {
        if (destroyed_)
            throw new OBJECT_NOT_EXIST();

        return any_;
    }

    synchronized AnyImpl _OB_currentAnyValue() {
        return any_;
    }

    synchronized DynAny _OB_getDynAny() {
        if (origType_.kind() != tk_any)
            return null;

        if (comp_ == null)
            comp_ = create((AnyImpl) any_.value(), true);

        return comp_;
    }

    synchronized boolean _OB_insertDynAny(DynAny p) {
        //
        // Do nothing if caller is passing our component
        //
        if (p == comp_)
            return true;

        try {
            checkValue(any_, tk_any);

            DynAny_impl impl = (DynAny_impl) p;
            AnyImpl implAny = impl._OB_currentAny();
            checkValue(implAny, tk_any);
            AnyImpl any = (AnyImpl) implAny.value();

            any_.replace(any_.type(), new AnyImpl(any));

            if (comp_ != null)
                comp_ = null;

            return true;
        } catch (TypeMismatch ex) {
        } catch (InvalidValue ex) {
        }

        return false;
    }
}
