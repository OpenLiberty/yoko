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
// IDL:omg.org/CORBA/ValueBoxDef:1.0
//
final public class ValueBoxDefHelper
{
    public static void
    insert(org.omg.CORBA.Any any, ValueBoxDef val)
    {
        any.insert_Object(val, type());
    }

    public static ValueBoxDef
    extract(org.omg.CORBA.Any any)
    {
        if(any.type().equivalent(type()))
            return narrow(any.extract_Object());

        throw new org.omg.CORBA.BAD_OPERATION();
    }

    private static org.omg.CORBA.TypeCode typeCode_;

    public static org.omg.CORBA.TypeCode
    type()
    {
        if(typeCode_ == null)
        {
            org.omg.CORBA.ORB orb = org.omg.CORBA.ORB.init();
            typeCode_ = orb.create_interface_tc(id(), "ValueBoxDef");
        }

        return typeCode_;
    }

    public static String
    id()
    {
        return "IDL:omg.org/CORBA/ValueBoxDef:1.0";
    }

    public static ValueBoxDef
    read(org.omg.CORBA.portable.InputStream in)
    {
        org.omg.CORBA.Object _ob_v = in.read_Object();

        try
        {
            return (ValueBoxDef)_ob_v;
        }
        catch(ClassCastException ex)
        {
        }

        org.omg.CORBA.portable.ObjectImpl _ob_impl;
        _ob_impl = (org.omg.CORBA.portable.ObjectImpl)_ob_v;
        _ValueBoxDefStub _ob_stub = new _ValueBoxDefStub();
        _ob_stub._set_delegate(_ob_impl._get_delegate());
        return _ob_stub;
    }

    public static void
    write(org.omg.CORBA.portable.OutputStream out, ValueBoxDef val)
    {
        out.write_Object(val);
    }

    public static ValueBoxDef
    narrow(org.omg.CORBA.Object val)
    {
        if(val != null)
        {
            try
            {
                return (ValueBoxDef)val;
            }
            catch(ClassCastException ex)
            {
            }

            if(val._is_a(id()))
            {
                org.omg.CORBA.portable.ObjectImpl _ob_impl;
                _ValueBoxDefStub _ob_stub = new _ValueBoxDefStub();
                _ob_impl = (org.omg.CORBA.portable.ObjectImpl)val;
                _ob_stub._set_delegate(_ob_impl._get_delegate());
                return _ob_stub;
            }

            throw new org.omg.CORBA.BAD_PARAM();
        }

        return null;
    }

    public static ValueBoxDef
    unchecked_narrow(org.omg.CORBA.Object val)
    {
        if(val != null)
        {
            try
            {
                return (ValueBoxDef)val;
            }
            catch(ClassCastException ex)
            {
            }

            org.omg.CORBA.portable.ObjectImpl _ob_impl;
            _ValueBoxDefStub _ob_stub = new _ValueBoxDefStub();
            _ob_impl = (org.omg.CORBA.portable.ObjectImpl)val;
            _ob_stub._set_delegate(_ob_impl._get_delegate());
            return _ob_stub;
        }

        return null;
    }
}
