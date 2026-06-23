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
import org.omg.CORBA.BAD_TYPECODE;
import org.omg.CORBA.TypeCode;
import org.omg.CORBA.TypeCodePackage.BadKind;
import org.omg.CORBA.TypeCodePackage.Bounds;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.yoko.util.Exceptions.as;
import static org.apache.yoko.util.MinorCodes.MinorTypeMismatch;
import static org.omg.CORBA.CompletionStatus.COMPLETED_NO;
import static org.omg.CORBA.TCKind.tk_except;

/**
 * TypeCode implementation for exception types.
 */
public final class ExceptionTypeCode extends YokoTypeCode {
    private final String id;
    private final String name;
    private final String[] memberNames;
    private final YokoTypeCode[] memberTypes;

    public ExceptionTypeCode(String id, String name, String[] memberNames, TypeCode[] memberTypes) {
        super(tk_except);
        this.id = id;
        this.name = name;
        this.memberNames = memberNames.clone();
        this.memberTypes = Arrays.stream(memberTypes)
            .map(YokoTypeCode::from)
            .toArray(YokoTypeCode[]::new);
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
        if (t.kind() != tk_except) return false;
        try {
            String otherId = t.id();
            if (!this.id.isEmpty() && !otherId.isEmpty()) {
                return this.id.equals(otherId);
            }

            // Structural comparison
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

        YokoTypeCode result = new ExceptionTypeCode(id, "", emptyNames, compactTypes);
        compacted.add(result);

        return result;
    }

    @Override
    public boolean equal(TypeCode other) {
        if (other == null) return false;
        if (other == this) return true;
        if (other.kind() != tk_except) return false;
        try {
            String otherId = other.id();
            String otherName = other.name();
            if (!this.id.isEmpty() || !otherId.isEmpty()) {
                if (!this.id.equals(otherId)) return false;
            }
            if (!this.name.equals(otherName)) return false;

            int count = other.member_count();
            if (count != memberNames.length) return false;

            for (int i = 0; i < count; i++) {
                if (!memberNames[i].equals(other.member_name(i))) return false;
                if (!memberTypes[i].equal(other.member_type(i))) return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Converts a foreign TypeCode to an ExceptionTypeCode.
     * 
     * @param tc the TypeCode to convert (must be tk_except)
     * @param history map of already converted TypeCodes
     * @param recHistory list of TypeCodes currently being processed (for recursion detection)
     * @return a new ExceptionTypeCode instance
     */
    public static YokoTypeCode from(TypeCode tc, Map<TypeCode, YokoTypeCode> history, List<TypeCode> recHistory) {
        try {
            int count = tc.member_count();
            String[] memberNames = new String[count];
            TypeCode[] memberTypes = new TypeCode[count];
            
            // Collect member names
            for (int i = 0; i < count; i++) {
                memberNames[i] = tc.member_name(i);
            }
            
            // Add to recursion history before processing member types
            recHistory.add(tc);
            
            // Convert member types (may encounter recursion)
            for (int i = 0; i < count; i++) {
                memberTypes[i] = YokoTypeCode.from(tc.member_type(i));
            }
            
            // Remove from recursion history
            recHistory.remove(recHistory.size() - 1);
            
            return new ExceptionTypeCode(tc.id(), tc.name(), memberNames, memberTypes);
        } catch (BadKind | Bounds e) {
            throw as(BAD_TYPECODE::new, e, "Invalid exception TypeCode", MinorTypeMismatch, COMPLETED_NO);
        }
    }
}
