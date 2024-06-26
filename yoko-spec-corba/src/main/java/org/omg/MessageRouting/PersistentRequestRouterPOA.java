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
// IDL:omg.org/MessageRouting/PersistentRequestRouter:1.0
//
public abstract class PersistentRequestRouterPOA
    extends org.omg.PortableServer.Servant
    implements org.omg.CORBA.portable.InvokeHandler,
               PersistentRequestRouterOperations
{
    static final String[] _ob_ids_ =
    {
        "IDL:omg.org/MessageRouting/PersistentRequestRouter:1.0",
    };

    public PersistentRequestRouter
    _this()
    {
        return PersistentRequestRouterHelper.narrow(super._this_object());
    }

    public PersistentRequestRouter
    _this(org.omg.CORBA.ORB orb)
    {
        return PersistentRequestRouterHelper.narrow(super._this_object(orb));
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
        final String[] _ob_names =
        {
            "create_persistent_request"
        };

        int _ob_left = 0;
        int _ob_right = _ob_names.length;
        int _ob_index = -1;

        while(_ob_left < _ob_right)
        {
            int _ob_m = (_ob_left + _ob_right) / 2;
            int _ob_res = _ob_names[_ob_m].compareTo(opName);
            if(_ob_res == 0)
            {
                _ob_index = _ob_m;
                break;
            }
            else if(_ob_res > 0)
                _ob_right = _ob_m;
            else
                _ob_left = _ob_m + 1;
        }

        if(_ob_index == -1 && opName.charAt(0) == '_')
        {
            _ob_left = 0;
            _ob_right = _ob_names.length;
            String _ob_ami_op =
                opName.substring(1);

            while(_ob_left < _ob_right)
            {
                int _ob_m = (_ob_left + _ob_right) / 2;
                int _ob_res = _ob_names[_ob_m].compareTo(_ob_ami_op);
                if(_ob_res == 0)
                {
                    _ob_index = _ob_m;
                    break;
                }
                else if(_ob_res > 0)
                    _ob_right = _ob_m;
                else
                    _ob_left = _ob_m + 1;
            }
        }

        switch(_ob_index)
        {
        case 0: // create_persistent_request
            return _OB_op_create_persistent_request(in, handler);
        }

        throw new org.omg.CORBA.BAD_OPERATION();
    }

    private org.omg.CORBA.portable.OutputStream
    _OB_op_create_persistent_request(org.omg.CORBA.portable.InputStream in,
                                     org.omg.CORBA.portable.ResponseHandler handler)
    {
        org.omg.CORBA.portable.OutputStream out = null;
        short _ob_a0 = in.read_ushort();
        Router[] _ob_a1 = RouterListHelper.read(in);
        org.omg.CORBA.Object _ob_a2 = in.read_Object();
        org.omg.CORBA.Policy[] _ob_a3 = org.omg.CORBA.PolicyListHelper.read(in);
        RequestMessage _ob_a4 = RequestMessageHelper.read(in);
        PersistentRequest _ob_r = create_persistent_request(_ob_a0, _ob_a1, _ob_a2, _ob_a3, _ob_a4);
        out = handler.createReply();
        PersistentRequestHelper.write(out, _ob_r);
        return out;
    }
}
