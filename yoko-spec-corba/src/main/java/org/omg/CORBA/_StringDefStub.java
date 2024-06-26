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
package org.omg.CORBA;

//
// IDL:omg.org/CORBA/StringDef:1.0
//
public class _StringDefStub extends org.omg.CORBA.portable.ObjectImpl
                            implements StringDef
{
    private static final String[] _ob_ids_ =
    {
        "IDL:omg.org/CORBA/StringDef:1.0",
        "IDL:omg.org/CORBA/IDLType:1.0",
        "IDL:omg.org/CORBA/IRObject:1.0"
    };

    public String[]
    _ids()
    {
        return _ob_ids_;
    }

    final public static java.lang.Class _ob_opsClass = StringDefOperations.class;

    //
    // IDL:omg.org/CORBA/StringDef/bound:1.0
    //
    public int
    bound()
    {
        while(true)
        {
            if(!this._is_local())
            {
                org.omg.CORBA.portable.OutputStream out = null;
                org.omg.CORBA.portable.InputStream in = null;
                try
                {
                    out = _request("_get_bound", true);
                    in = _invoke(out);
                    int _ob_r = in.read_ulong();
                    return _ob_r;
                }
                catch(org.omg.CORBA.portable.RemarshalException _ob_ex)
                {
                    continue;
                }
                catch(org.omg.CORBA.portable.ApplicationException _ob_aex)
                {
                    final String _ob_id = _ob_aex.getId();
                    throw new org.omg.CORBA.UNKNOWN("Unexpected User Exception: " + _ob_id);
                }
                finally
                {
                    _releaseReply(in);
                }
            }
            else
            {
                org.omg.CORBA.portable.ServantObject _ob_so = _servant_preinvoke("bound", _ob_opsClass);
                if(_ob_so == null)
                    continue;
                StringDefOperations _ob_self = (StringDefOperations)_ob_so.servant;
                try
                {
                    return _ob_self.bound();
                }
                finally
                {
                    _servant_postinvoke(_ob_so);
                }
            }
        }
    }

    public void
    bound(int _ob_a)
    {
        while(true)
        {
            if(!this._is_local())
            {
                org.omg.CORBA.portable.OutputStream out = null;
                org.omg.CORBA.portable.InputStream in = null;
                try
                {
                    out = _request("_set_bound", true);
                    out.write_ulong(_ob_a);
                    in = _invoke(out);
                    return;
                }
                catch(org.omg.CORBA.portable.RemarshalException _ob_ex)
                {
                    continue;
                }
                catch(org.omg.CORBA.portable.ApplicationException _ob_aex)
                {
                    final String _ob_id = _ob_aex.getId();
                    throw new org.omg.CORBA.UNKNOWN("Unexpected User Exception: " + _ob_id);
                }
                finally
                {
                    _releaseReply(in);
                }
            }
            else
            {
                org.omg.CORBA.portable.ServantObject _ob_so = _servant_preinvoke("bound", _ob_opsClass);
                if(_ob_so == null)
                    continue;
                StringDefOperations _ob_self = (StringDefOperations)_ob_so.servant;
                try
                {
                    _ob_self.bound(_ob_a);
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
    // IDL:omg.org/CORBA/IDLType/type:1.0
    //
    public org.omg.CORBA.TypeCode
    type()
    {
        while(true)
        {
            if(!this._is_local())
            {
                org.omg.CORBA.portable.OutputStream out = null;
                org.omg.CORBA.portable.InputStream in = null;
                try
                {
                    out = _request("_get_type", true);
                    in = _invoke(out);
                    org.omg.CORBA.TypeCode _ob_r = in.read_TypeCode();
                    return _ob_r;
                }
                catch(org.omg.CORBA.portable.RemarshalException _ob_ex)
                {
                    continue;
                }
                catch(org.omg.CORBA.portable.ApplicationException _ob_aex)
                {
                    final String _ob_id = _ob_aex.getId();
                    throw new org.omg.CORBA.UNKNOWN("Unexpected User Exception: " + _ob_id);
                }
                finally
                {
                    _releaseReply(in);
                }
            }
            else
            {
                org.omg.CORBA.portable.ServantObject _ob_so = _servant_preinvoke("type", _ob_opsClass);
                if(_ob_so == null)
                    continue;
                StringDefOperations _ob_self = (StringDefOperations)_ob_so.servant;
                try
                {
                    return _ob_self.type();
                }
                finally
                {
                    _servant_postinvoke(_ob_so);
                }
            }
        }
    }

    //
    // IDL:omg.org/CORBA/IRObject/def_kind:1.0
    //
    public DefinitionKind
    def_kind()
    {
        while(true)
        {
            if(!this._is_local())
            {
                org.omg.CORBA.portable.OutputStream out = null;
                org.omg.CORBA.portable.InputStream in = null;
                try
                {
                    out = _request("_get_def_kind", true);
                    in = _invoke(out);
                    DefinitionKind _ob_r = DefinitionKindHelper.read(in);
                    return _ob_r;
                }
                catch(org.omg.CORBA.portable.RemarshalException _ob_ex)
                {
                    continue;
                }
                catch(org.omg.CORBA.portable.ApplicationException _ob_aex)
                {
                    final String _ob_id = _ob_aex.getId();
                    throw new org.omg.CORBA.UNKNOWN("Unexpected User Exception: " + _ob_id);
                }
                finally
                {
                    _releaseReply(in);
                }
            }
            else
            {
                org.omg.CORBA.portable.ServantObject _ob_so = _servant_preinvoke("def_kind", _ob_opsClass);
                if(_ob_so == null)
                    continue;
                StringDefOperations _ob_self = (StringDefOperations)_ob_so.servant;
                try
                {
                    return _ob_self.def_kind();
                }
                finally
                {
                    _servant_postinvoke(_ob_so);
                }
            }
        }
    }

    //
    // IDL:omg.org/CORBA/IRObject/destroy:1.0
    //
    public void
    destroy()
    {
        while(true)
        {
            if(!this._is_local())
            {
                org.omg.CORBA.portable.OutputStream out = null;
                org.omg.CORBA.portable.InputStream in = null;
                try
                {
                    out = _request("destroy", true);
                    in = _invoke(out);
                    return;
                }
                catch(org.omg.CORBA.portable.RemarshalException _ob_ex)
                {
                    continue;
                }
                catch(org.omg.CORBA.portable.ApplicationException _ob_aex)
                {
                    final String _ob_id = _ob_aex.getId();
                    in = _ob_aex.getInputStream();

                    throw new org.omg.CORBA.UNKNOWN("Unexpected User Exception: " + _ob_id);
                }
                finally
                {
                    _releaseReply(in);
                }
            }
            else
            {
                org.omg.CORBA.portable.ServantObject _ob_so = _servant_preinvoke("destroy", _ob_opsClass);
                if(_ob_so == null)
                    continue;
                StringDefOperations _ob_self = (StringDefOperations)_ob_so.servant;
                try
                {
                    _ob_self.destroy();
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
