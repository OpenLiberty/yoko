/*
 * Copyright 2024 IBM Corporation and others.
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
package org.apache.yoko.orb.CORBA;

import java.util.Vector;

import org.omg.CORBA.Bounds;
import org.omg.CORBA.ORB;

final public class NVList extends org.omg.CORBA.NVList {
    private org.omg.CORBA.ORB orb_;

    private Vector namedValueVec_ = new Vector();

    // ------------------------------------------------------------------
    // Standard IDL to Java Mapping
    // ------------------------------------------------------------------

    public int count() {
        return namedValueVec_.size();
    }

    public org.omg.CORBA.NamedValue add(int flags) {
        NamedValue n = new NamedValue("", orb_.create_any(), flags);
        namedValueVec_.addElement(n);
        return n;
    }

    public org.omg.CORBA.NamedValue add_item(String item_name, int flags) {
        NamedValue n = new NamedValue(item_name, orb_.create_any(), flags);
        namedValueVec_.addElement(n);
        return n;
    }

    public org.omg.CORBA.NamedValue add_value(String item_name,
            org.omg.CORBA.Any a, int flags) {
        NamedValue n = new NamedValue(item_name, a, flags);
        namedValueVec_.addElement(n);
        return n;
    }

    public org.omg.CORBA.NamedValue item(int index) throws Bounds {
        try {
            return (NamedValue) namedValueVec_.elementAt(index);
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw new Bounds();
        }
    }

    public void remove(int index) throws org.omg.CORBA.Bounds {
        try {
            namedValueVec_.removeElementAt(index);
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw new Bounds();
        }
    }

    // ------------------------------------------------------------------
    // Yoko internal functions
    // Application programs must not use these functions directly
    // ------------------------------------------------------------------

    public NVList(ORB orb) {
        orb_ = orb;
    }

    public NVList(ORB orb, int n) {
        orb_ = orb;

        //
        // The parameter n is only a "hint" for how many elements the
        // user intends to create with operations such as
        // add_item(). Thus the following code is disabled:
        //
        // for(int i = 0 ; i < n ; i++)
        // {
        // NamedValue nv = new NamedValue("", orb_.create_any(), 0);
        // namedValueVec_.addElement(nv);
        // }
    }
}
