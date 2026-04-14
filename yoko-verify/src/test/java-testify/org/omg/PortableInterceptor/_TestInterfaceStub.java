/*
 * Copyright 2026 IBM Corporation and others.
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
package org.omg.PortableInterceptor;

import org.omg.PortableInterceptor.TestInterfacePackage.s;
import org.omg.PortableInterceptor.TestInterfacePackage.sHelper;
import org.omg.PortableInterceptor.TestInterfacePackage.sHolder;
import org.omg.PortableInterceptor.TestInterfacePackage.user;
import org.omg.PortableInterceptor.TestInterfacePackage.userHelper;

//
// IDL:TestInterface:1.0
//
public class _TestInterfaceStub extends org.apache.yoko.orb.CORBA.ObjectImpl
                                implements TestInterface
{
    private static final String[] _ob_ids_ =
    {
        "IDL:TestInterface:1.0",
    };

    public String[]
    _ids()
    {
        return _ob_ids_;
    }

    final public static java.lang.Class _ob_opsClass = TestInterfaceOperations.class;

    //
    // IDL:TestInterface/string_attrib:1.0
    //
    public String
    string_attrib()
    {
        org.omg.CORBA.StringHolder _ob_rh = new org.omg.CORBA.StringHolder();
        org.apache.yoko.orb.OB.ParameterDesc _ob_retDesc = new org.apache.yoko.orb.OB.ParameterDesc(_ob_rh, _orb().get_primitive_tc(org.omg.CORBA.TCKind.tk_string), 0);

        org.apache.yoko.orb.CORBA.RetryInfo _ob_info = new org.apache.yoko.orb.CORBA.RetryInfo();
        while(true)
        {
            try
            {
                while(true)
                {
                    if(!this._is_local())
                    {
                        org.apache.yoko.orb.OB.DowncallStub _ob_stub = _OB_getDowncallStub();
                        org.apache.yoko.orb.OB.Downcall _ob_down = _ob_stub.createPIArgsDowncall("_get_string_attrib", true, null, _ob_retDesc, null);
                        try
                        {
                            _ob_stub.preMarshal(_ob_down);
                            _ob_stub.postMarshal(_ob_down);
                            _ob_stub.request(_ob_down);
                            org.omg.CORBA.BooleanHolder _ob_uex = new org.omg.CORBA.BooleanHolder();
                            org.omg.CORBA.portable.InputStream in = _ob_stub.preUnmarshal(_ob_down, _ob_uex);
                            if(_ob_uex.value)
                            {
                                _ob_stub.postUnmarshal(_ob_down);
                            }
                            else
                            {
                                try
                                {
                                    _ob_rh.value = in.read_string();
                                }
                                catch(org.omg.CORBA.SystemException _ob_ex)
                                {
                                    _ob_stub.unmarshalEx(_ob_down, _ob_ex);
                                }
                                _ob_stub.postUnmarshal(_ob_down);
                                return _ob_rh.value;
                            }
                        }
                        catch(org.apache.yoko.orb.OB.FailureException _ob_ex)
                        {
                            _ob_stub.handleFailureException(_ob_down, _ob_ex);
                        }
                    }
                    else
                    {
                        org.omg.CORBA.portable.ServantObject _ob_so = _servant_preinvoke("string_attrib", _ob_opsClass);
                        if(_ob_so == null)
                            continue;
                        TestInterfaceOperations _ob_self = (TestInterfaceOperations)_ob_so.servant;
                        try
                        {
                            return _ob_self.string_attrib();
                        }
                        finally
                        {
                            _servant_postinvoke(_ob_so);
                        }
                    }
                }
            }
            catch(Exception _ob_ex)
            {
                _OB_handleException(_ob_ex, _ob_info);
            }
        }
    }

    public void
    string_attrib(String _ob_a)
    {
        org.omg.CORBA.StringHolder _ob_ah = new org.omg.CORBA.StringHolder(_ob_a);
        org.apache.yoko.orb.OB.ParameterDesc[] _ob_desc =
        {
            new org.apache.yoko.orb.OB.ParameterDesc(_ob_ah, _orb().get_primitive_tc(org.omg.CORBA.TCKind.tk_string), 0)
        };

        org.apache.yoko.orb.CORBA.RetryInfo _ob_info = new org.apache.yoko.orb.CORBA.RetryInfo();
        while(true)
        {
            try
            {
                while(true)
                {
                    if(!this._is_local())
                    {
                        org.apache.yoko.orb.OB.DowncallStub _ob_stub = _OB_getDowncallStub();
                        org.apache.yoko.orb.OB.Downcall _ob_down = _ob_stub.createPIArgsDowncall("_set_string_attrib", true, _ob_desc, null, null);
                        try
                        {
                            org.omg.CORBA.portable.OutputStream out = _ob_stub.preMarshal(_ob_down);
                            try
                            {
                                out.write_string(_ob_ah.value);
                            }
                            catch(org.omg.CORBA.SystemException _ob_ex)
                            {
                                _ob_stub.marshalEx(_ob_down, _ob_ex);
                            }
                            _ob_stub.postMarshal(_ob_down);
                            _ob_stub.request(_ob_down);
                            _ob_stub.preUnmarshal(_ob_down);
                            _ob_stub.postUnmarshal(_ob_down);
                            return;
                        }
                        catch(org.apache.yoko.orb.OB.FailureException _ob_ex)
                        {
                            _ob_stub.handleFailureException(_ob_down, _ob_ex);
                        }
                    }
                    else
                    {
                        org.omg.CORBA.portable.ServantObject _ob_so = _servant_preinvoke("string_attrib", _ob_opsClass);
                        if(_ob_so == null)
                            continue;
                        TestInterfaceOperations _ob_self = (TestInterfaceOperations)_ob_so.servant;
                        try
                        {
                            _ob_self.string_attrib(_ob_a);
                            return;
                        }
                        finally
                        {
                            _servant_postinvoke(_ob_so);
                        }
                    }
                }
            }
            catch(Exception _ob_ex)
            {
                _OB_handleException(_ob_ex, _ob_info);
            }
        }
    }

    //
    // IDL:TestInterface/struct_attrib:1.0
    //
    public s
    struct_attrib()
    {
        sHolder _ob_rh = new sHolder();
        org.apache.yoko.orb.OB.ParameterDesc _ob_retDesc = new org.apache.yoko.orb.OB.ParameterDesc(_ob_rh, sHelper.type(), 0);

        org.apache.yoko.orb.CORBA.RetryInfo _ob_info = new org.apache.yoko.orb.CORBA.RetryInfo();
        while(true)
        {
            try
            {
                while(true)
                {
                    if(!this._is_local())
                    {
                        org.apache.yoko.orb.OB.DowncallStub _ob_stub = _OB_getDowncallStub();
                        org.apache.yoko.orb.OB.Downcall _ob_down = _ob_stub.createPIArgsDowncall("_get_struct_attrib", true, null, _ob_retDesc, null);
                        try
                        {
                            _ob_stub.preMarshal(_ob_down);
                            _ob_stub.postMarshal(_ob_down);
                            _ob_stub.request(_ob_down);
                            org.omg.CORBA.BooleanHolder _ob_uex = new org.omg.CORBA.BooleanHolder();
                            org.omg.CORBA.portable.InputStream in = _ob_stub.preUnmarshal(_ob_down, _ob_uex);
                            if(_ob_uex.value)
                            {
                                _ob_stub.postUnmarshal(_ob_down);
                            }
                            else
                            {
                                try
                                {
                                    _ob_rh.value = sHelper.read(in);
                                }
                                catch(org.omg.CORBA.SystemException _ob_ex)
                                {
                                    _ob_stub.unmarshalEx(_ob_down, _ob_ex);
                                }
                                _ob_stub.postUnmarshal(_ob_down);
                                return _ob_rh.value;
                            }
                        }
                        catch(org.apache.yoko.orb.OB.FailureException _ob_ex)
                        {
                            _ob_stub.handleFailureException(_ob_down, _ob_ex);
                        }
                    }
                    else
                    {
                        org.omg.CORBA.portable.ServantObject _ob_so = _servant_preinvoke("struct_attrib", _ob_opsClass);
                        if(_ob_so == null)
                            continue;
                        TestInterfaceOperations _ob_self = (TestInterfaceOperations)_ob_so.servant;
                        try
                        {
                            return _ob_self.struct_attrib();
                        }
                        finally
                        {
                            _servant_postinvoke(_ob_so);
                        }
                    }
                }
            }
            catch(Exception _ob_ex)
            {
                _OB_handleException(_ob_ex, _ob_info);
            }
        }
    }

    public void
    struct_attrib(s _ob_a)
    {
        sHolder _ob_ah = new sHolder(_ob_a);
        org.apache.yoko.orb.OB.ParameterDesc[] _ob_desc =
        {
            new org.apache.yoko.orb.OB.ParameterDesc(_ob_ah, sHelper.type(), 0)
        };

        org.apache.yoko.orb.CORBA.RetryInfo _ob_info = new org.apache.yoko.orb.CORBA.RetryInfo();
        while(true)
        {
            try
            {
                while(true)
                {
                    if(!this._is_local())
                    {
                        org.apache.yoko.orb.OB.DowncallStub _ob_stub = _OB_getDowncallStub();
                        org.apache.yoko.orb.OB.Downcall _ob_down = _ob_stub.createPIArgsDowncall("_set_struct_attrib", true, _ob_desc, null, null);
                        try
                        {
                            org.omg.CORBA.portable.OutputStream out = _ob_stub.preMarshal(_ob_down);
                            try
                            {
                                sHelper.write(out, _ob_ah.value);
                            }
                            catch(org.omg.CORBA.SystemException _ob_ex)
                            {
                                _ob_stub.marshalEx(_ob_down, _ob_ex);
                            }
                            _ob_stub.postMarshal(_ob_down);
                            _ob_stub.request(_ob_down);
                            _ob_stub.preUnmarshal(_ob_down);
                            _ob_stub.postUnmarshal(_ob_down);
                            return;
                        }
                        catch(org.apache.yoko.orb.OB.FailureException _ob_ex)
                        {
                            _ob_stub.handleFailureException(_ob_down, _ob_ex);
                        }
                    }
                    else
                    {
                        org.omg.CORBA.portable.ServantObject _ob_so = _servant_preinvoke("struct_attrib", _ob_opsClass);
                        if(_ob_so == null)
                            continue;
                        TestInterfaceOperations _ob_self = (TestInterfaceOperations)_ob_so.servant;
                        try
                        {
                            _ob_self.struct_attrib(_ob_a);
                            return;
                        }
                        finally
                        {
                            _servant_postinvoke(_ob_so);
                        }
                    }
                }
            }
            catch(Exception _ob_ex)
            {
                _OB_handleException(_ob_ex, _ob_info);
            }
        }
    }

    //
    // IDL:TestInterface/noargs:1.0
    //
    public void
    noargs()
    {
        org.apache.yoko.orb.CORBA.RetryInfo _ob_info = new org.apache.yoko.orb.CORBA.RetryInfo();
        while(true)
        {
            try
            {
                while(true)
                {
                    if(!this._is_local())
                    {
                        org.apache.yoko.orb.OB.DowncallStub _ob_stub = _OB_getDowncallStub();
                        org.apache.yoko.orb.OB.Downcall _ob_down = _ob_stub.createPIArgsDowncall("noargs", true, null, null, null);
                        try
                        {
                            _ob_stub.preMarshal(_ob_down);
                            _ob_stub.postMarshal(_ob_down);
                            _ob_stub.request(_ob_down);
                            _ob_stub.preUnmarshal(_ob_down);
                            _ob_stub.postUnmarshal(_ob_down);
                            return;
                        }
                        catch(org.apache.yoko.orb.OB.FailureException _ob_ex)
                        {
                            _ob_stub.handleFailureException(_ob_down, _ob_ex);
                        }
                    }
                    else
                    {
                        org.omg.CORBA.portable.ServantObject _ob_so = _servant_preinvoke("noargs", _ob_opsClass);
                        if(_ob_so == null)
                            continue;
                        TestInterfaceOperations _ob_self = (TestInterfaceOperations)_ob_so.servant;
                        try
                        {
                            _ob_self.noargs();
                            return;
                        }
                        finally
                        {
                            _servant_postinvoke(_ob_so);
                        }
                    }
                }
            }
            catch(Exception _ob_ex)
            {
                _OB_handleException(_ob_ex, _ob_info);
            }
        }
    }

    //
    // IDL:TestInterface/noargs_oneway:1.0
    //
    public void
    noargs_oneway()
    {
        org.apache.yoko.orb.CORBA.RetryInfo _ob_info = new org.apache.yoko.orb.CORBA.RetryInfo();
        while(true)
        {
            try
            {
                while(true)
                {
                    if(!this._is_local())
                    {
                        org.apache.yoko.orb.OB.DowncallStub _ob_stub = _OB_getDowncallStub();
                        org.apache.yoko.orb.OB.Downcall _ob_down = _ob_stub.createPIArgsDowncall("noargs_oneway", false, null, null, null);
                        try
                        {
                            _ob_stub.preMarshal(_ob_down);
                            _ob_stub.postMarshal(_ob_down);
                            _ob_stub.oneway(_ob_down);
                            _ob_stub.preUnmarshal(_ob_down);
                            _ob_stub.postUnmarshal(_ob_down);
                            return;
                        }
                        catch(org.apache.yoko.orb.OB.FailureException _ob_ex)
                        {
                            _ob_stub.handleFailureException(_ob_down, _ob_ex);
                        }
                    }
                    else
                    {
                        org.omg.CORBA.portable.ServantObject _ob_so = _servant_preinvoke("noargs_oneway", _ob_opsClass);
                        if(_ob_so == null)
                            continue;
                        TestInterfaceOperations _ob_self = (TestInterfaceOperations)_ob_so.servant;
                        try
                        {
                            _ob_self.noargs_oneway();
                            return;
                        }
                        finally
                        {
                            _servant_postinvoke(_ob_so);
                        }
                    }
                }
            }
            catch(Exception _ob_ex)
            {
                _OB_handleException(_ob_ex, _ob_info);
            }
        }
    }

    //
    // IDL:TestInterface/systemexception:1.0
    //
    public void
    systemexception()
    {
        org.apache.yoko.orb.CORBA.RetryInfo _ob_info = new org.apache.yoko.orb.CORBA.RetryInfo();
        while(true)
        {
            try
            {
                while(true)
                {
                    if(!this._is_local())
                    {
                        org.apache.yoko.orb.OB.DowncallStub _ob_stub = _OB_getDowncallStub();
                        org.apache.yoko.orb.OB.Downcall _ob_down = _ob_stub.createPIArgsDowncall("systemexception", true, null, null, null);
                        try
                        {
                            _ob_stub.preMarshal(_ob_down);
                            _ob_stub.postMarshal(_ob_down);
                            _ob_stub.request(_ob_down);
                            _ob_stub.preUnmarshal(_ob_down);
                            _ob_stub.postUnmarshal(_ob_down);
                            return;
                        }
                        catch(org.apache.yoko.orb.OB.FailureException _ob_ex)
                        {
                            _ob_stub.handleFailureException(_ob_down, _ob_ex);
                        }
                    }
                    else
                    {
                        org.omg.CORBA.portable.ServantObject _ob_so = _servant_preinvoke("systemexception", _ob_opsClass);
                        if(_ob_so == null)
                            continue;
                        TestInterfaceOperations _ob_self = (TestInterfaceOperations)_ob_so.servant;
                        try
                        {
                            _ob_self.systemexception();
                            return;
                        }
                        finally
                        {
                            _servant_postinvoke(_ob_so);
                        }
                    }
                }
            }
            catch(Exception _ob_ex)
            {
                _OB_handleException(_ob_ex, _ob_info);
            }
        }
    }

    //
    // IDL:TestInterface/userexception:1.0
    //
    public void
    userexception()
        throws user
    {
        org.omg.CORBA.TypeCode[] _ob_exceptions =
        {
            userHelper.type()
        };

        org.apache.yoko.orb.CORBA.RetryInfo _ob_info = new org.apache.yoko.orb.CORBA.RetryInfo();
        while(true)
        {
            try
            {
                while(true)
                {
                    if(!this._is_local())
                    {
                        org.apache.yoko.orb.OB.DowncallStub _ob_stub = _OB_getDowncallStub();
                        org.apache.yoko.orb.OB.Downcall _ob_down = _ob_stub.createPIArgsDowncall("userexception", true, null, null, _ob_exceptions);
                        try
                        {
                            _ob_stub.preMarshal(_ob_down);
                            _ob_stub.postMarshal(_ob_down);
                            _ob_stub.request(_ob_down);
                            org.omg.CORBA.BooleanHolder _ob_uex = new org.omg.CORBA.BooleanHolder();
                            org.omg.CORBA.portable.InputStream in = _ob_stub.preUnmarshal(_ob_down, _ob_uex);
                            if(_ob_uex.value)
                            {
                                try
                                {
                                    String _ob_id = _ob_stub.unmarshalExceptionId(_ob_down);
                                    if(_ob_id.equals(userHelper.id()))
                                    {
                                        user _ob_ex = userHelper.read(in);
                                        _ob_stub.setUserException(_ob_down, _ob_ex, _ob_id);
                                        _ob_stub.postUnmarshal(_ob_down);
                                        throw _ob_ex;
                                    }
                                }
                                catch(org.omg.CORBA.SystemException _ob_ex)
                                {
                                    _ob_stub.unmarshalEx(_ob_down, _ob_ex);
                                }
                                _ob_stub.postUnmarshal(_ob_down);
                            }
                            else
                            {
                                _ob_stub.postUnmarshal(_ob_down);
                                return;
                            }
                        }
                        catch(org.apache.yoko.orb.OB.FailureException _ob_ex)
                        {
                            _ob_stub.handleFailureException(_ob_down, _ob_ex);
                        }
                    }
                    else
                    {
                        org.omg.CORBA.portable.ServantObject _ob_so = _servant_preinvoke("userexception", _ob_opsClass);
                        if(_ob_so == null)
                            continue;
                        TestInterfaceOperations _ob_self = (TestInterfaceOperations)_ob_so.servant;
                        try
                        {
                            _ob_self.userexception();
                            return;
                        }
                        finally
                        {
                            _servant_postinvoke(_ob_so);
                        }
                    }
                }
            }
            catch(user _ob_ex)
            {
                throw _ob_ex;
            }
            catch(Exception _ob_ex)
            {
                _OB_handleException(_ob_ex, _ob_info);
            }
        }
    }

    //
    // IDL:TestInterface/location_forward:1.0
    //
    public void
    location_forward()
    {
        org.apache.yoko.orb.CORBA.RetryInfo _ob_info = new org.apache.yoko.orb.CORBA.RetryInfo();
        while(true)
        {
            try
            {
                while(true)
                {
                    if(!this._is_local())
                    {
                        org.apache.yoko.orb.OB.DowncallStub _ob_stub = _OB_getDowncallStub();
                        org.apache.yoko.orb.OB.Downcall _ob_down = _ob_stub.createPIArgsDowncall("location_forward", true, null, null, null);
                        try
                        {
                            _ob_stub.preMarshal(_ob_down);
                            _ob_stub.postMarshal(_ob_down);
                            _ob_stub.request(_ob_down);
                            _ob_stub.preUnmarshal(_ob_down);
                            _ob_stub.postUnmarshal(_ob_down);
                            return;
                        }
                        catch(org.apache.yoko.orb.OB.FailureException _ob_ex)
                        {
                            _ob_stub.handleFailureException(_ob_down, _ob_ex);
                        }
                    }
                    else
                    {
                        org.omg.CORBA.portable.ServantObject _ob_so = _servant_preinvoke("location_forward", _ob_opsClass);
                        if(_ob_so == null)
                            continue;
                        TestInterfaceOperations _ob_self = (TestInterfaceOperations)_ob_so.servant;
                        try
                        {
                            _ob_self.location_forward();
                            return;
                        }
                        finally
                        {
                            _servant_postinvoke(_ob_so);
                        }
                    }
                }
            }
            catch(Exception _ob_ex)
            {
                _OB_handleException(_ob_ex, _ob_info);
            }
        }
    }

    //
    // IDL:TestInterface/test_service_context:1.0
    //
    public void
    test_service_context()
    {
        org.apache.yoko.orb.CORBA.RetryInfo _ob_info = new org.apache.yoko.orb.CORBA.RetryInfo();
        while(true)
        {
            try
            {
                while(true)
                {
                    if(!this._is_local())
                    {
                        org.apache.yoko.orb.OB.DowncallStub _ob_stub = _OB_getDowncallStub();
                        org.apache.yoko.orb.OB.Downcall _ob_down = _ob_stub.createPIArgsDowncall("test_service_context", true, null, null, null);
                        try
                        {
                            _ob_stub.preMarshal(_ob_down);
                            _ob_stub.postMarshal(_ob_down);
                            _ob_stub.request(_ob_down);
                            _ob_stub.preUnmarshal(_ob_down);
                            _ob_stub.postUnmarshal(_ob_down);
                            return;
                        }
                        catch(org.apache.yoko.orb.OB.FailureException _ob_ex)
                        {
                            _ob_stub.handleFailureException(_ob_down, _ob_ex);
                        }
                    }
                    else
                    {
                        org.omg.CORBA.portable.ServantObject _ob_so = _servant_preinvoke("test_service_context", _ob_opsClass);
                        if(_ob_so == null)
                            continue;
                        TestInterfaceOperations _ob_self = (TestInterfaceOperations)_ob_so.servant;
                        try
                        {
                            _ob_self.test_service_context();
                            return;
                        }
                        finally
                        {
                            _servant_postinvoke(_ob_so);
                        }
                    }
                }
            }
            catch(Exception _ob_ex)
            {
                _OB_handleException(_ob_ex, _ob_info);
            }
        }
    }

    //
    // IDL:TestInterface/one_string_in:1.0
    //
    public void
    one_string_in(String _ob_a0)
    {
        org.omg.CORBA.StringHolder _ob_ah0 = new org.omg.CORBA.StringHolder(_ob_a0);
        org.apache.yoko.orb.OB.ParameterDesc[] _ob_desc =
        {
            new org.apache.yoko.orb.OB.ParameterDesc(_ob_ah0, _orb().get_primitive_tc(org.omg.CORBA.TCKind.tk_string), 0 /*in*/)
        };

        org.apache.yoko.orb.CORBA.RetryInfo _ob_info = new org.apache.yoko.orb.CORBA.RetryInfo();
        while(true)
        {
            try
            {
                while(true)
                {
                    if(!this._is_local())
                    {
                        org.apache.yoko.orb.OB.DowncallStub _ob_stub = _OB_getDowncallStub();
                        org.apache.yoko.orb.OB.Downcall _ob_down = _ob_stub.createPIArgsDowncall("one_string_in", true, _ob_desc, null, null);
                        try
                        {
                            org.omg.CORBA.portable.OutputStream out = _ob_stub.preMarshal(_ob_down);
                            try
                            {
                                out.write_string(_ob_ah0.value);
                            }
                            catch(org.omg.CORBA.SystemException _ob_ex)
                            {
                                _ob_stub.marshalEx(_ob_down, _ob_ex);
                            }
                            _ob_stub.postMarshal(_ob_down);
                            _ob_stub.request(_ob_down);
                            _ob_stub.preUnmarshal(_ob_down);
                            _ob_stub.postUnmarshal(_ob_down);
                            return;
                        }
                        catch(org.apache.yoko.orb.OB.FailureException _ob_ex)
                        {
                            _ob_stub.handleFailureException(_ob_down, _ob_ex);
                        }
                    }
                    else
                    {
                        org.omg.CORBA.portable.ServantObject _ob_so = _servant_preinvoke("one_string_in", _ob_opsClass);
                        if(_ob_so == null)
                            continue;
                        TestInterfaceOperations _ob_self = (TestInterfaceOperations)_ob_so.servant;
                        try
                        {
                            _ob_self.one_string_in(_ob_a0);
                            return;
                        }
                        finally
                        {
                            _servant_postinvoke(_ob_so);
                        }
                    }
                }
            }
            catch(Exception _ob_ex)
            {
                _OB_handleException(_ob_ex, _ob_info);
            }
        }
    }

    //
    // IDL:TestInterface/one_string_inout:1.0
    //
    public void
    one_string_inout(org.omg.CORBA.StringHolder _ob_ah0)
    {
        org.apache.yoko.orb.OB.ParameterDesc[] _ob_desc =
        {
            new org.apache.yoko.orb.OB.ParameterDesc(_ob_ah0, _orb().get_primitive_tc(org.omg.CORBA.TCKind.tk_string), 2 /*inout*/)
        };

        org.apache.yoko.orb.CORBA.RetryInfo _ob_info = new org.apache.yoko.orb.CORBA.RetryInfo();
        while(true)
        {
            try
            {
                while(true)
                {
                    if(!this._is_local())
                    {
                        org.apache.yoko.orb.OB.DowncallStub _ob_stub = _OB_getDowncallStub();
                        org.apache.yoko.orb.OB.Downcall _ob_down = _ob_stub.createPIArgsDowncall("one_string_inout", true, _ob_desc, null, null);
                        try
                        {
                            org.omg.CORBA.portable.OutputStream out = _ob_stub.preMarshal(_ob_down);
                            try
                            {
                                out.write_string(_ob_ah0.value);
                            }
                            catch(org.omg.CORBA.SystemException _ob_ex)
                            {
                                _ob_stub.marshalEx(_ob_down, _ob_ex);
                            }
                            _ob_stub.postMarshal(_ob_down);
                            _ob_stub.request(_ob_down);
                            org.omg.CORBA.BooleanHolder _ob_uex = new org.omg.CORBA.BooleanHolder();
                            org.omg.CORBA.portable.InputStream in = _ob_stub.preUnmarshal(_ob_down, _ob_uex);
                            if(_ob_uex.value)
                            {
                                _ob_stub.postUnmarshal(_ob_down);
                            }
                            else
                            {
                                try
                                {
                                    _ob_ah0.value = in.read_string();
                                }
                                catch(org.omg.CORBA.SystemException _ob_ex)
                                {
                                    _ob_stub.unmarshalEx(_ob_down, _ob_ex);
                                }
                                _ob_stub.postUnmarshal(_ob_down);
                                return;
                            }
                        }
                        catch(org.apache.yoko.orb.OB.FailureException _ob_ex)
                        {
                            _ob_stub.handleFailureException(_ob_down, _ob_ex);
                        }
                    }
                    else
                    {
                        org.omg.CORBA.portable.ServantObject _ob_so = _servant_preinvoke("one_string_inout", _ob_opsClass);
                        if(_ob_so == null)
                            continue;
                        TestInterfaceOperations _ob_self = (TestInterfaceOperations)_ob_so.servant;
                        try
                        {
                            _ob_self.one_string_inout(_ob_ah0);
                            return;
                        }
                        finally
                        {
                            _servant_postinvoke(_ob_so);
                        }
                    }
                }
            }
            catch(Exception _ob_ex)
            {
                _OB_handleException(_ob_ex, _ob_info);
            }
        }
    }

    //
    // IDL:TestInterface/one_string_out:1.0
    //
    public void
    one_string_out(org.omg.CORBA.StringHolder _ob_ah0)
    {
        org.apache.yoko.orb.OB.ParameterDesc[] _ob_desc =
        {
            new org.apache.yoko.orb.OB.ParameterDesc(_ob_ah0, _orb().get_primitive_tc(org.omg.CORBA.TCKind.tk_string), 1 /*out*/)
        };

        org.apache.yoko.orb.CORBA.RetryInfo _ob_info = new org.apache.yoko.orb.CORBA.RetryInfo();
        while(true)
        {
            try
            {
                while(true)
                {
                    if(!this._is_local())
                    {
                        org.apache.yoko.orb.OB.DowncallStub _ob_stub = _OB_getDowncallStub();
                        org.apache.yoko.orb.OB.Downcall _ob_down = _ob_stub.createPIArgsDowncall("one_string_out", true, _ob_desc, null, null);
                        try
                        {
                            _ob_stub.preMarshal(_ob_down);
                            _ob_stub.postMarshal(_ob_down);
                            _ob_stub.request(_ob_down);
                            org.omg.CORBA.BooleanHolder _ob_uex = new org.omg.CORBA.BooleanHolder();
                            org.omg.CORBA.portable.InputStream in = _ob_stub.preUnmarshal(_ob_down, _ob_uex);
                            if(_ob_uex.value)
                            {
                                _ob_stub.postUnmarshal(_ob_down);
                            }
                            else
                            {
                                try
                                {
                                    _ob_ah0.value = in.read_string();
                                }
                                catch(org.omg.CORBA.SystemException _ob_ex)
                                {
                                    _ob_stub.unmarshalEx(_ob_down, _ob_ex);
                                }
                                _ob_stub.postUnmarshal(_ob_down);
                                return;
                            }
                        }
                        catch(org.apache.yoko.orb.OB.FailureException _ob_ex)
                        {
                            _ob_stub.handleFailureException(_ob_down, _ob_ex);
                        }
                    }
                    else
                    {
                        org.omg.CORBA.portable.ServantObject _ob_so = _servant_preinvoke("one_string_out", _ob_opsClass);
                        if(_ob_so == null)
                            continue;
                        TestInterfaceOperations _ob_self = (TestInterfaceOperations)_ob_so.servant;
                        try
                        {
                            _ob_self.one_string_out(_ob_ah0);
                            return;
                        }
                        finally
                        {
                            _servant_postinvoke(_ob_so);
                        }
                    }
                }
            }
            catch(Exception _ob_ex)
            {
                _OB_handleException(_ob_ex, _ob_info);
            }
        }
    }

    //
    // IDL:TestInterface/one_string_return:1.0
    //
    public String
    one_string_return()
    {
        org.omg.CORBA.StringHolder _ob_rh = new org.omg.CORBA.StringHolder();
        org.apache.yoko.orb.OB.ParameterDesc _ob_retDesc = new org.apache.yoko.orb.OB.ParameterDesc(_ob_rh, _orb().get_primitive_tc(org.omg.CORBA.TCKind.tk_string), 0);

        org.apache.yoko.orb.CORBA.RetryInfo _ob_info = new org.apache.yoko.orb.CORBA.RetryInfo();
        while(true)
        {
            try
            {
                while(true)
                {
                    if(!this._is_local())
                    {
                        org.apache.yoko.orb.OB.DowncallStub _ob_stub = _OB_getDowncallStub();
                        org.apache.yoko.orb.OB.Downcall _ob_down = _ob_stub.createPIArgsDowncall("one_string_return", true, null, _ob_retDesc, null);
                        try
                        {
                            _ob_stub.preMarshal(_ob_down);
                            _ob_stub.postMarshal(_ob_down);
                            _ob_stub.request(_ob_down);
                            org.omg.CORBA.BooleanHolder _ob_uex = new org.omg.CORBA.BooleanHolder();
                            org.omg.CORBA.portable.InputStream in = _ob_stub.preUnmarshal(_ob_down, _ob_uex);
                            if(_ob_uex.value)
                            {
                                _ob_stub.postUnmarshal(_ob_down);
                            }
                            else
                            {
                                try
                                {
                                    _ob_rh.value = in.read_string();
                                }
                                catch(org.omg.CORBA.SystemException _ob_ex)
                                {
                                    _ob_stub.unmarshalEx(_ob_down, _ob_ex);
                                }
                                _ob_stub.postUnmarshal(_ob_down);
                                return _ob_rh.value;
                            }
                        }
                        catch(org.apache.yoko.orb.OB.FailureException _ob_ex)
                        {
                            _ob_stub.handleFailureException(_ob_down, _ob_ex);
                        }
                    }
                    else
                    {
                        org.omg.CORBA.portable.ServantObject _ob_so = _servant_preinvoke("one_string_return", _ob_opsClass);
                        if(_ob_so == null)
                            continue;
                        TestInterfaceOperations _ob_self = (TestInterfaceOperations)_ob_so.servant;
                        try
                        {
                            return _ob_self.one_string_return();
                        }
                        finally
                        {
                            _servant_postinvoke(_ob_so);
                        }
                    }
                }
            }
            catch(Exception _ob_ex)
            {
                _OB_handleException(_ob_ex, _ob_info);
            }
        }
    }

    //
    // IDL:TestInterface/one_struct_in:1.0
    //
    public void
    one_struct_in(s _ob_a0)
    {
        sHolder _ob_ah0 = new sHolder(_ob_a0);
        org.apache.yoko.orb.OB.ParameterDesc[] _ob_desc =
        {
            new org.apache.yoko.orb.OB.ParameterDesc(_ob_ah0, sHelper.type(), 0 /*in*/)
        };

        org.apache.yoko.orb.CORBA.RetryInfo _ob_info = new org.apache.yoko.orb.CORBA.RetryInfo();
        while(true)
        {
            try
            {
                while(true)
                {
                    if(!this._is_local())
                    {
                        org.apache.yoko.orb.OB.DowncallStub _ob_stub = _OB_getDowncallStub();
                        org.apache.yoko.orb.OB.Downcall _ob_down = _ob_stub.createPIArgsDowncall("one_struct_in", true, _ob_desc, null, null);
                        try
                        {
                            org.omg.CORBA.portable.OutputStream out = _ob_stub.preMarshal(_ob_down);
                            try
                            {
                                sHelper.write(out, _ob_ah0.value);
                            }
                            catch(org.omg.CORBA.SystemException _ob_ex)
                            {
                                _ob_stub.marshalEx(_ob_down, _ob_ex);
                            }
                            _ob_stub.postMarshal(_ob_down);
                            _ob_stub.request(_ob_down);
                            _ob_stub.preUnmarshal(_ob_down);
                            _ob_stub.postUnmarshal(_ob_down);
                            return;
                        }
                        catch(org.apache.yoko.orb.OB.FailureException _ob_ex)
                        {
                            _ob_stub.handleFailureException(_ob_down, _ob_ex);
                        }
                    }
                    else
                    {
                        org.omg.CORBA.portable.ServantObject _ob_so = _servant_preinvoke("one_struct_in", _ob_opsClass);
                        if(_ob_so == null)
                            continue;
                        TestInterfaceOperations _ob_self = (TestInterfaceOperations)_ob_so.servant;
                        try
                        {
                            _ob_self.one_struct_in(_ob_a0);
                            return;
                        }
                        finally
                        {
                            _servant_postinvoke(_ob_so);
                        }
                    }
                }
            }
            catch(Exception _ob_ex)
            {
                _OB_handleException(_ob_ex, _ob_info);
            }
        }
    }

    //
    // IDL:TestInterface/one_struct_inout:1.0
    //
    public void
    one_struct_inout(sHolder _ob_ah0)
    {
        org.apache.yoko.orb.OB.ParameterDesc[] _ob_desc =
        {
            new org.apache.yoko.orb.OB.ParameterDesc(_ob_ah0, sHelper.type(), 2 /*inout*/)
        };

        org.apache.yoko.orb.CORBA.RetryInfo _ob_info = new org.apache.yoko.orb.CORBA.RetryInfo();
        while(true)
        {
            try
            {
                while(true)
                {
                    if(!this._is_local())
                    {
                        org.apache.yoko.orb.OB.DowncallStub _ob_stub = _OB_getDowncallStub();
                        org.apache.yoko.orb.OB.Downcall _ob_down = _ob_stub.createPIArgsDowncall("one_struct_inout", true, _ob_desc, null, null);
                        try
                        {
                            org.omg.CORBA.portable.OutputStream out = _ob_stub.preMarshal(_ob_down);
                            try
                            {
                                sHelper.write(out, _ob_ah0.value);
                            }
                            catch(org.omg.CORBA.SystemException _ob_ex)
                            {
                                _ob_stub.marshalEx(_ob_down, _ob_ex);
                            }
                            _ob_stub.postMarshal(_ob_down);
                            _ob_stub.request(_ob_down);
                            org.omg.CORBA.BooleanHolder _ob_uex = new org.omg.CORBA.BooleanHolder();
                            org.omg.CORBA.portable.InputStream in = _ob_stub.preUnmarshal(_ob_down, _ob_uex);
                            if(_ob_uex.value)
                            {
                                _ob_stub.postUnmarshal(_ob_down);
                            }
                            else
                            {
                                try
                                {
                                    _ob_ah0.value = sHelper.read(in);
                                }
                                catch(org.omg.CORBA.SystemException _ob_ex)
                                {
                                    _ob_stub.unmarshalEx(_ob_down, _ob_ex);
                                }
                                _ob_stub.postUnmarshal(_ob_down);
                                return;
                            }
                        }
                        catch(org.apache.yoko.orb.OB.FailureException _ob_ex)
                        {
                            _ob_stub.handleFailureException(_ob_down, _ob_ex);
                        }
                    }
                    else
                    {
                        org.omg.CORBA.portable.ServantObject _ob_so = _servant_preinvoke("one_struct_inout", _ob_opsClass);
                        if(_ob_so == null)
                            continue;
                        TestInterfaceOperations _ob_self = (TestInterfaceOperations)_ob_so.servant;
                        try
                        {
                            _ob_self.one_struct_inout(_ob_ah0);
                            return;
                        }
                        finally
                        {
                            _servant_postinvoke(_ob_so);
                        }
                    }
                }
            }
            catch(Exception _ob_ex)
            {
                _OB_handleException(_ob_ex, _ob_info);
            }
        }
    }

    //
    // IDL:TestInterface/one_struct_out:1.0
    //
    public void
    one_struct_out(sHolder _ob_ah0)
    {
        org.apache.yoko.orb.OB.ParameterDesc[] _ob_desc =
        {
            new org.apache.yoko.orb.OB.ParameterDesc(_ob_ah0, sHelper.type(), 1 /*out*/)
        };

        org.apache.yoko.orb.CORBA.RetryInfo _ob_info = new org.apache.yoko.orb.CORBA.RetryInfo();
        while(true)
        {
            try
            {
                while(true)
                {
                    if(!this._is_local())
                    {
                        org.apache.yoko.orb.OB.DowncallStub _ob_stub = _OB_getDowncallStub();
                        org.apache.yoko.orb.OB.Downcall _ob_down = _ob_stub.createPIArgsDowncall("one_struct_out", true, _ob_desc, null, null);
                        try
                        {
                            _ob_stub.preMarshal(_ob_down);
                            _ob_stub.postMarshal(_ob_down);
                            _ob_stub.request(_ob_down);
                            org.omg.CORBA.BooleanHolder _ob_uex = new org.omg.CORBA.BooleanHolder();
                            org.omg.CORBA.portable.InputStream in = _ob_stub.preUnmarshal(_ob_down, _ob_uex);
                            if(_ob_uex.value)
                            {
                                _ob_stub.postUnmarshal(_ob_down);
                            }
                            else
                            {
                                try
                                {
                                    _ob_ah0.value = sHelper.read(in);
                                }
                                catch(org.omg.CORBA.SystemException _ob_ex)
                                {
                                    _ob_stub.unmarshalEx(_ob_down, _ob_ex);
                                }
                                _ob_stub.postUnmarshal(_ob_down);
                                return;
                            }
                        }
                        catch(org.apache.yoko.orb.OB.FailureException _ob_ex)
                        {
                            _ob_stub.handleFailureException(_ob_down, _ob_ex);
                        }
                    }
                    else
                    {
                        org.omg.CORBA.portable.ServantObject _ob_so = _servant_preinvoke("one_struct_out", _ob_opsClass);
                        if(_ob_so == null)
                            continue;
                        TestInterfaceOperations _ob_self = (TestInterfaceOperations)_ob_so.servant;
                        try
                        {
                            _ob_self.one_struct_out(_ob_ah0);
                            return;
                        }
                        finally
                        {
                            _servant_postinvoke(_ob_so);
                        }
                    }
                }
            }
            catch(Exception _ob_ex)
            {
                _OB_handleException(_ob_ex, _ob_info);
            }
        }
    }

    //
    // IDL:TestInterface/one_struct_return:1.0
    //
    public s
    one_struct_return()
    {
        sHolder _ob_rh = new sHolder();
        org.apache.yoko.orb.OB.ParameterDesc _ob_retDesc = new org.apache.yoko.orb.OB.ParameterDesc(_ob_rh, sHelper.type(), 0);

        org.apache.yoko.orb.CORBA.RetryInfo _ob_info = new org.apache.yoko.orb.CORBA.RetryInfo();
        while(true)
        {
            try
            {
                while(true)
                {
                    if(!this._is_local())
                    {
                        org.apache.yoko.orb.OB.DowncallStub _ob_stub = _OB_getDowncallStub();
                        org.apache.yoko.orb.OB.Downcall _ob_down = _ob_stub.createPIArgsDowncall("one_struct_return", true, null, _ob_retDesc, null);
                        try
                        {
                            _ob_stub.preMarshal(_ob_down);
                            _ob_stub.postMarshal(_ob_down);
                            _ob_stub.request(_ob_down);
                            org.omg.CORBA.BooleanHolder _ob_uex = new org.omg.CORBA.BooleanHolder();
                            org.omg.CORBA.portable.InputStream in = _ob_stub.preUnmarshal(_ob_down, _ob_uex);
                            if(_ob_uex.value)
                            {
                                _ob_stub.postUnmarshal(_ob_down);
                            }
                            else
                            {
                                try
                                {
                                    _ob_rh.value = sHelper.read(in);
                                }
                                catch(org.omg.CORBA.SystemException _ob_ex)
                                {
                                    _ob_stub.unmarshalEx(_ob_down, _ob_ex);
                                }
                                _ob_stub.postUnmarshal(_ob_down);
                                return _ob_rh.value;
                            }
                        }
                        catch(org.apache.yoko.orb.OB.FailureException _ob_ex)
                        {
                            _ob_stub.handleFailureException(_ob_down, _ob_ex);
                        }
                    }
                    else
                    {
                        org.omg.CORBA.portable.ServantObject _ob_so = _servant_preinvoke("one_struct_return", _ob_opsClass);
                        if(_ob_so == null)
                            continue;
                        TestInterfaceOperations _ob_self = (TestInterfaceOperations)_ob_so.servant;
                        try
                        {
                            return _ob_self.one_struct_return();
                        }
                        finally
                        {
                            _servant_postinvoke(_ob_so);
                        }
                    }
                }
            }
            catch(Exception _ob_ex)
            {
                _OB_handleException(_ob_ex, _ob_info);
            }
        }
    }
}
