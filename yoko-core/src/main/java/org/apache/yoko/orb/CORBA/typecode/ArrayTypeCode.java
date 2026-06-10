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

import org.omg.CORBA.BAD_TYPECODE;
import org.omg.CORBA.TypeCode;
import org.omg.CORBA.TypeCodePackage.BadKind;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.yoko.util.Exceptions.as;
import static org.apache.yoko.util.MinorCodes.MinorTypeMismatch;
import static org.omg.CORBA.CompletionStatus.COMPLETED_NO;
import static org.omg.CORBA.TCKind.tk_array;

/**
 * TypeCode implementation for array types.
 */
public final class ArrayTypeCode extends YokoTypeCode {
    private final int length;
    private final YokoTypeCode contentType;

    public ArrayTypeCode(int length, TypeCode contentType) {
        super(tk_array);
        this.length = length;
        this.contentType = YokoTypeCode.from(contentType);
    }

    @Override
    public int length() {
        return length;
    }

    @Override
    public TypeCode content_type() {
        return contentType;
    }

    @Override
    protected StringBuilder describe(StringBuilder sb, String prefix, Set<String> describedIds) {
        final String indent = prefix + "\t";
        sb.append("TypeCode {").append(NL);
        sb.append(indent).append("kind: ").append(kind).append(NL);
        sb.append(indent).append("length: ").append(length).append(NL);
        sb.append(indent).append("content type: ");
        if (contentType instanceof YokoTypeCode) {
            ((YokoTypeCode) contentType).describe(sb, indent, describedIds);
        } else {
            sb.append(contentType.toString());
        }
        sb.append(NL);
        return sb.append(prefix).append("}");
    }

    @Override
    protected boolean equivalentRec(TypeCode t, List<TypeCode> history, List<TypeCode> otherHistory) {
        if (t.kind() != tk_array) return false;
        try {
            if (t.length() != this.length) return false;

            TypeCode otherContent = t.content_type();
            if (contentType instanceof YokoTypeCode) {
                return ((YokoTypeCode) contentType).equivalentRecHelper(otherContent, history, otherHistory);
            }
            return contentType.equivalent(otherContent);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    protected YokoTypeCode getCompactTypeCodeRec(List<TypeCode> history, List<TypeCode> compacted) {
        TypeCode compactContent = contentType instanceof YokoTypeCode
            ? ((YokoTypeCode) contentType).getCompactTypeCodeRec(history, compacted)
            : contentType.get_compact_typecode();

        return new ArrayTypeCode(length, compactContent);
    }

    @Override
    public boolean equal(TypeCode other) {
        if (other == null) return false;
        if (other == this) return true;
        if (other.kind() != tk_array) return false;
        try {
            return other.length() == this.length && contentType.equal(other.content_type());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Converts a foreign TypeCode to an ArrayTypeCode.
     * 
     * @param tc the TypeCode to convert (must be tk_array)
     * @param history map of already converted TypeCodes
     * @param recHistory list of TypeCodes currently being processed
     * @return a new ArrayTypeCode instance
     */
    public static YokoTypeCode from(TypeCode tc, Map<TypeCode, YokoTypeCode> history, List<TypeCode> recHistory) {
        try {
            TypeCode contentType = tc.content_type();
            YokoTypeCode convertedContent = YokoTypeCode.from(contentType);
            return new ArrayTypeCode(tc.length(), convertedContent);
        } catch (BadKind e) {
            throw as(BAD_TYPECODE::new, e, "Invalid array TypeCode", MinorTypeMismatch, COMPLETED_NO);
        }
    }
}
