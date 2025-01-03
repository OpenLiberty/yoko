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
package org.apache.yoko.orb.IMR;

import org.omg.CORBA.ORB;
import org.omg.CORBA.portable.InputStream;
import org.omg.CORBA.portable.InvokeHandler;
import org.omg.CORBA.portable.OutputStream;
import org.omg.CORBA.portable.ResponseHandler;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.Servant;

//
// IDL:orb.yoko.apache.org/IMR/ServerFactory:1.0
//
public abstract class ServerFactoryPOA
    extends Servant
    implements InvokeHandler,
               ServerFactoryOperations
{
    static final String[] _ob_ids_ =
    {
        "IDL:orb.yoko.apache.org/IMR/ServerFactory:1.0",
    };

    public ServerFactory
    _this()
    {
        return ServerFactoryHelper.narrow(super._this_object());
    }

    public ServerFactory
    _this(ORB orb)
    {
        return ServerFactoryHelper.narrow(super._this_object(orb));
    }

    public String[]
    _all_interfaces(POA poa, byte[] objectId)
    {
        return _ob_ids_;
    }

    public OutputStream
    _invoke(String opName,
            InputStream in,
            ResponseHandler handler)
    {
        final String[] _ob_names =
        {
            "create_server_record",
            "get_server",
            "list_orphaned_servers",
            "list_servers",
            "list_servers_by_host"
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
        case 0: // create_server_record
            return _OB_op_create_server_record(in, handler);

        case 1: // get_server
            return _OB_op_get_server(in, handler);

        case 2: // list_orphaned_servers
            return _OB_op_list_orphaned_servers(in, handler);

        case 3: // list_servers
            return _OB_op_list_servers(in, handler);

        case 4: // list_servers_by_host
            return _OB_op_list_servers_by_host(in, handler);
        }

        throw new org.omg.CORBA.BAD_OPERATION();
    }

    private OutputStream
    _OB_op_create_server_record(InputStream in,
                                ResponseHandler handler)
    {
        OutputStream out = null;
        try
        {
            String _ob_a0 = in.read_string();
            Server _ob_r = create_server_record(_ob_a0);
            out = handler.createReply();
            ServerHelper.write(out, _ob_r);
        }
        catch(ServerAlreadyRegistered _ob_ex)
        {
            out = handler.createExceptionReply();
            ServerAlreadyRegisteredHelper.write(out, _ob_ex);
        }
        return out;
    }

    private OutputStream
    _OB_op_get_server(InputStream in,
                      ResponseHandler handler)
    {
        OutputStream out = null;
        try
        {
            String _ob_a0 = in.read_string();
            Server _ob_r = get_server(_ob_a0);
            out = handler.createReply();
            ServerHelper.write(out, _ob_r);
        }
        catch(NoSuchServer _ob_ex)
        {
            out = handler.createExceptionReply();
            NoSuchServerHelper.write(out, _ob_ex);
        }
        return out;
    }

    private OutputStream
    _OB_op_list_orphaned_servers(InputStream in,
                                 ResponseHandler handler)
    {
        OutputStream out = null;
        Server[] _ob_r = list_orphaned_servers();
        out = handler.createReply();
        ServerSeqHelper.write(out, _ob_r);
        return out;
    }

    private OutputStream
    _OB_op_list_servers(InputStream in,
                        ResponseHandler handler)
    {
        OutputStream out = null;
        Server[] _ob_r = list_servers();
        out = handler.createReply();
        ServerSeqHelper.write(out, _ob_r);
        return out;
    }

    private OutputStream
    _OB_op_list_servers_by_host(InputStream in,
                                ResponseHandler handler)
    {
        OutputStream out = null;
        String _ob_a0 = in.read_string();
        Server[] _ob_r = list_servers_by_host(_ob_a0);
        out = handler.createReply();
        ServerSeqHelper.write(out, _ob_r);
        return out;
    }
}
