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
package org.omg.Messaging;

//
// IDL:omg.org/Messaging/PriorityRange:1.0
//
final public class PriorityRangeHelper
{
    public static void
    insert(org.omg.CORBA.Any any, PriorityRange val)
    {
        org.omg.CORBA.portable.OutputStream out = any.create_output_stream();
        write(out, val);
        any.read_value(out.create_input_stream(), type());
    }

    public static PriorityRange
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
            members[0].name = "min";
            members[0].type = PriorityHelper.type();

            members[1] = new org.omg.CORBA.StructMember();
            members[1].name = "max";
            members[1].type = PriorityHelper.type();

            typeCode_ = orb.create_struct_tc(id(), "PriorityRange", members);
        }

        return typeCode_;
    }

    public static String
    id()
    {
        return "IDL:omg.org/Messaging/PriorityRange:1.0";
    }

    public static PriorityRange
    read(org.omg.CORBA.portable.InputStream in)
    {
        PriorityRange _ob_v = new PriorityRange();
        _ob_v.min = PriorityHelper.read(in);
        _ob_v.max = PriorityHelper.read(in);
        return _ob_v;
    }

    public static void
    write(org.omg.CORBA.portable.OutputStream out, PriorityRange val)
    {
        PriorityHelper.write(out, val.min);
        PriorityHelper.write(out, val.max);
    }
}
