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
// IDL:omg.org/Messaging/ReplyHandler:1.0
//
public abstract class ReplyHandlerPOA
    extends org.omg.PortableServer.Servant
    implements org.omg.CORBA.portable.InvokeHandler,
               ReplyHandlerOperations
{
    static final String[] _ob_ids_ =
    {
        "IDL:omg.org/Messaging/ReplyHandler:1.0",
    };

    public ReplyHandler
    _this()
    {
        return ReplyHandlerHelper.narrow(super._this_object());
    }

    public ReplyHandler
    _this(org.omg.CORBA.ORB orb)
    {
        return ReplyHandlerHelper.narrow(super._this_object(orb));
    }

    public String[]
    _all_interfaces(org.omg.PortableServer.POA poa, byte[] objectId)
    {
        return _ob_ids_;
    }

    public org.omg.CORBA.portable.OutputStream
    _invoke(String opName,
            org.omg.CORBA.portable.InputStream in,
            org.omg.CORBA.portable.ResponseHandler handler)
    {
        throw new org.omg.CORBA.BAD_OPERATION();
    }
}
