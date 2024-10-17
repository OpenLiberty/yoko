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

import org.apache.yoko.orb.OAD.ProcessEndpointManagerHelper;
import org.apache.yoko.orb.OAD.ProcessEndpointManagerHolder;
import org.omg.CORBA.UNKNOWN;
import org.omg.CORBA.portable.ApplicationException;
import org.omg.CORBA.portable.InputStream;
import org.omg.CORBA.portable.ObjectImpl;
import org.omg.CORBA.portable.OutputStream;
import org.omg.CORBA.portable.RemarshalException;
import org.omg.CORBA.portable.ServantObject;
import org.omg.PortableInterceptor.ObjectReferenceTemplate;
import org.omg.PortableInterceptor.ObjectReferenceTemplateHelper;

//
// IDL:orb.yoko.apache.org/IMR/Domain:1.0
//
public class _DomainStub extends ObjectImpl
                         implements Domain
{
    private static final String[] _ob_ids_ =
    {
        "IDL:orb.yoko.apache.org/IMR/Domain:1.0",
    };

    public String[]
    _ids()
    {
        return _ob_ids_;
    }

    final public static Class _ob_opsClass = DomainOperations.class;

    //
    // IDL:orb.yoko.apache.org/IMR/Domain/registerServer:1.0
    //
    public void
    registerServer(String _ob_a0,
                   String _ob_a1,
                   String _ob_a2)
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
                    out = _request("registerServer", true);
                    out.write_string(_ob_a0);
                    out.write_string(_ob_a1);
                    out.write_string(_ob_a2);
                    in = _invoke(out);
                    return;
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
                ServantObject _ob_so = _servant_preinvoke("registerServer", _ob_opsClass);
                if(_ob_so == null)
                    continue;
                DomainOperations _ob_self = (DomainOperations)_ob_so.servant;
                try
                {
                    _ob_self.registerServer(_ob_a0, _ob_a1, _ob_a2);
                    return;
                }
                finally
                {
                    _servant_postinvoke(_ob_so);
                }
            }
        }
    }

    //
    // IDL:orb.yoko.apache.org/IMR/Domain/startup:1.0
    //
    public ActiveState
    startup(String _ob_a0,
            String _ob_a1,
            ObjectReferenceTemplate _ob_a2,
            ProcessEndpointManagerHolder _ob_ah3)
        throws NoSuchServer,
               NoSuchOAD,
               OADNotRunning
    {
        while(true)
        {
            if(!this._is_local())
            {
                OutputStream out = null;
                InputStream in = null;
                try
                {
                    out = _request("startup", true);
                    out.write_string(_ob_a0);
                    out.write_string(_ob_a1);
                    ObjectReferenceTemplateHelper.write(out, _ob_a2);
                    in = _invoke(out);
                    ActiveState _ob_r = ActiveStateHelper.read(in);
                    _ob_ah3.value = ProcessEndpointManagerHelper.read(in);
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
                    if(_ob_id.equals(NoSuchOADHelper.id()))
                        throw NoSuchOADHelper.read(in);
                    if(_ob_id.equals(OADNotRunningHelper.id()))
                        throw OADNotRunningHelper.read(in);
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
                ServantObject _ob_so = _servant_preinvoke("startup", _ob_opsClass);
                if(_ob_so == null)
                    continue;
                DomainOperations _ob_self = (DomainOperations)_ob_so.servant;
                try
                {
                    OutputStream _ob_out = _orb().create_output_stream();
                    ObjectReferenceTemplateHelper.write(_ob_out, _ob_a2);
                    InputStream _ob_in = _ob_out.create_input_stream();
                    _ob_a2 = ObjectReferenceTemplateHelper.read(_ob_in);
                    return _ob_self.startup(_ob_a0, _ob_a1, _ob_a2, _ob_ah3);
                }
                finally
                {
                    _servant_postinvoke(_ob_so);
                }
            }
        }
    }
}
