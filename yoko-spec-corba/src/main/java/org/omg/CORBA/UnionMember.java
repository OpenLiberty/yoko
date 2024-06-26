/*
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
package org.omg.CORBA;

//
// IDL:omg.org/CORBA/UnionMember:1.0
//
/***/

final public class UnionMember implements org.omg.CORBA.portable.IDLEntity
{
    private static final String _ob_id = "IDL:omg.org/CORBA/UnionMember:1.0";

    public
    UnionMember()
    {
    }

    public
    UnionMember(String name,
                org.omg.CORBA.Any label,
                org.omg.CORBA.TypeCode type,
                IDLType type_def)
    {
        this.name = name;
        this.label = label;
        this.type = type;
        this.type_def = type_def;
    }

    public String name;
    public org.omg.CORBA.Any label;
    public org.omg.CORBA.TypeCode type;
    public IDLType type_def;
}
