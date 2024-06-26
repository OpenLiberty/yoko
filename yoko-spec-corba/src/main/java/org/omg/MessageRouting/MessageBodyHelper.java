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
package org.omg.MessageRouting;

//
// IDL:omg.org/MessageRouting/MessageBody:1.0
//
final public class MessageBodyHelper
{
    public static void
    insert(org.omg.CORBA.Any any, MessageBody val)
    {
        org.omg.CORBA.portable.OutputStream out = any.create_output_stream();
        write(out, val);
        any.read_value(out.create_input_stream(), type());
    }

    public static MessageBody
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
            members[0].name = "body";
            members[0].type = orb.create_sequence_tc(0, orb.get_primitive_tc(org.omg.CORBA.TCKind.tk_octet));

            members[1] = new org.omg.CORBA.StructMember();
            members[1].name = "byte_order";
            members[1].type = orb.get_primitive_tc(org.omg.CORBA.TCKind.tk_boolean);

            typeCode_ = orb.create_struct_tc(id(), "MessageBody", members);
        }

        return typeCode_;
    }

    public static String
    id()
    {
        return "IDL:omg.org/MessageRouting/MessageBody:1.0";
    }

    public static MessageBody
    read(org.omg.CORBA.portable.InputStream in)
    {
        MessageBody _ob_v = new MessageBody();
        int len0 = in.read_ulong();
        _ob_v.body = new byte[len0];
        in.read_octet_array(_ob_v.body, 0, len0);
        _ob_v.byte_order = in.read_boolean();
        return _ob_v;
    }

    public static void
    write(org.omg.CORBA.portable.OutputStream out, MessageBody val)
    {
        int len0 = val.body.length;
        out.write_ulong(len0);
        out.write_octet_array(val.body, 0, len0);
        out.write_boolean(val.byte_order);
    }
}
