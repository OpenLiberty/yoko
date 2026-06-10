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
import static org.omg.CORBA.TCKind.tk_value_box;

/**
 * TypeCode implementation for valuebox types.
 */
public final class ValueBoxTypeCode extends YokoTypeCode {
    private final String id;
    private final String name;
    private final YokoTypeCode contentType;

    public ValueBoxTypeCode(String id, String name, TypeCode contentType) {
        super(tk_value_box);
        this.id = id;
        this.name = name;
        this.contentType = YokoTypeCode.from(contentType);
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
    public TypeCode content_type() {
        return contentType;
    }

    @Override
    protected StringBuilder describe(StringBuilder sb, String prefix, Set<String> describedIds) {
        final String indent = prefix + "\t";
        sb.append("TypeCode {").append(NL);
        sb.append(indent).append("kind: ").append(kind).append(NL);
        sb.append(indent).append("id: ").append(id).append(NL);
        sb.append(indent).append("name: ").append(name).append(NL);
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
        if (t.kind() != tk_value_box) return false;
        try {
            String otherId = t.id();
            if (!this.id.isEmpty() && !otherId.isEmpty()) {
                return this.id.equals(otherId);
            }

            // Structural comparison
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

        return new ValueBoxTypeCode(id, "", compactContent);
    }

    @Override
    public boolean equal(TypeCode other) {
        if (other == null) return false;
        if (other == this) return true;
        if (other.kind() != tk_value_box) return false;
        try {
            String otherId = other.id();
            String otherName = other.name();
            if (!this.id.isEmpty() || !otherId.isEmpty()) {
                if (!this.id.equals(otherId)) return false;
            }
            if (!this.name.equals(otherName)) return false;
            return contentType.equal(other.content_type());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Converts a foreign TypeCode to a ValueBoxTypeCode.
     * 
     * @param tc the TypeCode to convert (must be tk_value_box)
     * @param history map of already converted TypeCodes
     * @param recHistory list of TypeCodes currently being processed
     * @return a new ValueBoxTypeCode instance
     */
    public static YokoTypeCode from(TypeCode tc, Map<TypeCode, YokoTypeCode> history, List<TypeCode> recHistory) {
        try {
            TypeCode contentType = tc.content_type();
            YokoTypeCode convertedContent = YokoTypeCode.from(contentType);
            return new ValueBoxTypeCode(tc.id(), tc.name(), convertedContent);
        } catch (BadKind e) {
            throw as(BAD_TYPECODE::new, e, "Invalid value box TypeCode", MinorTypeMismatch, COMPLETED_NO);
        }
    }
}
