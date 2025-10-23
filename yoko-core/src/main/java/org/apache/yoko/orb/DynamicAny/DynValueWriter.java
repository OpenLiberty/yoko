/*
 * Copyright 2025 IBM Corporation and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an \"AS IS\" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.apache.yoko.orb.DynamicAny;

import org.apache.yoko.orb.CORBA.YokoOutputStream;
import org.apache.yoko.orb.OB.ORBInstance;
import org.omg.DynamicAny.DynAny;
import org.omg.DynamicAny.DynAnyFactory;

import java.util.HashMap;
import java.util.Map;

final public class DynValueWriter {

    private final DynValueReader dynValueReader_;

    private final Map<Object, Integer> instanceTable_;

    public DynValueWriter(ORBInstance orbInstance, DynAnyFactory factory) {
        instanceTable_ = new HashMap<>(131);
        dynValueReader_ = new DynValueReader(orbInstance, factory, false);
    }

    public boolean writeIndirection(DynAny dv, YokoOutputStream out) {
        Integer pos = instanceTable_.get(dv);
        if (pos == null) return false;
        out.write_long(-1);
        int off = pos - out.getPosition();
        out.write_long(off);
        return true;

    }

    public void indexValue(DynAny dv, int startPos) {
        instanceTable_.put(dv, startPos);
        dynValueReader_.indexValue(startPos, dv);
    }

    public DynValueReader getReader() {
        return dynValueReader_;
    }
}
