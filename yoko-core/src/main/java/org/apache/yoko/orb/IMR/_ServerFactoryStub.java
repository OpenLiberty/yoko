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

import org.omg.CORBA.UNKNOWN;
import org.omg.CORBA.portable.ApplicationException;
import org.omg.CORBA.portable.InputStream;
import org.omg.CORBA.portable.ObjectImpl;
import org.omg.CORBA.portable.OutputStream;
import org.omg.CORBA.portable.RemarshalException;
import org.omg.CORBA.portable.ServantObject;

//
// IDL:orb.yoko.apache.org/IMR/ServerFactory:1.0
//
public class _ServerFactoryStub extends ObjectImpl
                                implements ServerFactory
{
    private static final String[] _ob_ids_ =
    {
        "IDL:orb.yoko.apache.org/IMR/ServerFactory:1.0",
    };

    public String[]
    _ids()
    {
        return _ob_ids_;
    }

    final public static Class _ob_opsClass = ServerFactoryOperations.class;

    //
    // IDL:orb.yoko.apache.org/IMR/ServerFactory/get_server:1.0
    //
    public Server
    get_server(String _ob_a0)
        throws NoSuchServer
    {
        while(true)
        {
            if(!this._is_local())
            {
                OutputStream out = null;
                InputStream in = null;
                try
                {
                    out = _request("get_server", true);
                    out.write_string(_ob_a0);
                    in = _invoke(out);
                    Server _ob_r = ServerHelper.read(in);
                    return _ob_r;
                }
                catch(RemarshalException _ob_ex)
                {
                    continue;
                }
                catch(ApplicationException _ob_aex)
                {
                    final String _ob_id = _ob_aex.getId();
                    in = _ob_aex.getInputStream();

                    if(_ob_id.equals(NoSuchServerHelper.id()))
                        throw NoSuchServerHelper.read(in);
                    throw (UNKNOWN)new 
                        UNKNOWN("Unexpected User Exception: " + _ob_id).initCause(_ob_aex);
                }
                finally
                {
                    _releaseReply(in);
                }
            }
            else
            {
                ServantObject _ob_so = _servant_preinvoke("get_server", _ob_opsClass);
                if(_ob_so == null)
                    continue;
                ServerFactoryOperations _ob_self = (ServerFactoryOperations)_ob_so.servant;
                try
                {
                    return _ob_self.get_server(_ob_a0);
                }
                finally
                {
                    _servant_postinvoke(_ob_so);
                }
            }
        }
    }

    //
    // IDL:orb.yoko.apache.org/IMR/ServerFactory/create_server_record:1.0
    //
    public Server
    create_server_record(String _ob_a0)
        throws ServerAlreadyRegistered
    {
        while(true)
        {
            if(!this._is_local())
            {
                OutputStream out = null;
                InputStream in = null;
                try
                {
                    out = _request("create_server_record", true);
                    out.write_string(_ob_a0);
                    in = _invoke(out);
                    Server _ob_r = ServerHelper.read(in);
                    return _ob_r;
                }
                catch(RemarshalException _ob_ex)
                {
                    continue;
                }
                catch(ApplicationException _ob_aex)
                {
                    final String _ob_id = _ob_aex.getId();
                    in = _ob_aex.getInputStream();

                    if(_ob_id.equals(ServerAlreadyRegisteredHelper.id()))
                        throw ServerAlreadyRegisteredHelper.read(in);
                    throw (UNKNOWN)new 
                        UNKNOWN("Unexpected User Exception: " + _ob_id).initCause(_ob_aex);
                }
                finally
                {
                    _releaseReply(in);
                }
            }
            else
            {
                ServantObject _ob_so = _servant_preinvoke("create_server_record", _ob_opsClass);
                if(_ob_so == null)
                    continue;
                ServerFactoryOperations _ob_self = (ServerFactoryOperations)_ob_so.servant;
                try
                {
                    return _ob_self.create_server_record(_ob_a0);
                }
                finally
                {
                    _servant_postinvoke(_ob_so);
                }
            }
        }
    }

    //
    // IDL:orb.yoko.apache.org/IMR/ServerFactory/list_servers:1.0
    //
    public Server[]
    list_servers()
    {
        while(true)
        {
            if(!this._is_local())
            {
                OutputStream out = null;
                InputStream in = null;
                try
                {
                    out = _request("list_servers", true);
                    in = _invoke(out);
                    Server[] _ob_r = ServerSeqHelper.read(in);
                    return _ob_r;
                }
                catch(RemarshalException _ob_ex)
                {
                    continue;
                }
                catch(ApplicationException _ob_aex)
                {
                    final String _ob_id = _ob_aex.getId();
                    in = _ob_aex.getInputStream();

                    throw (UNKNOWN)new 
                        UNKNOWN("Unexpected User Exception: " + _ob_id).initCause(_ob_aex);
                }
                finally
                {
                    _releaseReply(in);
                }
            }
            else
            {
                ServantObject _ob_so = _servant_preinvoke("list_servers", _ob_opsClass);
                if(_ob_so == null)
                    continue;
                ServerFactoryOperations _ob_self = (ServerFactoryOperations)_ob_so.servant;
                try
                {
                    return _ob_self.list_servers();
                }
                finally
                {
                    _servant_postinvoke(_ob_so);
                }
            }
        }
    }

    //
    // IDL:orb.yoko.apache.org/IMR/ServerFactory/list_servers_by_host:1.0
    //
    public Server[]
    list_servers_by_host(String _ob_a0)
    {
        while(true)
        {
            if(!this._is_local())
            {
                OutputStream out = null;
                InputStream in = null;
                try
                {
                    out = _request("list_servers_by_host", true);
                    out.write_string(_ob_a0);
                    in = _invoke(out);
                    Server[] _ob_r = ServerSeqHelper.read(in);
                    return _ob_r;
                }
                catch(RemarshalException _ob_ex)
                {
                    continue;
                }
                catch(ApplicationException _ob_aex)
                {
                    final String _ob_id = _ob_aex.getId();
                    in = _ob_aex.getInputStream();

                    throw (UNKNOWN)new 
                        UNKNOWN("Unexpected User Exception: " + _ob_id).initCause(_ob_aex);
                }
                finally
                {
                    _releaseReply(in);
                }
            }
            else
            {
                ServantObject _ob_so = _servant_preinvoke("list_servers_by_host", _ob_opsClass);
                if(_ob_so == null)
                    continue;
                ServerFactoryOperations _ob_self = (ServerFactoryOperations)_ob_so.servant;
                try
                {
                    return _ob_self.list_servers_by_host(_ob_a0);
                }
                finally
                {
                    _servant_postinvoke(_ob_so);
                }
            }
        }
    }

    //
    // IDL:orb.yoko.apache.org/IMR/ServerFactory/list_orphaned_servers:1.0
    //
    public Server[]
    list_orphaned_servers()
    {
        while(true)
        {
            if(!this._is_local())
            {
                OutputStream out = null;
                InputStream in = null;
                try
                {
                    out = _request("list_orphaned_servers", true);
                    in = _invoke(out);
                    Server[] _ob_r = ServerSeqHelper.read(in);
                    return _ob_r;
                }
                catch(RemarshalException _ob_ex)
                {
                    continue;
                }
                catch(ApplicationException _ob_aex)
                {
                    final String _ob_id = _ob_aex.getId();
                    in = _ob_aex.getInputStream();

                    throw (UNKNOWN)new 
                        UNKNOWN("Unexpected User Exception: " + _ob_id).initCause(_ob_aex);
                }
                finally
                {
                    _releaseReply(in);
                }
            }
            else
            {
                ServantObject _ob_so = _servant_preinvoke("list_orphaned_servers", _ob_opsClass);
                if(_ob_so == null)
                    continue;
                ServerFactoryOperations _ob_self = (ServerFactoryOperations)_ob_so.servant;
                try
                {
                    return _ob_self.list_orphaned_servers();
                }
                finally
                {
                    _servant_postinvoke(_ob_so);
                }
            }
        }
    }
}
