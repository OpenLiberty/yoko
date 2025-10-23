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
package org.apache.yoko.orb.OB;

import static java.lang.Character.isLetter;
import static java.lang.Character.isLetterOrDigit;
import static org.apache.yoko.orb.CORBA.TypeCodeImpl._OB_convertForeignTypeCode;
import static org.apache.yoko.orb.CORBA.TypeCodeImpl._OB_embedRecTC;
import static org.apache.yoko.orb.CORBA.TypeCodeImpl._OB_getOrigType;
import static org.apache.yoko.util.MinorCodes.MinorDuplicateLabel;
import static org.apache.yoko.util.MinorCodes.MinorIncompatibleLabelType;
import static org.apache.yoko.util.MinorCodes.MinorInvalidDiscriminatorType;
import static org.apache.yoko.util.MinorCodes.MinorInvalidId;
import static org.apache.yoko.util.MinorCodes.MinorInvalidMemberName;
import static org.apache.yoko.util.MinorCodes.MinorInvalidMemberType;
import static org.apache.yoko.util.MinorCodes.MinorInvalidName;
import static org.apache.yoko.util.MinorCodes.describeBadParam;
import static org.apache.yoko.util.MinorCodes.describeBadTypecode;
import static org.omg.CORBA.CompletionStatus.COMPLETED_NO;
import static org.omg.CORBA.TCKind._tk_Principal;
import static org.omg.CORBA.TCKind._tk_TypeCode;
import static org.omg.CORBA.TCKind._tk_any;
import static org.omg.CORBA.TCKind._tk_boolean;
import static org.omg.CORBA.TCKind._tk_char;
import static org.omg.CORBA.TCKind._tk_double;
import static org.omg.CORBA.TCKind._tk_enum;
import static org.omg.CORBA.TCKind._tk_fixed;
import static org.omg.CORBA.TCKind._tk_float;
import static org.omg.CORBA.TCKind._tk_long;
import static org.omg.CORBA.TCKind._tk_longdouble;
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
import static org.omg.CORBA.TCKind.tk_abstract_interface;
import static org.omg.CORBA.TCKind.tk_alias;
import static org.omg.CORBA.TCKind.tk_array;
import static org.omg.CORBA.TCKind.tk_enum;
import static org.omg.CORBA.TCKind.tk_except;
import static org.omg.CORBA.TCKind.tk_fixed;
import static org.omg.CORBA.TCKind.tk_native;
import static org.omg.CORBA.TCKind.tk_null;
import static org.omg.CORBA.TCKind.tk_objref;
import static org.omg.CORBA.TCKind.tk_octet;
import static org.omg.CORBA.TCKind.tk_sequence;
import static org.omg.CORBA.TCKind.tk_string;
import static org.omg.CORBA.TCKind.tk_struct;
import static org.omg.CORBA.TCKind.tk_union;
import static org.omg.CORBA.TCKind.tk_value;
import static org.omg.CORBA.TCKind.tk_value_box;
import static org.omg.CORBA.TCKind.tk_void;
import static org.omg.CORBA.TCKind.tk_wstring;
import static org.omg.CORBA_2_4.TCKind.tk_local_interface;

import org.apache.yoko.orb.CORBA.AnyImpl;
import org.apache.yoko.orb.CORBA.TypeCodeImpl;
import org.apache.yoko.util.Assert;
import org.omg.CORBA.Any;
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.BAD_TYPECODE;
import org.omg.CORBA.NO_IMPLEMENT;
import org.omg.CORBA.StructMember;
import org.omg.CORBA.TCKind;
import org.omg.CORBA.UnionMember;
import org.omg.CORBA.VM_ABSTRACT;
import org.omg.CORBA.ValueMember;

public final class TypeCodeFactory {
    //
    // Cache the primitive TypeCodes
    //
    private static org.omg.CORBA.TypeCode[] primitives_;
    static {
        primitives_ = new org.omg.CORBA.TypeCode[34];
    }

    private TypeCodeFactory() {
    }

    // ----------------------------------------------------------------------
    // TypeCodeFactory private and protected member implementation
    // ----------------------------------------------------------------------

    private static boolean checkId(String id) {
        //
        // Check for a valid repository ID (<format>:<string>)
        //
        if (id.length() > 0) {
            int colon = id.indexOf(':');
            if (colon == -1 || colon == 0 || colon == id.length() - 1)
                return false;
        }

        return true;
    }

    private static final boolean CHECK_IDL_NAMES = false;

    private static boolean checkName(String name) {
        if (!CHECK_IDL_NAMES) return true;
        //
        // Check for a valid IDL name
        //
        if (name.length() > 0) {
            if (!isLetter(name.charAt(0)))
                return false;
            for (int i = 1; i < name.length(); i++) {
                char ch = name.charAt(i);
                if (!isLetterOrDigit(ch) && ch != '_')
                    return false;
            }
        }

        return true;
    }

    private static boolean checkType(org.omg.CORBA.TypeCode type) {
        //
        // Check for an illegal content or member type
        //
        try {
            org.omg.CORBA.TypeCode origType = _OB_getOrigType(type);
            TCKind kind = origType.kind();
            if (kind == tk_null
                    || kind == tk_void
                    || kind == tk_except)
                return false;
        } catch (BAD_TYPECODE ex) {
            // TypeCode may be recursive
        }

        return true;
    }

    private static boolean compareLabels(TCKind kind,
            Any a1, Any a2) {
        switch (kind.value()) {
        case _tk_short:
            return a1.extract_short() == a2.extract_short();

        case _tk_ushort:
            return a1.extract_ushort() == a2.extract_ushort();

        case _tk_long:
            return a1.extract_long() == a2.extract_long();

        case _tk_ulong:
            return a1.extract_ulong() == a2.extract_ulong();

        case _tk_longlong:
            return a1.extract_longlong() == a2.extract_longlong();

        case _tk_ulonglong:
            return a1.extract_ulonglong() == a2.extract_ulonglong();

        case _tk_char:
            return a1.extract_char() == a2.extract_char();

        case _tk_boolean:
            return a1.extract_boolean() == a2.extract_boolean();

        case _tk_wchar:
            return a1.extract_wchar() == a2.extract_wchar();

        case _tk_enum:
            return a1.create_input_stream().read_ulong() == a2
                    .create_input_stream().read_ulong();

        default:
            throw Assert.fail("Unsupported typecode for compare");
        }
    }

    // ----------------------------------------------------------------------
    // TypeCodeFactory public member implementation
    // ----------------------------------------------------------------------

    public static org.omg.CORBA.TypeCode createPrimitiveTC(
            TCKind kind) {
        Assert.ensure(kind.value() < primitives_.length);

        if (primitives_[kind.value()] != null)
            return primitives_[kind.value()];

        org.omg.CORBA.TypeCode tc;

        switch (kind.value()) {
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
        case _tk_Principal:
        case _tk_string:
        case _tk_longlong:
        case _tk_ulonglong:
        case _tk_longdouble:
        case _tk_wchar:
        case _tk_wstring:
        case _tk_fixed: {
            TypeCodeImpl p = new TypeCodeImpl();
            p.kind_ = kind;
            p.length_ = 0; // For strings
            tc = p;
            break;
        }

        case _tk_objref:
            tc = createInterfaceTC("IDL:omg.org/CORBA/Object:1.0", "Object");
            break;

        case _tk_value:
            tc = createValueTC("IDL:omg.org/CORBA/ValueBase:1.0", "ValueBase", VM_ABSTRACT.value, null, new ValueMember[0]);
            break;

        default:
            throw Assert.fail();
        }

        primitives_[kind.value()] = tc;

        return tc;
    }

    public static org.omg.CORBA.TypeCode createStructTC(String id, String name,
            StructMember[] members) {
        Assert.ensure(id != null && name != null);

        if (!checkId(id))
            throw new BAD_PARAM(describeBadParam(MinorInvalidId)
                    + ": " + id, MinorInvalidId,
                    COMPLETED_NO);
        if (!checkName(name))
            throw new BAD_PARAM(describeBadParam(MinorInvalidName)
                    + ": " + name, MinorInvalidName,
                    COMPLETED_NO);

        for (int i = 0; i < members.length; i++) {
            if (!checkName(members[i].name))
                throw new BAD_PARAM(describeBadParam(MinorInvalidMemberName)
                        + ": " + members[i].name,
                        MinorInvalidMemberName,
                        COMPLETED_NO);
            if (!checkType(members[i].type))
                throw new BAD_TYPECODE(describeBadTypecode(MinorInvalidMemberType)
                        + ": " + members[i].name,
                        MinorInvalidMemberType,
                        COMPLETED_NO);
            for (int j = i + 1; j < members.length; j++)
                if (members[i].name.length() > 0
                        && members[i].name.equalsIgnoreCase(members[j].name)) {
                    throw new BAD_PARAM(
                            describeBadParam(MinorInvalidMemberName)
                                    + ": " + members[i].name,
                            MinorInvalidMemberName,
                            COMPLETED_NO);
                }
        }

        TypeCodeImpl tc = new TypeCodeImpl();

        tc.kind_ = tk_struct;
        tc.id_ = id;
        tc.name_ = name;

        tc.memberNames_ = new String[members.length];
        tc.memberTypes_ = new TypeCodeImpl[members.length];

        for (int i = 0; i < members.length; i++) {
            tc.memberNames_[i] = members[i].name;
            try {
                tc.memberTypes_[i] = (TypeCodeImpl) members[i].type;
            } catch (ClassCastException ex) {
                tc.memberTypes_[i] = _OB_convertForeignTypeCode(members[i].type);
            }
        }

        _OB_embedRecTC(tc);
        return tc;
    }

    public static org.omg.CORBA.TypeCode createUnionTC(String id, String name,
            org.omg.CORBA.TypeCode discriminator_type,
            UnionMember[] members) {
        Assert.ensure(id != null && name != null);

        if (!checkId(id))
            throw new BAD_PARAM(describeBadParam(MinorInvalidId)
                    + ": " + id, MinorInvalidId,
                    COMPLETED_NO);
        if (!checkName(name))
            throw new BAD_PARAM(describeBadParam(MinorInvalidName)
                    + ": " + name, MinorInvalidName,
                    COMPLETED_NO);

        org.omg.CORBA.TypeCode origDisc = _OB_getOrigType(discriminator_type);
        switch (origDisc.kind().value()) {
        case _tk_short:
        case _tk_ushort:
        case _tk_long:
        case _tk_ulong:
        case _tk_longlong:
        case _tk_ulonglong:
        case _tk_char:
        case _tk_boolean:
        case _tk_wchar:
        case _tk_enum:
            break;

        default:
            throw new BAD_PARAM(
                    describeBadParam(MinorInvalidDiscriminatorType),
                    MinorInvalidDiscriminatorType,
                    COMPLETED_NO);
        }

        for (int i = 0; i < members.length; i++) {
            if (!checkName(members[i].name))
                throw new BAD_PARAM(describeBadParam(MinorInvalidMemberName)
                        + ": " + members[i].name,
                        MinorInvalidMemberName,
                        COMPLETED_NO);
            if (!checkType(members[i].type))
                throw new BAD_TYPECODE(describeBadTypecode(MinorInvalidMemberType)
                        + ": " + members[i].name,
                        MinorInvalidMemberType,
                        COMPLETED_NO);
            org.omg.CORBA.TypeCode labelType = members[i].label.type();
            org.omg.CORBA.TypeCode origLabelType = _OB_getOrigType(labelType);
            TCKind kind = origLabelType.kind();
            if (kind != tk_octet
                    && !origLabelType.equivalent(discriminator_type)) {
                throw new BAD_PARAM(
                        describeBadParam(MinorIncompatibleLabelType)
                                + ": " + members[i].name,
                        MinorIncompatibleLabelType,
                        COMPLETED_NO);
            }
            for (int j = i + 1; j < members.length; j++) {
                if (kind != tk_octet) {
                    org.omg.CORBA.TypeCode otherLabelType = members[j].label
                            .type();
                    if (origLabelType.equivalent(otherLabelType)
                            && compareLabels(kind, members[i].label,
                                    members[j].label)) {
                        throw new BAD_PARAM(
                                describeBadParam(MinorDuplicateLabel)
                                        + ": " + members[i].name,
                                MinorDuplicateLabel,
                                COMPLETED_NO);
                    }
                }
            }
        }

        TypeCodeImpl tc = new TypeCodeImpl();

        tc.kind_ = tk_union;
        tc.id_ = id;
        tc.name_ = name;
        try {
            tc.discriminatorType_ = (TypeCodeImpl) discriminator_type;
        } catch (ClassCastException ex) {
            tc.discriminatorType_ = _OB_convertForeignTypeCode(discriminator_type);
        }

        tc.labels_ = new AnyImpl[members.length];
        tc.memberNames_ = new String[members.length];
        tc.memberTypes_ = new TypeCodeImpl[members.length];

        for (int i = 0; i < members.length; i++) {
            try {
                tc.labels_[i] = (AnyImpl) members[i].label;
            } catch (ClassCastException ex) {
                tc.labels_[i] = new AnyImpl(
                        members[i].label);
            }

            tc.memberNames_[i] = members[i].name;

            try {
                tc.memberTypes_[i] = (TypeCodeImpl) members[i].type;
            } catch (ClassCastException ex) {
                tc.memberTypes_[i] = _OB_convertForeignTypeCode(members[i].type);
            }
        }

        _OB_embedRecTC(tc);
        return tc;
    }

    public static org.omg.CORBA.TypeCode createEnumTC(String id, String name,
            String[] members) {
        Assert.ensure(id != null && name != null);

        if (!checkId(id))
            throw new BAD_PARAM(describeBadParam(MinorInvalidId)
                    + ": " + id, MinorInvalidId,
                    COMPLETED_NO);
        if (!checkName(name))
            throw new BAD_PARAM(describeBadParam(MinorInvalidName)
                    + ": " + name, MinorInvalidName,
                    COMPLETED_NO);

        for (int i = 0; i < members.length; i++) {
            if (!checkName(members[i]))
                throw new BAD_PARAM(describeBadParam(MinorInvalidMemberName)
                        + ": " + members[i], MinorInvalidMemberName,
                        COMPLETED_NO);
            for (int j = i + 1; j < members.length; j++)
                if (members[i].length() > 0
                        && members[i].equalsIgnoreCase(members[j])) {
                    throw new BAD_PARAM(
                            describeBadParam(MinorInvalidMemberName)
                                    + ": " + members[i],
                            MinorInvalidMemberName,
                            COMPLETED_NO);
                }
        }

        TypeCodeImpl tc = new TypeCodeImpl();

        tc.kind_ = tk_enum;
        tc.id_ = id;
        tc.name_ = name;

        tc.memberNames_ = new String[members.length];

        System.arraycopy(members, 0, tc.memberNames_, 0, members.length);

        return tc;
    }

    public static org.omg.CORBA.TypeCode createAliasTC(String id, String name,
            org.omg.CORBA.TypeCode original_type) {
        Assert.ensure(id != null && name != null);

        if (!checkId(id))
            throw new BAD_PARAM(describeBadParam(MinorInvalidId)
                    + ": " + id, MinorInvalidId,
                    COMPLETED_NO);
        if (!checkName(name))
            throw new BAD_PARAM(describeBadParam(MinorInvalidName)
                    + ": " + name, MinorInvalidName,
                    COMPLETED_NO);
        if (!checkType(original_type))
            throw new BAD_TYPECODE(describeBadTypecode(MinorInvalidMemberType),
                    MinorInvalidMemberType,
                    COMPLETED_NO);

        TypeCodeImpl tc = new TypeCodeImpl();

        tc.kind_ = tk_alias;
        tc.id_ = id;
        tc.name_ = name;

        try {
            tc.contentType_ = (TypeCodeImpl) original_type;
        } catch (ClassCastException ex) {
            tc.contentType_ = _OB_convertForeignTypeCode(original_type);
        }

        return tc;
    }

    public static org.omg.CORBA.TypeCode createExceptionTC(String id,
            String name, StructMember[] members) {
        Assert.ensure(id != null && name != null);

        if (!checkId(id))
            throw new BAD_PARAM(describeBadParam(MinorInvalidId)
                    + ": " + id, MinorInvalidId,
                    COMPLETED_NO);
        if (!checkName(name))
            throw new BAD_PARAM(describeBadParam(MinorInvalidName)
                    + ": " + name, MinorInvalidName,
                    COMPLETED_NO);

        for (int i = 0; i < members.length; i++) {
            if (!checkName(members[i].name))
                throw new BAD_PARAM(describeBadParam(MinorInvalidMemberName)
                        + ": " + members[i].name,
                        MinorInvalidMemberName,
                        COMPLETED_NO);
            if (!checkType(members[i].type))
                throw new BAD_TYPECODE(describeBadTypecode(MinorInvalidMemberType)
                        + ": " + members[i].name,
                        MinorInvalidMemberType,
                        COMPLETED_NO);
            for (int j = i + 1; j < members.length; j++)
                if (members[i].name.length() > 0
                        && members[i].name.equalsIgnoreCase(members[j].name)) {
                    throw new BAD_PARAM(
                            describeBadParam(MinorInvalidMemberName)
                                    + ": " + members[i].name,
                            MinorInvalidMemberName,
                            COMPLETED_NO);
                }
        }

        TypeCodeImpl tc = new TypeCodeImpl();

        tc.kind_ = tk_except;
        tc.id_ = id;
        tc.name_ = name;

        tc.memberNames_ = new String[members.length];
        tc.memberTypes_ = new TypeCodeImpl[members.length];

        for (int i = 0; i < members.length; i++) {
            tc.memberNames_[i] = members[i].name;
            try {
                tc.memberTypes_[i] = (TypeCodeImpl) members[i].type;
            } catch (ClassCastException ex) {
                tc.memberTypes_[i] = _OB_convertForeignTypeCode(members[i].type);
            }
        }

        _OB_embedRecTC(tc);
        return tc;
    }

    public static org.omg.CORBA.TypeCode createInterfaceTC(String id,
            String name) {
        Assert.ensure(id != null && name != null);

        if (!checkId(id))
            throw new BAD_PARAM(describeBadParam(MinorInvalidId)
                    + ": " + id, MinorInvalidId,
                    COMPLETED_NO);
        if (!checkName(name))
            throw new BAD_PARAM(describeBadParam(MinorInvalidName)
                    + ": " + name, MinorInvalidName,
                    COMPLETED_NO);

        TypeCodeImpl tc = new TypeCodeImpl();

        tc.kind_ = tk_objref;
        tc.id_ = id;
        tc.name_ = name;

        return tc;
    }

    public static org.omg.CORBA.TypeCode createStringTC(int bound) {
        Assert.ensure(bound >= 0);

        TypeCodeImpl tc = new TypeCodeImpl();

        tc.kind_ = tk_string;
        tc.length_ = bound;

        return tc;
    }

    public static org.omg.CORBA.TypeCode createWStringTC(int bound) {
        Assert.ensure(bound >= 0);

        TypeCodeImpl tc = new TypeCodeImpl();

        tc.kind_ = tk_wstring;
        tc.length_ = bound;

        return tc;
    }

    public static org.omg.CORBA.TypeCode createFixedTC(short digits, short scale) {
        Assert.ensure(digits >= 0);

        TypeCodeImpl tc = new TypeCodeImpl();

        tc.kind_ = tk_fixed;
        tc.fixedDigits_ = digits;
        tc.fixedScale_ = scale;

        return tc;
    }

    public static org.omg.CORBA.TypeCode createSequenceTC(int bound,
            org.omg.CORBA.TypeCode element_type) {
        Assert.ensure(bound >= 0);

        if (!checkType(element_type))
            throw new BAD_TYPECODE(describeBadTypecode(MinorInvalidMemberType),
                    MinorInvalidMemberType,
                    COMPLETED_NO);

        TypeCodeImpl tc = new TypeCodeImpl();

        tc.kind_ = tk_sequence;
        tc.length_ = bound;
        try {
            tc.contentType_ = (TypeCodeImpl) element_type;
        } catch (ClassCastException ex) {
            tc.contentType_ = _OB_convertForeignTypeCode(element_type);
        }

        return tc;
    }

    public static org.omg.CORBA.TypeCode createRecursiveSequenceTC(int bound,
            int offset) {
        throw new NO_IMPLEMENT();
    }

    public static org.omg.CORBA.TypeCode createArrayTC(int length,
            org.omg.CORBA.TypeCode element_type) {
        Assert.ensure(length > 0);

        if (!checkType(element_type))
            throw new BAD_TYPECODE(describeBadTypecode(MinorInvalidMemberType),
                    MinorInvalidMemberType,
                    COMPLETED_NO);

        TypeCodeImpl tc = new TypeCodeImpl();

        tc.kind_ = tk_array;
        tc.length_ = length;
        try {
            tc.contentType_ = (TypeCodeImpl) element_type;
        } catch (ClassCastException ex) {
            tc.contentType_ = _OB_convertForeignTypeCode(element_type);
        }

        return tc;
    }

    public static org.omg.CORBA.TypeCode createValueTC(String id, String name,
            short type_modifier, org.omg.CORBA.TypeCode concrete_base,
            ValueMember[] members) {
        Assert.ensure(id != null && name != null);

        if (!checkId(id))
            throw new BAD_PARAM(describeBadParam(MinorInvalidId)
                    + ": " + id, MinorInvalidId,
                    COMPLETED_NO);
        if (!checkName(name))
            throw new BAD_PARAM(describeBadParam(MinorInvalidName)
                    + ": " + name, MinorInvalidName,
                    COMPLETED_NO);
        if (concrete_base != null) {
            try {
                org.omg.CORBA.TypeCode origBaseType = _OB_getOrigType(concrete_base);
                if (origBaseType.kind() != tk_value)
                    throw new BAD_TYPECODE();
                // TODO: No standard minor code
            } catch (BAD_TYPECODE ex) {
                // TypeCode may be recursive
            }
        }

        for (int i = 0; i < members.length; i++) {
            if (!checkName(members[i].name))
                throw new BAD_PARAM(describeBadParam(MinorInvalidMemberName)
                        + ": " + members[i].name,
                        MinorInvalidMemberName,
                        COMPLETED_NO);
            if (!checkType(members[i].type))
                throw new BAD_TYPECODE(describeBadTypecode(MinorInvalidMemberType)
                        + ": " + members[i].name,
                        MinorInvalidMemberType,
                        COMPLETED_NO);
            if (!CHECK_IDL_NAMES) continue;
            for (int j = i + 1; j < members.length; j++)
                if (members[i].name.length() > 0
                        && members[i].name.equalsIgnoreCase(members[j].name)) {
                    throw new BAD_PARAM(
                            describeBadParam(MinorInvalidMemberName)
                                    + ": " + members[i].name,
                            MinorInvalidMemberName,
                            COMPLETED_NO);
                }
        }

        TypeCodeImpl tc = new TypeCodeImpl();

        tc.kind_ = tk_value;
        tc.id_ = id;
        tc.name_ = name;
        tc.typeModifier_ = type_modifier;
        try {
            tc.concreteBaseType_ = (TypeCodeImpl) concrete_base;
        } catch (ClassCastException ex) {
            tc.concreteBaseType_ = _OB_convertForeignTypeCode(concrete_base);
        }

        tc.memberNames_ = new String[members.length];
        tc.memberTypes_ = new TypeCodeImpl[members.length];
        tc.memberVisibility_ = new short[members.length];

        for (int i = 0; i < members.length; i++) {
            tc.memberNames_[i] = members[i].name;

            try {
                tc.memberTypes_[i] = (TypeCodeImpl) members[i].type;
            } catch (ClassCastException ex) {
                tc.memberTypes_[i] = _OB_convertForeignTypeCode(members[i].type);
            }

            tc.memberVisibility_[i] = members[i].access;
        }

        _OB_embedRecTC(tc);
        return tc;
    }

    public static org.omg.CORBA.TypeCode createValueBoxTC(String id,
            String name, org.omg.CORBA.TypeCode boxed_type) {
        Assert.ensure(id != null && name != null);

        if (!checkId(id))
            throw new BAD_PARAM(describeBadParam(MinorInvalidId)
                    + ": " + id, MinorInvalidId,
                    COMPLETED_NO);
        if (!checkName(name))
            throw new BAD_PARAM(describeBadParam(MinorInvalidName)
                    + ": " + name, MinorInvalidName,
                    COMPLETED_NO);
        if (!checkType(boxed_type))
            throw new BAD_TYPECODE(describeBadTypecode(MinorInvalidMemberType),
                    MinorInvalidMemberType,
                    COMPLETED_NO);

        TypeCodeImpl tc = new TypeCodeImpl();

        tc.kind_ = tk_value_box;
        tc.id_ = id;
        tc.name_ = name;
        try {
            tc.contentType_ = (TypeCodeImpl) boxed_type;
        } catch (ClassCastException ex) {
            tc.contentType_ = _OB_convertForeignTypeCode(boxed_type);
        }

        return tc;
    }

    public static org.omg.CORBA.TypeCode createNativeTC(String id, String name) {
        Assert.ensure(id != null && name != null);

        if (!checkId(id))
            throw new BAD_PARAM(describeBadParam(MinorInvalidId)
                    + ": " + id, MinorInvalidId,
                    COMPLETED_NO);
        if (!checkName(name))
            throw new BAD_PARAM(describeBadParam(MinorInvalidName)
                    + ": " + name, MinorInvalidName,
                    COMPLETED_NO);

        TypeCodeImpl tc = new TypeCodeImpl();

        tc.kind_ = tk_native;
        tc.id_ = id;
        tc.name_ = name;

        return tc;
    }

    public static org.omg.CORBA.TypeCode createRecursiveTC(String id) {
        Assert.ensure(id != null);

        if (!checkId(id))
            throw new BAD_PARAM(describeBadParam(MinorInvalidId)
                    + ": " + id, MinorInvalidId,
                    COMPLETED_NO);

        TypeCodeImpl tc = new TypeCodeImpl();

        tc.recId_ = id;

        return tc;
    }

    public static org.omg.CORBA.TypeCode createAbstractInterfaceTC(String id,
            String name) {
        Assert.ensure(id != null && name != null);

        if (!checkId(id))
            throw new BAD_PARAM(describeBadParam(MinorInvalidId)
                    + ": " + id, MinorInvalidId,
                    COMPLETED_NO);
        if (!checkName(name))
            throw new BAD_PARAM(describeBadParam(MinorInvalidName)
                    + ": " + name, MinorInvalidName,
                    COMPLETED_NO);

        TypeCodeImpl tc = new TypeCodeImpl();

        tc.kind_ = tk_abstract_interface;
        tc.id_ = id;
        tc.name_ = name;

        return tc;
    }

    public static org.omg.CORBA.TypeCode createLocalInterfaceTC(String id,
            String name) {
        Assert.ensure(id != null && name != null);

        if (!checkId(id))
            throw new BAD_PARAM(describeBadParam(MinorInvalidId)
                    + ": " + id, MinorInvalidId,
                    COMPLETED_NO);
        if (!checkName(name))
            throw new BAD_PARAM(describeBadParam(MinorInvalidName)
                    + ": " + name, MinorInvalidName,
                    COMPLETED_NO);

        TypeCodeImpl tc = new TypeCodeImpl();

        tc.kind_ = tk_local_interface;
        tc.id_ = id;
        tc.name_ = name;

        return tc;
    }
}
