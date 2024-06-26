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
package org.omg.DynamicAny;

//
// IDL:omg.org/DynamicAny/NameDynAnyPair:1.0
//
public class NameDynAnyPairHelper
{
    public static void
    insert(org.omg.CORBA.Any any, NameDynAnyPair val)
    {
        org.omg.CORBA.portable.OutputStream out = any.create_output_stream();
        write(out, val);
        any.read_value(out.create_input_stream(), type());
    }

    public static NameDynAnyPair
    extract(org.omg.CORBA.Any any)
    {
        if(any.type().equivalent(type()))
            return read(any.create_input_stream());
        else
            throw new org.omg.CORBA.BAD_OPERATION();
    }

    private static org.omg.CORBA.TypeCode typeCode_;

    public static org.omg.CORBA.TypeCode
    type()
    {
        if(typeCode_ == null)
        {
            org.omg.CORBA.ORB orb = org.omg.CORBA.ORB.init();
            org.omg.CORBA.StructMember[] members = new org.omg.CORBA.StructMember[2];

            members[0] = new org.omg.CORBA.StructMember();
            members[0].name = "id";
            members[0].type = FieldNameHelper.type();

            members[1] = new org.omg.CORBA.StructMember();
            members[1].name = "value";
            members[1].type = DynAnyHelper.type();

            typeCode_ = orb.create_struct_tc(id(), "NameDynAnyPair", members);
        }

        return typeCode_;
    }

    public static String
    id()
    {
        return "IDL:omg.org/DynamicAny/NameDynAnyPair:1.0";
    }

    public static NameDynAnyPair
    read(org.omg.CORBA.portable.InputStream in)
    {
        throw new org.omg.CORBA.MARSHAL();
    }

    public static void
    write(org.omg.CORBA.portable.OutputStream out, NameDynAnyPair val)
    {
        throw new org.omg.CORBA.MARSHAL();
    }
}
