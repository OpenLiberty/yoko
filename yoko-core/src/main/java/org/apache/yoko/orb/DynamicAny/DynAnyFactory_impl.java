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

import static org.omg.CORBA.TCKind._tk_Principal;
import static org.omg.CORBA.TCKind._tk_TypeCode;
import static org.omg.CORBA.TCKind._tk_abstract_interface;
import static org.omg.CORBA.TCKind._tk_alias;
import static org.omg.CORBA.TCKind._tk_any;
import static org.omg.CORBA.TCKind._tk_array;
import static org.omg.CORBA.TCKind._tk_boolean;
import static org.omg.CORBA.TCKind._tk_char;
import static org.omg.CORBA.TCKind._tk_double;
import static org.omg.CORBA.TCKind._tk_enum;
import static org.omg.CORBA.TCKind._tk_except;
import static org.omg.CORBA.TCKind._tk_fixed;
import static org.omg.CORBA.TCKind._tk_float;
import static org.omg.CORBA.TCKind._tk_long;
import static org.omg.CORBA.TCKind._tk_longdouble;
import static org.omg.CORBA.TCKind._tk_longlong;
import static org.omg.CORBA.TCKind._tk_native;
import static org.omg.CORBA.TCKind._tk_null;
import static org.omg.CORBA.TCKind._tk_objref;
import static org.omg.CORBA.TCKind._tk_octet;
import static org.omg.CORBA.TCKind._tk_sequence;
import static org.omg.CORBA.TCKind._tk_short;
import static org.omg.CORBA.TCKind._tk_string;
import static org.omg.CORBA.TCKind._tk_struct;
import static org.omg.CORBA.TCKind._tk_ulong;
import static org.omg.CORBA.TCKind._tk_ulonglong;
import static org.omg.CORBA.TCKind._tk_union;
import static org.omg.CORBA.TCKind._tk_ushort;
import static org.omg.CORBA.TCKind._tk_value;
import static org.omg.CORBA.TCKind._tk_value_box;
import static org.omg.CORBA.TCKind._tk_void;
import static org.omg.CORBA.TCKind._tk_wchar;
import static org.omg.CORBA.TCKind._tk_wstring;
import static org.omg.CORBA_2_4.TCKind._tk_local_interface;

import org.apache.yoko.orb.CORBA.AnyImpl;
import org.apache.yoko.orb.CORBA.InputStream;
import org.apache.yoko.orb.CORBA.TypeCodeImpl;
import org.apache.yoko.orb.OB.ORBInstance;
import org.apache.yoko.util.Assert;
import org.omg.CORBA.Any;
import org.omg.CORBA.AnySeqHelper;
import org.omg.CORBA.LocalObject;
import org.omg.CORBA.VM_CUSTOM;
import org.omg.CORBA.TypeCodePackage.BadKind;
import org.omg.DynamicAny.DynAny;
import org.omg.DynamicAny.DynAnyFactory;
import org.omg.DynamicAny.MustTruncate;
import org.omg.DynamicAny.DynAnyFactoryPackage.InconsistentTypeCode;
import org.omg.DynamicAny.DynAnyPackage.InvalidValue;
import org.omg.DynamicAny.DynAnyPackage.TypeMismatch;

final public class DynAnyFactory_impl extends LocalObject
        implements DynAnyFactory {
    private ORBInstance orbInstance_;

    public DynAnyFactory_impl(ORBInstance orbInstance) {
        orbInstance_ = orbInstance;
    }

    // ------------------------------------------------------------------
    // Standard IDL to Java Mapping
    // ------------------------------------------------------------------

    public DynAny create_dyn_any(Any value)
            throws InconsistentTypeCode {
        DynValueReader dynValueReader = new DynValueReader(orbInstance_, this,
                true);

        try {
            DynAny p = prepare_dyn_any_from_type_code(value
                    .type(), dynValueReader);
            p.from_any(value);
            return p;
        } catch (TypeMismatch ex) {
        } catch (InvalidValue ex) {
        }

        throw new InconsistentTypeCode();
    }

    public DynAny create_dyn_any_without_truncation(
            Any value)
            throws InconsistentTypeCode,
            MustTruncate {
        DynValueReader dynValueReader = new DynValueReader(orbInstance_, this,
                false);

        try {
            DynAny p = prepare_dyn_any_from_type_code(value
                    .type(), dynValueReader);

            p.from_any(value);

            if (dynValueReader.mustTruncate)
                throw new MustTruncate();

            return p;
        } catch (TypeMismatch ex) {
        } catch (InvalidValue ex) {
        }

        throw new InconsistentTypeCode();
    }

    public DynAny prepare_dyn_any_from_type_code(
            org.omg.CORBA.TypeCode tc,
            DynValueReader dvr)
            throws InconsistentTypeCode {
        DynAny result = null;

        TypeCodeImpl type = null;
        try {
            type = (TypeCodeImpl) tc;
        } catch (ClassCastException ex) {
            type = TypeCodeImpl._OB_convertForeignTypeCode(tc);
        }

        org.omg.CORBA.TypeCode origTC = TypeCodeImpl
                ._OB_getOrigType(type);
        switch (origTC.kind().value()) {
        case _tk_struct:
        case _tk_except:
            result = new DynStruct_impl(this, orbInstance_, type, dvr);
            break;

        case _tk_union:
            result = new DynUnion_impl(this, orbInstance_, type, dvr);
            break;

        case _tk_sequence:
            result = new DynSequence_impl(this, orbInstance_, type, dvr);
            break;

        case _tk_array:
            result = new DynArray_impl(this, orbInstance_, type, dvr);
            break;

        case _tk_value:
            try {
                if (origTC.type_modifier() == VM_CUSTOM.value)
                    result = create_dyn_any_from_type_code(tc);
                else
                    result = new DynValue_impl(this, orbInstance_, type, dvr);
            } catch (BadKind ex) {
            }
            break;

        default:
            result = create_dyn_any_from_type_code(tc);
        }

        return result;
    }

    public DynAny create_dyn_any_from_type_code(
            org.omg.CORBA.TypeCode tc)
            throws InconsistentTypeCode {
        DynAny result = null;

        TypeCodeImpl type = null;
        try {
            type = (TypeCodeImpl) tc;
        } catch (ClassCastException ex) {
            type = TypeCodeImpl._OB_convertForeignTypeCode(tc);
        }

        org.omg.CORBA.TypeCode origTC = TypeCodeImpl
                ._OB_getOrigType(type);
        switch (origTC.kind().value()) {
        case _tk_null:
        case _tk_void:
        case _tk_short:
        case _tk_long:
        case _tk_ushort:
        case _tk_ulong:
        case _tk_float:
        case _tk_double:
        case _tk_boolean:
        case _tk_char:
        case _tk_octet:
        case _tk_any:
        case _tk_TypeCode:
        case _tk_objref:
        case _tk_string:
        case _tk_longlong:
        case _tk_ulonglong:
        case _tk_wchar:
        case _tk_wstring:
        case _tk_abstract_interface:
        case _tk_local_interface:
            result = new DynBasic_impl(this, orbInstance_, type);
            break;

        case _tk_fixed:
            result = new DynFixed_impl(this, orbInstance_, type);
            break;

        case _tk_enum:
            result = new DynEnum_impl(this, orbInstance_, type);
            break;

        case _tk_struct:
        case _tk_except:
            result = new DynStruct_impl(this, orbInstance_, type);
            break;

        case _tk_union:
            result = new DynUnion_impl(this, orbInstance_, type);
            break;

        case _tk_sequence:
            result = new DynSequence_impl(this, orbInstance_, type);
            break;

        case _tk_array:
            result = new DynArray_impl(this, orbInstance_, type);
            break;

        case _tk_value:
            try {
                if (origTC.type_modifier() == VM_CUSTOM.value)
                    result = new DynBasic_impl(this, orbInstance_, type);
                else
                    result = new DynValue_impl(this, orbInstance_, type);
            } catch (BadKind ex) {
            }
            break;

        case _tk_value_box:
            result = new DynValueBox_impl(this, orbInstance_, type);
            break;

        case _tk_Principal:
        case _tk_native:
        case _tk_longdouble:
            throw new InconsistentTypeCode();

        case _tk_alias:
        default:
            throw Assert.fail("Unsupported type code");
        }

        return result;
    }

    public DynAny[] create_multiple_dyn_anys(
            org.omg.CORBA.Any[] values, boolean allow_truncate)
            throws InconsistentTypeCode,
            MustTruncate {
        //
        // Put the sequence of Anys and marshal it. This ensures that
        // ValueType equality that "spans" dynAnys will be mapped by
        // indirections when marshalled.
        //
        Any aSeq = orbInstance_.getORB().create_any();
        AnySeqHelper.insert(aSeq, values);

        AnyImpl valSeq;
        valSeq = (AnyImpl) aSeq;

        InputStream in = (InputStream) valSeq.create_input_stream();

        // NOTE: the input stream I obtain does not contain
        // indirections that "span" the original members of the sequence.
        // (that is an issue with the implementation of Anys). Thus
        // ValueType that span the original Any instance are not
        // properly mapped.

        //
        // Create a sequence of Dynamic Anys
        //
        DynAny result[] = new DynAny[values.length];

        DynValueReader dynValueReader = new DynValueReader(orbInstance_, this,
                allow_truncate);

        for (int i = 0; i < values.length; i++) {
            org.omg.CORBA.TypeCode type = ((AnyImpl) values[i])
                    ._OB_type();

            result[i] = prepare_dyn_any_from_type_code(type, dynValueReader);
        }

        //
        // Populate the DynAnys by unmarshalling the sequence of Anys.
        // Start by skipping the sequence lenght
        //
        in.read_ulong();

        for (int i = 0; i < values.length; i++) {
            in.read_TypeCode();
            DynAny_impl impl = (DynAny_impl) result[i];
            impl._OB_unmarshal(in);
        }

        return result;
    }

    public Any[] create_multiple_anys(
            DynAny[] values) {
        // TODO: DynValue equalities that "span" members of
        // the sequence of DynAnys are not maintained

        Any[] result = new Any[values.length];

        for (int i = 0; i < values.length; i++) {
            DynAny_impl impl = (DynAny_impl) values[i];
            result[i] = impl.to_any();
        }

        return result;
    }

}
