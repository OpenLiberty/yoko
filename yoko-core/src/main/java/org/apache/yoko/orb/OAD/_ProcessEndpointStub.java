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
package org.apache.yoko.orb.OAD;

import org.omg.CORBA.UNKNOWN;
import org.omg.CORBA.portable.ApplicationException;
import org.omg.CORBA.portable.InputStream;
import org.omg.CORBA.portable.ObjectImpl;
import org.omg.CORBA.portable.OutputStream;
import org.omg.CORBA.portable.RemarshalException;
import org.omg.CORBA.portable.ServantObject;

//
// IDL:orb.yoko.apache.org/OAD/ProcessEndpoint:1.0
//
public class _ProcessEndpointStub extends ObjectImpl
                                  implements ProcessEndpoint
{
    private static final String[] _ob_ids_ =
    {
        "IDL:orb.yoko.apache.org/OAD/ProcessEndpoint:1.0",
    };

    public String[]
    _ids()
    {
        return _ob_ids_;
    }

    final public static Class _ob_opsClass = ProcessEndpointOperations.class;

    //
    // IDL:orb.yoko.apache.org/OAD/ProcessEndpoint/reestablish_link:1.0
    //
    public void
    reestablish_link(ProcessEndpointManager _ob_a0)
    {
        while(true)
        {
            if(!this._is_local())
            {
                OutputStream out = null;
                InputStream in = null;
                try
                {
                    out = _request("reestablish_link", false);
                    ProcessEndpointManagerHelper.write(out, _ob_a0);
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
                ServantObject _ob_so = _servant_preinvoke("reestablish_link", _ob_opsClass);
                if(_ob_so == null)
                    continue;
                ProcessEndpointOperations _ob_self = (ProcessEndpointOperations)_ob_so.servant;
                try
                {
                    _ob_self.reestablish_link(_ob_a0);
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
    // IDL:orb.yoko.apache.org/OAD/ProcessEndpoint/stop:1.0
    //
    public void
    stop()
    {
        while(true)
        {
            if(!this._is_local())
            {
                OutputStream out = null;
                InputStream in = null;
                try
                {
                    out = _request("stop", true);
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
                ServantObject _ob_so = _servant_preinvoke("stop", _ob_opsClass);
                if(_ob_so == null)
                    continue;
                ProcessEndpointOperations _ob_self = (ProcessEndpointOperations)_ob_so.servant;
                try
                {
                    _ob_self.stop();
                    return;
                }
                finally
                {
                    _servant_postinvoke(_ob_so);
                }
            }
        }
    }
}
