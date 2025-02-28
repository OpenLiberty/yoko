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
import org.omg.CORBA.TypeCode;

final public class ExceptionList extends org.omg.CORBA.ExceptionList {
    java.util.Vector typeCodeVec_ = new Vector();

    // ------------------------------------------------------------------
    // Standard IDL to Java Mapping
    // ------------------------------------------------------------------

    public int count() {
        return typeCodeVec_.size();
    }

    public void add(TypeCode exc) {
        typeCodeVec_.addElement(exc);
    }

    public TypeCode item(int index) throws Bounds {
        try {
            return (TypeCode) typeCodeVec_.elementAt(index);
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw new org.omg.CORBA.Bounds();
        }
    }

    public void remove(int index) throws Bounds {
        try {
            typeCodeVec_.removeElementAt(index);
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw new Bounds();
        }
    }

    // ------------------------------------------------------------------
    // Yoko internal functions
    // Application programs must not use these functions directly
    // ------------------------------------------------------------------

    public ExceptionList() {
    }
}
