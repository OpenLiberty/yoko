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
// IDL:omg.org/MessageRouting/RequestInfo:1.0
//
final public class RequestInfoHelper
{
    public static void
    insert(org.omg.CORBA.Any any, RequestInfo val)
    {
        org.omg.CORBA.portable.OutputStream out = any.create_output_stream();
        write(out, val);
        any.read_value(out.create_input_stream(), type());
    }

    public static RequestInfo
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
            org.omg.CORBA.StructMember[] members = new org.omg.CORBA.StructMember[7];

            members[0] = new org.omg.CORBA.StructMember();
            members[0].name = "visited";
            members[0].type = RouterListHelper.type();

            members[1] = new org.omg.CORBA.StructMember();
            members[1].name = "to_visit";
            members[1].type = RouterListHelper.type();

            members[2] = new org.omg.CORBA.StructMember();
            members[2].name = "target";
            members[2].type = orb.create_interface_tc("IDL:omg.org/CORBA/Object:1.0", "Object");

            members[3] = new org.omg.CORBA.StructMember();
            members[3].name = "profile_index";
            members[3].type = orb.get_primitive_tc(org.omg.CORBA.TCKind.tk_ushort);

            members[4] = new org.omg.CORBA.StructMember();
            members[4].name = "reply_destination";
            members[4].type = ReplyDestinationHelper.type();

            members[5] = new org.omg.CORBA.StructMember();
            members[5].name = "selected_qos";
            members[5].type = org.omg.Messaging.PolicyValueSeqHelper.type();

            members[6] = new org.omg.CORBA.StructMember();
            members[6].name = "payload";
            members[6].type = RequestMessageHelper.type();

            typeCode_ = orb.create_struct_tc(id(), "RequestInfo", members);
        }

        return typeCode_;
    }

    public static String
    id()
    {
        return "IDL:omg.org/MessageRouting/RequestInfo:1.0";
    }

    public static RequestInfo
    read(org.omg.CORBA.portable.InputStream in)
    {
        RequestInfo _ob_v = new RequestInfo();
        _ob_v.visited = RouterListHelper.read(in);
        _ob_v.to_visit = RouterListHelper.read(in);
        _ob_v.target = in.read_Object();
        _ob_v.profile_index = in.read_ushort();
        _ob_v.reply_destination = ReplyDestinationHelper.read(in);
        _ob_v.selected_qos = org.omg.Messaging.PolicyValueSeqHelper.read(in);
        _ob_v.payload = RequestMessageHelper.read(in);
        return _ob_v;
    }

    public static void
    write(org.omg.CORBA.portable.OutputStream out, RequestInfo val)
    {
        RouterListHelper.write(out, val.visited);
        RouterListHelper.write(out, val.to_visit);
        out.write_Object(val.target);
        out.write_ushort(val.profile_index);
        ReplyDestinationHelper.write(out, val.reply_destination);
        org.omg.Messaging.PolicyValueSeqHelper.write(out, val.selected_qos);
        RequestMessageHelper.write(out, val.payload);
    }
}
