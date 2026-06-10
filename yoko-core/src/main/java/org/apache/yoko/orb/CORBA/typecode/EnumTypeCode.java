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
import org.omg.CORBA.TypeCodePackage.Bounds;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.omg.CORBA.TCKind.tk_enum;

/**
 * TypeCode implementation for enumeration types.
 */
public final class EnumTypeCode extends YokoTypeCode {
    private final String id;
    private final String name;
    private final String[] memberNames;

    public EnumTypeCode(String id, String name, String[] memberNames) {
        super(tk_enum);
        this.id = id;
        this.name = name;
        this.memberNames = memberNames.clone();
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
            throw new Bounds();
        }
    }

    @Override
    protected StringBuilder describe(StringBuilder sb, String prefix, Set<String> describedIds) {
        final String indent = prefix + "\t";
        sb.append("TypeCode {").append(NL);
        sb.append(indent).append("kind: ").append(kind).append(NL);
        sb.append(indent).append("id: ").append(id).append(NL);
        sb.append(indent).append("name: ").append(name).append(NL);
        sb.append(indent).append("members: ").append(Arrays.toString(memberNames)).append(NL);
        return sb.append(prefix).append("}");
    }

    @Override
    protected boolean equivalentRec(TypeCode t, List<TypeCode> history, List<TypeCode> otherHistory) {
        if (t.kind() != tk_enum) return false;
        try {
            String otherId = t.id();
            if (!this.id.isEmpty() && !otherId.isEmpty()) {
                return this.id.equals(otherId);
            }
            // If either ID is empty, fall through to structural comparison
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    protected YokoTypeCode getCompactTypeCodeRec(List<TypeCode> history, List<TypeCode> compacted) {
        // Return a compact version with empty name and empty member names
        String[] emptyNames = new String[memberNames.length];
        Arrays.fill(emptyNames, "");
        return new EnumTypeCode(id, "", emptyNames);
    }

    @Override
    public boolean equal(TypeCode other) {
        if (other == null) return false;
        if (other == this) return true;
        if (other.kind() != tk_enum) return false;
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
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
