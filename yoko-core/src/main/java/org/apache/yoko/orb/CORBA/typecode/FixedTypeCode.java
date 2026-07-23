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

import org.omg.CORBA.TypeCode;

import java.util.List;
import java.util.Set;

import static org.omg.CORBA.TCKind.tk_fixed;

/**
 * TypeCode implementation for fixed-point decimal types.
 */
public final class FixedTypeCode extends YokoTypeCode {
    private final short digits;
    private final short scale;

    public FixedTypeCode(short digits, short scale) {
        super(tk_fixed);
        this.digits = digits;
        this.scale = scale;
    }

    @Override
    public short fixed_digits() {
        return digits;
    }

    @Override
    public short fixed_scale() {
        return scale;
    }

    @Override
    protected StringBuilder describe(StringBuilder sb, String prefix, Set<String> describedIds) {
        final String indent = prefix + "\t";
        sb.append("TypeCode {").append(NL);
        sb.append(indent).append("kind: ").append(kind).append(NL);
        sb.append(indent).append("fixed digits: ").append(digits).append(NL);
        sb.append(indent).append("fixed scale: ").append(scale).append(NL);
        return sb.append(prefix).append("}");
    }

    @Override
    protected boolean equivalentRec(TypeCode t, List<TypeCode> history, List<TypeCode> otherHistory) {
        if (t.kind() != tk_fixed) return false;
        try {
            return t.fixed_digits() == this.digits && t.fixed_scale() == this.scale;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    protected YokoTypeCode getCompactTypeCodeRec(List<TypeCode> history, List<TypeCode> compacted) {
        // Fixed TypeCodes are already compact
        return this;
    }

    @Override
    public boolean equal(TypeCode other) {
        if (other == null) return false;
        if (other == this) return true;
        if (other.kind() != tk_fixed) return false;
        try {
            return other.fixed_digits() == this.digits && other.fixed_scale() == this.scale;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Converts a foreign TypeCode to a FixedTypeCode.
     * 
     * @param tc the TypeCode to convert
     * @param history map of already converted TypeCodes (unused for fixed types)
     * @param recHistory list of TypeCodes currently being processed (unused for fixed types)
     * @return a new FixedTypeCode instance
     */
    public static YokoTypeCode from(TypeCode tc, java.util.Map<TypeCode, YokoTypeCode> history, java.util.List<TypeCode> recHistory) {
        try {
            return new FixedTypeCode(tc.fixed_digits(), tc.fixed_scale());
        } catch (org.omg.CORBA.TypeCodePackage.BadKind e) {
            throw new org.omg.CORBA.BAD_TYPECODE("Invalid fixed TypeCode: " + e.getMessage());
        }
    }
}
