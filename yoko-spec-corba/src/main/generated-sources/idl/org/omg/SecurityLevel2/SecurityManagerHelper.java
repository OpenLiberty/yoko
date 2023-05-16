/*==============================================================================
 * Copyright 2010 IBM Corporation and others.
 * 
 * Licensed under the Apache License, Version 2.0 (the License);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *=============================================================================*/
package org.omg.SecurityLevel2;


/**
* org/omg/SecurityLevel2/SecurityManagerHelper.java .
* Error reading Messages File.
* Error reading Messages File.
* Thursday, January 14, 2010 1:08:59 AM PST
*/


/* */
abstract public class SecurityManagerHelper
{
  private static String  _id = "IDL:SecurityLevel2/SecurityManager:1.0";

  public static void insert (org.omg.CORBA.Any a, org.omg.SecurityLevel2.SecurityManager that)
  {
    org.omg.CORBA.portable.OutputStream out = a.create_output_stream ();
    a.type (type ());
    write (out, that);
    a.read_value (out.create_input_stream (), type ());
  }

  public static org.omg.SecurityLevel2.SecurityManager extract (org.omg.CORBA.Any a)
  {
    return read (a.create_input_stream ());
  }

  private static org.omg.CORBA.TypeCode __typeCode = null;
  synchronized public static org.omg.CORBA.TypeCode type ()
  {
    if (__typeCode == null)
    {
      __typeCode = org.omg.CORBA.ORB.init ().create_interface_tc (org.omg.SecurityLevel2.SecurityManagerHelper.id (), "SecurityManager");
    }
    return __typeCode;
  }

  public static String id ()
  {
    return _id;
  }

  public static org.omg.SecurityLevel2.SecurityManager read (org.omg.CORBA.portable.InputStream istream)
  {
    return narrow (istream.read_Object (_SecurityManagerStub.class));
  }

  public static void write (org.omg.CORBA.portable.OutputStream ostream, org.omg.SecurityLevel2.SecurityManager value)
  {
    ostream.write_Object ((org.omg.CORBA.Object) value);
  }

  public static org.omg.SecurityLevel2.SecurityManager narrow (org.omg.CORBA.Object obj)
  {
    if (obj == null)
      return null;
    else if (obj instanceof org.omg.SecurityLevel2.SecurityManager)
      return (org.omg.SecurityLevel2.SecurityManager)obj;
    else if (!obj._is_a (id ()))
      throw new org.omg.CORBA.BAD_PARAM ();
    else
    {
      org.omg.CORBA.portable.Delegate delegate = ((org.omg.CORBA.portable.ObjectImpl)obj)._get_delegate ();
      org.omg.SecurityLevel2._SecurityManagerStub stub = new org.omg.SecurityLevel2._SecurityManagerStub ();
      stub._set_delegate(delegate);
      return stub;
    }
  }

  public static org.omg.SecurityLevel2.SecurityManager unchecked_narrow (org.omg.CORBA.Object obj)
  {
    if (obj == null)
      return null;
    else if (obj instanceof org.omg.SecurityLevel2.SecurityManager)
      return (org.omg.SecurityLevel2.SecurityManager)obj;
    else
    {
      org.omg.CORBA.portable.Delegate delegate = ((org.omg.CORBA.portable.ObjectImpl)obj)._get_delegate ();
      org.omg.SecurityLevel2._SecurityManagerStub stub = new org.omg.SecurityLevel2._SecurityManagerStub ();
      stub._set_delegate(delegate);
      return stub;
    }
  }

}
