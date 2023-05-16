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
* org/omg/CosTransactions/PropagationContextHelper.java .
* Error reading Messages File.
* Error reading Messages File.
* Thursday, January 14, 2010 1:08:59 AM PST
*/

abstract public class PropagationContextHelper
{
  private static String  _id = "IDL:CosTransactions/PropagationContext:1.0";

  public static void insert (org.omg.CORBA.Any a, org.omg.CosTransactions.PropagationContext that)
  {
    org.omg.CORBA.portable.OutputStream out = a.create_output_stream ();
    a.type (type ());
    write (out, that);
    a.read_value (out.create_input_stream (), type ());
  }

  public static org.omg.CosTransactions.PropagationContext extract (org.omg.CORBA.Any a)
  {
    return read (a.create_input_stream ());
  }

  private static org.omg.CORBA.TypeCode __typeCode = null;
  private static boolean __active = false;
  synchronized public static org.omg.CORBA.TypeCode type ()
  {
    if (__typeCode == null)
    {
      synchronized (org.omg.CORBA.TypeCode.class)
      {
        if (__typeCode == null)
        {
          if (__active)
          {
            return org.omg.CORBA.ORB.init().create_recursive_tc ( _id );
          }
          __active = true;
          org.omg.CORBA.StructMember[] _members0 = new org.omg.CORBA.StructMember [4];
          org.omg.CORBA.TypeCode _tcOf_members0 = null;
          _tcOf_members0 = org.omg.CORBA.ORB.init ().get_primitive_tc (org.omg.CORBA.TCKind.tk_ulong);
          _members0[0] = new org.omg.CORBA.StructMember (
            "timeout",
            _tcOf_members0,
            null);
          _tcOf_members0 = org.omg.CosTransactions.TransIdentityHelper.type ();
          _members0[1] = new org.omg.CORBA.StructMember (
            "current",
            _tcOf_members0,
            null);
          _tcOf_members0 = org.omg.CosTransactions.TransIdentityHelper.type ();
          _tcOf_members0 = org.omg.CORBA.ORB.init ().create_sequence_tc (0, _tcOf_members0);
          _members0[2] = new org.omg.CORBA.StructMember (
            "parents",
            _tcOf_members0,
            null);
          _tcOf_members0 = org.omg.CORBA.ORB.init ().get_primitive_tc (org.omg.CORBA.TCKind.tk_any);
          _members0[3] = new org.omg.CORBA.StructMember (
            "implementation_specific_data",
            _tcOf_members0,
            null);
          __typeCode = org.omg.CORBA.ORB.init ().create_struct_tc (org.omg.CosTransactions.PropagationContextHelper.id (), "PropagationContext", _members0);
          __active = false;
        }
      }
    }
    return __typeCode;
  }

  public static String id ()
  {
    return _id;
  }

  public static org.omg.CosTransactions.PropagationContext read (org.omg.CORBA.portable.InputStream istream)
  {
    org.omg.CosTransactions.PropagationContext value = new org.omg.CosTransactions.PropagationContext ();
    value.timeout = istream.read_ulong ();
    value.current = org.omg.CosTransactions.TransIdentityHelper.read (istream);
    int _len0 = istream.read_long ();
    value.parents = new org.omg.CosTransactions.TransIdentity[_len0];
    for (int _o1 = 0;_o1 < value.parents.length; ++_o1)
      value.parents[_o1] = org.omg.CosTransactions.TransIdentityHelper.read (istream);
    value.implementation_specific_data = istream.read_any ();
    return value;
  }

  public static void write (org.omg.CORBA.portable.OutputStream ostream, org.omg.CosTransactions.PropagationContext value)
  {
    ostream.write_ulong (value.timeout);
    org.omg.CosTransactions.TransIdentityHelper.write (ostream, value.current);
    ostream.write_long (value.parents.length);
    for (int _i0 = 0;_i0 < value.parents.length; ++_i0)
      org.omg.CosTransactions.TransIdentityHelper.write (ostream, value.parents[_i0]);
    ostream.write_any (value.implementation_specific_data);
  }

}
