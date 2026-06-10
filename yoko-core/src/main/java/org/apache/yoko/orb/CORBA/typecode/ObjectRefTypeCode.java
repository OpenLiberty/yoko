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

import static org.omg.CORBA.TCKind.tk_abstract_interface;
import static org.omg.CORBA.TCKind.tk_objref;
import static org.omg.CORBA_2_4.TCKind.tk_local_interface;

/**
 * TypeCode implementation for object reference types (tk_objref, tk_abstract_interface, tk_local_interface).
 */
public final class ObjectRefTypeCode extends YokoTypeCode {
    private final String id;
    private final String name;

    public ObjectRefTypeCode(TCKind kind, String id, String name) {
        super(kind);
        assert kind == tk_objref || kind == tk_abstract_interface || kind == tk_local_interface
            : "ObjectRefTypeCode only supports tk_objref, tk_abstract_interface, and tk_local_interface";
        this.id = id;
        this.name = name;
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
    protected StringBuilder describe(StringBuilder sb, String prefix, Set<String> describedIds) {
        final String indent = prefix + "\t";
        sb.append("TypeCode {").append(NL);
        sb.append(indent).append("kind: ").append(kind).append(NL);
        sb.append(indent).append("id: ").append(id).append(NL);
        sb.append(indent).append("name: ").append(name).append(NL);
        return sb.append(prefix).append("}");
    }

    @Override
    protected boolean equivalentRec(TypeCode t, List<TypeCode> history, List<TypeCode> otherHistory) {
        if (t.kind() != this.kind) return false;
        try {
            String otherId = t.id();
            if (!this.id.isEmpty() && !otherId.isEmpty()) {
                return this.id.equals(otherId);
            }
            // If either ID is empty, fall through to name comparison
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    protected YokoTypeCode getCompactTypeCodeRec(List<TypeCode> history, List<TypeCode> compacted) {
        // Return a compact version with empty name
        return new ObjectRefTypeCode(kind, id, "");
    }

    @Override
    public boolean equal(TypeCode other) {
        if (other == null) return false;
        if (other == this) return true;
        if (other.kind() != this.kind) return false;
        try {
            String otherId = other.id();
            String otherName = other.name();
            if (!this.id.isEmpty() || !otherId.isEmpty()) {
                return this.id.equals(otherId);
            }
            return this.name.equals(otherName);
        } catch (Exception e) {
            return false;
        }
    }
}
