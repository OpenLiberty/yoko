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
package org.omg.CosTransactions;


/**
* org/omg/CosTransactions/OTSPolicyHelper.java .
* Error reading Messages File.
* Error reading Messages File.
* Thursday, January 14, 2010 1:08:59 AM PST
*/

abstract public class OTSPolicyHelper
{
  private static String  _id = "IDL:CosTransactions/OTSPolicy:1.0";

  public static void insert (org.omg.CORBA.Any a, org.omg.CosTransactions.OTSPolicy that)
  {
    org.omg.CORBA.portable.OutputStream out = a.create_output_stream ();
    a.type (type ());
    write (out, that);
    a.read_value (out.create_input_stream (), type ());
  }

  public static org.omg.CosTransactions.OTSPolicy extract (org.omg.CORBA.Any a)
  {
    return read (a.create_input_stream ());
  }

  private static org.omg.CORBA.TypeCode __typeCode = null;
  synchronized public static org.omg.CORBA.TypeCode type ()
  {
    if (__typeCode == null)
    {
      __typeCode = org.omg.CORBA.ORB.init ().create_interface_tc (org.omg.CosTransactions.OTSPolicyHelper.id (), "OTSPolicy");
    }
    return __typeCode;
  }

  public static String id ()
  {
    return _id;
  }

  public static org.omg.CosTransactions.OTSPolicy read (org.omg.CORBA.portable.InputStream istream)
  {
    return narrow (istream.read_Object (_OTSPolicyStub.class));
  }

  public static void write (org.omg.CORBA.portable.OutputStream ostream, org.omg.CosTransactions.OTSPolicy value)
  {
    ostream.write_Object ((org.omg.CORBA.Object) value);
  }

  public static org.omg.CosTransactions.OTSPolicy narrow (org.omg.CORBA.Object obj)
  {
    if (obj == null)
      return null;
    else if (obj instanceof org.omg.CosTransactions.OTSPolicy)
      return (org.omg.CosTransactions.OTSPolicy)obj;
    else if (!obj._is_a (id ()))
      throw new org.omg.CORBA.BAD_PARAM ();
    else
    {
      org.omg.CORBA.portable.Delegate delegate = ((org.omg.CORBA.portable.ObjectImpl)obj)._get_delegate ();
      org.omg.CosTransactions._OTSPolicyStub stub = new org.omg.CosTransactions._OTSPolicyStub ();
      stub._set_delegate(delegate);
      return stub;
    }
  }

  public static org.omg.CosTransactions.OTSPolicy unchecked_narrow (org.omg.CORBA.Object obj)
  {
    if (obj == null)
      return null;
    else if (obj instanceof org.omg.CosTransactions.OTSPolicy)
      return (org.omg.CosTransactions.OTSPolicy)obj;
    else
    {
      org.omg.CORBA.portable.Delegate delegate = ((org.omg.CORBA.portable.ObjectImpl)obj)._get_delegate ();
      org.omg.CosTransactions._OTSPolicyStub stub = new org.omg.CosTransactions._OTSPolicyStub ();
      stub._set_delegate(delegate);
      return stub;
    }
  }

}
