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

import org.omg.CORBA.TCKind;
import org.omg.CORBA.TypeCode;

import java.util.List;
import java.util.Set;

import static org.omg.CORBA.TCKind.tk_string;
import static org.omg.CORBA.TCKind.tk_wstring;

/**
 * TypeCode implementation for string and wstring types.
 */
public final class StringTypeCode extends YokoTypeCode {
    private final int length;

    public StringTypeCode(TCKind kind, int length) {
        super(kind);
        assert kind == tk_string || kind == tk_wstring : "StringTypeCode only supports tk_string and tk_wstring";
        this.length = length;
    }

    @Override
    public int length() {
        return length;
    }

    @Override
    protected StringBuilder describe(StringBuilder sb, String prefix, Set<String> describedIds) {
        final String indent = prefix + "\t";
        sb.append("TypeCode {").append(NL);
        sb.append(indent).append("kind: ").append(kind).append(NL);
        if (length != 0) {
            sb.append(indent).append("length: ").append(length).append(NL);
        }
        return sb.append(prefix).append("}");
    }

    @Override
    protected boolean equivalentRec(TypeCode t, List<TypeCode> history, List<TypeCode> otherHistory) {
        if (t.kind() != this.kind) return false;
        try {
            return t.length() == this.length;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    protected YokoTypeCode getCompactTypeCodeRec(List<TypeCode> history, List<TypeCode> compacted) {
        // String TypeCodes are already compact
        return this;
    }

    @Override
    public boolean equal(TypeCode other) {
        if (other == null) return false;
        if (other == this) return true;
        if (other.kind() != this.kind) return false;
        try {
            return other.length() == this.length;
        } catch (Exception e) {
            return false;
        }
    }
}
