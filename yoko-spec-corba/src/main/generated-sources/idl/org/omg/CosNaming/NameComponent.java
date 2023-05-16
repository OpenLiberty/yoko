/*==============================================================================
 * Copyright 2015 IBM Corporation and others.
 * 
 * Licensed under the Apache License, Version 2.0 (the License);
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
 *=============================================================================*/
package org.omg.CosNaming;

/**
 * org/omg/CosNaming/NameComponent.java . Error reading Messages File. Error
 * reading Messages File. Thursday, January 14, 2010 1:08:58 AM PST
 */

public final class NameComponent implements org.omg.CORBA.portable.IDLEntity {
    private static final long serialVersionUID = -1052538183391762390L;
    public String id = null;
    public String kind = null;

    public NameComponent() {
    }

    public NameComponent(String _id, String _kind) {
        id = _id;
        kind = _kind;
    }

    @Override
    public String toString() {
        final String eid = escape(id);
        return ((!!!"".equals(eid)) && "".equals(kind)) ? eid : (eid + '.' + escape(kind));
    }
    
    /** escape DOT, SLASH, and BACKSLASH as per CosNaming v1.4 section 2.4.2 */
    private static String escape(String s) {
        if (s == null) return s;
        return s.replaceAll("([\\\./])", "\\$1");
    }
}
