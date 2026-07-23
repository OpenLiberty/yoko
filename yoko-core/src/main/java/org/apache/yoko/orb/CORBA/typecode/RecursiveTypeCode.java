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

import org.omg.CORBA.Any;
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.BAD_TYPECODE;
import org.omg.CORBA.TCKind;
import org.omg.CORBA.TypeCode;
import org.omg.CORBA.TypeCodePackage.BadKind;
import org.omg.CORBA.TypeCodePackage.Bounds;

import java.util.List;
import java.util.Set;

import static org.apache.yoko.util.MinorCodes.MinorIncompleteTypeCode;
import static org.apache.yoko.util.MinorCodes.MinorIncompleteTypeCodeParameter;
import static org.apache.yoko.util.MinorCodes.describeBadParam;
import static org.apache.yoko.util.MinorCodes.describeBadTypecode;
import static org.omg.CORBA.CompletionStatus.COMPLETED_NO;

/**
 * TypeCode implementation for recursive type placeholders.
 * This is a special TypeCode created with create_recursive_tc() that acts as a placeholder
 * until it is embedded in the actual recursive type definition.
 */
public final class RecursiveTypeCode extends YokoTypeCode {
    private final String recId;
    private TypeCode recType;

    public RecursiveTypeCode(String recId) {
        super(null); // Kind is determined by the resolved type
        this.recId = recId;
        this.recType = null;
    }

    /**
     * Sets the actual recursive TypeCode that this placeholder refers to.
     * This is called during the embedding process.
     */
    public void setRecursiveType(TypeCode recType) {
        this.recType = recType;
    }

    /**
     * Gets the recursive ID for this placeholder.
     */
    public String getRecursiveId() {
        return recId;
    }

    /**
     * Gets the resolved recursive TypeCode, or null if not yet resolved.
     */
    public TypeCode getRecursiveType() {
        return recType;
    }

    private void checkResolved() throws BAD_TYPECODE {
        if (recType == null) {
            throw new BAD_TYPECODE(
                describeBadTypecode(MinorIncompleteTypeCode),
                MinorIncompleteTypeCode,
                COMPLETED_NO);
        }
    }

    @Override
    public TCKind kind() {
        checkResolved();
        return recType.kind();
    }

    @Override
    public String id() throws BadKind {
        checkResolved();
        return recType.id();
    }

    @Override
    public String name() throws BadKind {
        checkResolved();
        return recType.name();
    }

    @Override
    public int member_count() throws BadKind {
        checkResolved();
        return recType.member_count();
    }

    @Override
    public String member_name(int index) throws BadKind, Bounds {
        checkResolved();
        return recType.member_name(index);
    }

    @Override
    public TypeCode member_type(int index) throws BadKind, Bounds {
        checkResolved();
        return recType.member_type(index);
    }

    @Override
    public Any member_label(int index) throws BadKind, Bounds {
        checkResolved();
        return recType.member_label(index);
    }

    @Override
    public TypeCode discriminator_type() throws BadKind {
        checkResolved();
        return recType.discriminator_type();
    }

    @Override
    public int default_index() throws BadKind {
        checkResolved();
        return recType.default_index();
    }

    @Override
    public int length() throws BadKind {
        checkResolved();
        return recType.length();
    }

    @Override
    public TypeCode content_type() throws BadKind {
        checkResolved();
        return recType.content_type();
    }

    @Override
    public short fixed_digits() throws BadKind {
        checkResolved();
        return recType.fixed_digits();
    }

    @Override
    public short fixed_scale() throws BadKind {
        checkResolved();
        return recType.fixed_scale();
    }

    @Override
    public short member_visibility(int index) throws BadKind, Bounds {
        checkResolved();
        return recType.member_visibility(index);
    }

    @Override
    public short type_modifier() throws BadKind {
        checkResolved();
        return recType.type_modifier();
    }

    @Override
    public TypeCode concrete_base_type() throws BadKind {
        checkResolved();
        return recType.concrete_base_type();
    }

    @Override
    protected StringBuilder describe(StringBuilder sb, String prefix, Set<String> describedIds) {
        final String indent = prefix + "\t";
        sb.append("TypeCode {").append(NL);
        sb.append(indent).append("recursive id: ").append(recId).append(NL);
        if (recType != null) {
            sb.append(indent).append("resolved to: ");
            if (recType instanceof YokoTypeCode) {
                ((YokoTypeCode) recType).describe(sb, indent, describedIds);
            } else {
                sb.append(recType.toString());
            }
            sb.append(NL);
        } else {
            sb.append(indent).append("[not yet resolved]").append(NL);
        }
        return sb.append(prefix).append("}");
    }

    @Override
    protected boolean equivalentRec(TypeCode t, List<TypeCode> history, List<TypeCode> otherHistory) {
        checkResolved();

        if (t instanceof RecursiveTypeCode) {
            RecursiveTypeCode other = (RecursiveTypeCode) t;
            if (other.recType == null) {
                throw new BAD_PARAM(
                    describeBadParam(MinorIncompleteTypeCodeParameter),
                    MinorIncompleteTypeCodeParameter,
                    COMPLETED_NO);
            }
            t = other.recType;
        }

        if (recType instanceof YokoTypeCode) {
            return ((YokoTypeCode) recType).equivalentRecHelper(t, history, otherHistory);
        }
        return recType.equivalent(t);
    }

    @Override
    protected YokoTypeCode getCompactTypeCodeRec(List<TypeCode> history, List<TypeCode> compacted) {
        checkResolved();
        if (recType instanceof YokoTypeCode) {
            return ((YokoTypeCode) recType).getCompactTypeCodeRec(history, compacted);
        }
        return (YokoTypeCode) recType.get_compact_typecode();
    }

    @Override
    public boolean equal(TypeCode other) {
        if (other == null) return false;
        if (other == this) return true;

        checkResolved();

        if (other instanceof RecursiveTypeCode) {
            RecursiveTypeCode otherRec = (RecursiveTypeCode) other;
            if (otherRec.recType == null) {
                throw new BAD_PARAM(
                    describeBadParam(MinorIncompleteTypeCodeParameter),
                    MinorIncompleteTypeCodeParameter,
                    COMPLETED_NO);
            }
            return recType.equal(otherRec.recType);
        }

        return recType.equal(other);
    }
}
