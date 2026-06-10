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
import org.omg.CORBA.Any;
import org.omg.CORBA.TCKind;
import org.omg.CORBA.TypeCode;
import org.omg.CORBA.TypeCodePackage.Bounds;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.omg.CORBA.TCKind.tk_octet;
import static org.omg.CORBA.TCKind.tk_union;

/**
 * TypeCode implementation for union types.
 */
public final class UnionTypeCode extends YokoTypeCode {
    private final String id;
    private final String name;
    private final String[] memberNames;
    private final TypeCode[] memberTypes;
    private final Any[] labels;
    private final TypeCode discriminatorType;

    public UnionTypeCode(String id, String name, TypeCode discriminatorType,
                         String[] memberNames, TypeCode[] memberTypes, Any[] labels) {
        super(tk_union);
        this.id = id;
        this.name = name;
        this.discriminatorType = discriminatorType;
        this.memberNames = memberNames.clone();
        this.memberTypes = memberTypes.clone();
        this.labels = labels.clone();
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
    public Any member_label(int index) throws Bounds {
        try {
            return labels[index];
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw Exceptions.as(Bounds::new, ex);
        }
    }

    @Override
    public TypeCode discriminator_type() {
        return discriminatorType;
    }

    @Override
    public int default_index() {
        for (int i = 0; i < labels.length; i++) {
            TypeCode tc = labels[i].type();
            if (tc.kind() == tk_octet) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected StringBuilder describe(StringBuilder sb, String prefix, Set<String> describedIds) {
        final String indent = prefix + "\t";
        sb.append("TypeCode {").append(NL);
        sb.append(indent).append("kind: ").append(kind).append(NL);
        sb.append(indent).append("id: ").append(id).append(NL);
        sb.append(indent).append("name: ").append(name).append(NL);

        if (describedIds.contains(id)) {
            sb.append(indent).append("[already described]").append(NL);
            return sb.append(prefix).append("}");
        }
        describedIds.add(id);

        sb.append(indent).append("discriminator type: ");
        if (discriminatorType instanceof YokoTypeCode) {
            ((YokoTypeCode) discriminatorType).describe(sb, indent, describedIds);
        } else {
            sb.append(discriminatorType.toString());
        }
        sb.append(NL);

        for (int i = 0; i < memberNames.length; i++) {
            sb.append(indent).append(memberNames[i]).append(": ");
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
        if (t.kind() != tk_union) return false;
        try {
            String otherId = t.id();
            if (!this.id.isEmpty() && !otherId.isEmpty()) {
                return this.id.equals(otherId);
            }

            // Structural comparison
            if (!discriminatorType.equivalent(t.discriminator_type())) return false;

            int count = t.member_count();
            if (count != memberNames.length) return false;

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

                // Compare labels
                Any thisLabel = labels[i];
                Any otherLabel = t.member_label(i);
                if (!thisLabel.type().equal(otherLabel.type())) return false;
                if (!thisLabel.equal(otherLabel)) return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
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

        TypeCode compactDiscriminator = discriminatorType instanceof YokoTypeCode
            ? ((YokoTypeCode) discriminatorType).getCompactTypeCodeRec(history, compacted)
            : discriminatorType.get_compact_typecode();

        YokoTypeCode result = new UnionTypeCode(id, "", compactDiscriminator, emptyNames, compactTypes, labels);
        compacted.add(result);

        return result;
    }

    @Override
    public boolean equal(TypeCode other) {
        if (other == null) return false;
        if (other == this) return true;
        if (other.kind() != tk_union) return false;
        try {
            String otherId = other.id();
            String otherName = other.name();
            if (!this.id.isEmpty() || !otherId.isEmpty()) {
                if (!this.id.equals(otherId)) return false;
            }
            if (!this.name.equals(otherName)) return false;

            if (!discriminatorType.equal(other.discriminator_type())) return false;

            int count = other.member_count();
            if (count != memberNames.length) return false;

            for (int i = 0; i < count; i++) {
                if (!memberNames[i].equals(other.member_name(i))) return false;
                if (!memberTypes[i].equal(other.member_type(i))) return false;
                if (!labels[i].type().equal(other.member_label(i).type())) return false;
                if (!labels[i].equal(other.member_label(i))) return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
