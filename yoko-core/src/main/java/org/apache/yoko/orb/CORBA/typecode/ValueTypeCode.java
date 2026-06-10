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
package org.apache.yoko.orb.CORBA.typecode;

import org.apache.yoko.util.Exceptions;
import org.omg.CORBA.TypeCode;
import org.omg.CORBA.TypeCodePackage.BadKind;
import org.omg.CORBA.TypeCodePackage.Bounds;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.omg.CORBA.TCKind.tk_value;

/**
 * TypeCode implementation for valuetype types.
 */
public final class ValueTypeCode extends YokoTypeCode {
    private final String id;
    private final String name;
    private final String[] memberNames;
    private final TypeCode[] memberTypes;
    private final short[] memberVisibility;
    private final short typeModifier;
    private final TypeCode concreteBaseType;

    public ValueTypeCode(String id, String name, short typeModifier, TypeCode concreteBaseType,
                         String[] memberNames, TypeCode[] memberTypes, short[] memberVisibility) {
        super(tk_value);
        this.id = id;
        this.name = name;
        this.typeModifier = typeModifier;
        this.concreteBaseType = concreteBaseType;
        this.memberNames = memberNames.clone();
        this.memberTypes = memberTypes.clone();
        this.memberVisibility = memberVisibility.clone();
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public int member_count() {
        return memberNames.length;
    }

    @Override
    public String member_name(int index) throws Bounds {
        try {
            return memberNames[index];
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw Exceptions.as(Bounds::new, ex);
        }
    }

    @Override
    public TypeCode member_type(int index) throws Bounds {
        try {
            return memberTypes[index];
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw Exceptions.as(Bounds::new, ex);
        }
    }

    @Override
    public short member_visibility(int index) throws Bounds {
        try {
            return memberVisibility[index];
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw Exceptions.as(Bounds::new, ex);
        }
    }

    @Override
    public short type_modifier() {
        return typeModifier;
    }

    @Override
    public TypeCode concrete_base_type() {
        return concreteBaseType;
    }

    @Override
    protected StringBuilder describe(StringBuilder sb, String prefix, Set<String> describedIds) {
        final String indent = prefix + "\t";
        sb.append("TypeCode {").append(NL);
        sb.append(indent).append("kind: ").append(kind).append(NL);
        sb.append(indent).append("id: ").append(id).append(NL);
        sb.append(indent).append("name: ").append(name).append(NL);
        sb.append(indent).append("type modifier: ").append(typeModifier).append(NL);

        if (describedIds.contains(id)) {
            sb.append(indent).append("[already described]").append(NL);
            return sb.append(prefix).append("}");
        }
        describedIds.add(id);

        if (concreteBaseType != null) {
            sb.append(indent).append("concrete base type: ");
            if (concreteBaseType instanceof YokoTypeCode) {
                ((YokoTypeCode) concreteBaseType).describe(sb, indent, describedIds);
            } else {
                sb.append(concreteBaseType);
            }
            sb.append(NL);
        }

        for (int i = 0; i < memberNames.length; i++) {
            String visibility = (memberVisibility[i] == org.omg.CORBA.PRIVATE_MEMBER.value) ? "[private]" : "[public]";
            sb.append(indent).append(memberNames[i]).append(visibility).append(": ");
            if (memberTypes[i] instanceof YokoTypeCode) {
                ((YokoTypeCode) memberTypes[i]).describe(sb, indent, describedIds);
            } else {
                sb.append(memberTypes[i].toString());
            }
            sb.append(NL);
        }
        return sb.append(prefix).append("}");
    }

    @Override
    protected boolean equivalentRec(TypeCode t, List<TypeCode> history, List<TypeCode> otherHistory) {
        if (t.kind() != tk_value) return false;
        try {
            String otherId = t.id();
            if (!this.id.isEmpty() && !otherId.isEmpty()) {
                return this.id.equals(otherId);
            }

            // Structural comparison
            if (t.type_modifier() != this.typeModifier) return false;

            int count = t.member_count();
            if (count != memberNames.length) return false;

            if (!Arrays.equals(this.memberVisibility, getMemberVisibilities(t, count))) return false;

            for (int i = 0; i < count; i++) {
                TypeCode thisMemberType = memberTypes[i];
                TypeCode otherMemberType = t.member_type(i);

                if (thisMemberType instanceof YokoTypeCode) {
                    if (!((YokoTypeCode) thisMemberType).equivalentRecHelper(otherMemberType, history, otherHistory)) {
                        return false;
                    }
                } else if (!thisMemberType.equivalent(otherMemberType)) {
                    return false;
                }
            }

            // Compare concrete base types
            if (concreteBaseType == null) return t.concrete_base_type() == null;
            return concreteBaseType.equivalent(t.concrete_base_type());
        } catch (Exception e) {
            return false;
        }
    }

    private short[] getMemberVisibilities(TypeCode t, int count) throws BadKind, Bounds {
        short[] visibilities = new short[count];
        for (int i = 0; i < count; i++) {
            visibilities[i] = t.member_visibility(i);
        }
        return visibilities;
    }

    @Override
    protected YokoTypeCode getCompactTypeCodeRec(List<TypeCode> history, List<TypeCode> compacted) {
        // Check for recursion
        for (int i = 0; i < history.size(); i++) {
            if (this == history.get(i)) {
                return (YokoTypeCode) compacted.get(i);
            }
        }

        history.add(this);

        // Create compact version with empty names
        String[] emptyNames = new String[memberNames.length];
        Arrays.fill(emptyNames, "");

        TypeCode[] compactTypes = new TypeCode[memberTypes.length];
        for (int i = 0; i < memberTypes.length; i++) {
            if (memberTypes[i] instanceof YokoTypeCode) {
                compactTypes[i] = ((YokoTypeCode) memberTypes[i]).getCompactTypeCodeRec(history, compacted);
            } else {
                compactTypes[i] = memberTypes[i].get_compact_typecode();
            }
        }

        TypeCode compactBase = (concreteBaseType == null) ? null :
            (concreteBaseType instanceof YokoTypeCode
                ? ((YokoTypeCode) concreteBaseType).getCompactTypeCodeRec(history, compacted)
                : concreteBaseType.get_compact_typecode());

        YokoTypeCode result = new ValueTypeCode(id, "", typeModifier, compactBase, emptyNames, compactTypes, memberVisibility);
        compacted.add(result);

        return result;
    }

    @Override
    public boolean equal(TypeCode other) {
        if (other == null) return false;
        if (other == this) return true;
        if (other.kind() != tk_value) return false;
        try {
            String otherId = other.id();
            String otherName = other.name();
            if (!this.id.isEmpty() || !otherId.isEmpty()) {
                if (!this.id.equals(otherId)) return false;
            }
            if (!this.name.equals(otherName)) return false;
            if (other.type_modifier() != this.typeModifier) return false;

            int count = other.member_count();
            if (count != memberNames.length) return false;

            if (!Arrays.equals(this.memberVisibility, getMemberVisibilities(other, count))) return false;

            for (int i = 0; i < count; i++) {
                if (!memberNames[i].equals(other.member_name(i))) return false;
                if (!memberTypes[i].equal(other.member_type(i))) return false;
            }

            // Compare concrete base types
            if (concreteBaseType == null) return other.concrete_base_type() == null;
            return concreteBaseType.equal(other.concrete_base_type());
        } catch (Exception e) {
            return false;
        }
    }
}
