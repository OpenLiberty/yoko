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
package org.omg.CSIIOP;


/**
* org/omg/CSIIOP/AS_ContextSecHelper.java .
* Error reading Messages File.
* Error reading Messages File.
* Thursday, January 14, 2010 1:08:59 AM PST
*/

abstract public class AS_ContextSecHelper
{
  private static String  _id = "IDL:omg.org/CSIIOP/AS_ContextSec:1.0";

  public static void insert (org.omg.CORBA.Any a, org.omg.CSIIOP.AS_ContextSec that)
  {
    org.omg.CORBA.portable.OutputStream out = a.create_output_stream ();
    a.type (type ());
    write (out, that);
    a.read_value (out.create_input_stream (), type ());
  }

  public static org.omg.CSIIOP.AS_ContextSec extract (org.omg.CORBA.Any a)
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
          _tcOf_members0 = org.omg.CORBA.ORB.init ().get_primitive_tc (org.omg.CORBA.TCKind.tk_ushort);
          _tcOf_members0 = org.omg.CORBA.ORB.init ().create_alias_tc (org.omg.CSIIOP.AssociationOptionsHelper.id (), "AssociationOptions", _tcOf_members0);
          _members0[0] = new org.omg.CORBA.StructMember (
            "target_supports",
            _tcOf_members0,
            null);
          _tcOf_members0 = org.omg.CORBA.ORB.init ().get_primitive_tc (org.omg.CORBA.TCKind.tk_ushort);
          _tcOf_members0 = org.omg.CORBA.ORB.init ().create_alias_tc (org.omg.CSIIOP.AssociationOptionsHelper.id (), "AssociationOptions", _tcOf_members0);
          _members0[1] = new org.omg.CORBA.StructMember (
            "target_requires",
            _tcOf_members0,
            null);
          _tcOf_members0 = org.omg.CORBA.ORB.init ().get_primitive_tc (org.omg.CORBA.TCKind.tk_octet);
          _tcOf_members0 = org.omg.CORBA.ORB.init ().create_sequence_tc (0, _tcOf_members0);
          _tcOf_members0 = org.omg.CORBA.ORB.init ().create_alias_tc (org.omg.CSI.OIDHelper.id (), "OID", _tcOf_members0);
          _members0[2] = new org.omg.CORBA.StructMember (
            "client_authentication_mech",
            _tcOf_members0,
            null);
          _tcOf_members0 = org.omg.CORBA.ORB.init ().get_primitive_tc (org.omg.CORBA.TCKind.tk_octet);
          _tcOf_members0 = org.omg.CORBA.ORB.init ().create_sequence_tc (0, _tcOf_members0);
          _tcOf_members0 = org.omg.CORBA.ORB.init ().create_alias_tc (org.omg.CSI.GSS_NT_ExportedNameHelper.id (), "GSS_NT_ExportedName", _tcOf_members0);
          _members0[3] = new org.omg.CORBA.StructMember (
            "target_name",
            _tcOf_members0,
            null);
          __typeCode = org.omg.CORBA.ORB.init ().create_struct_tc (org.omg.CSIIOP.AS_ContextSecHelper.id (), "AS_ContextSec", _members0);
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

  public static org.omg.CSIIOP.AS_ContextSec read (org.omg.CORBA.portable.InputStream istream)
  {
    org.omg.CSIIOP.AS_ContextSec value = new org.omg.CSIIOP.AS_ContextSec ();
    value.target_supports = istream.read_ushort ();
    value.target_requires = istream.read_ushort ();
    value.client_authentication_mech = org.omg.CSI.OIDHelper.read (istream);
    value.target_name = org.omg.CSI.GSS_NT_ExportedNameHelper.read (istream);
    return value;
  }

  public static void write (org.omg.CORBA.portable.OutputStream ostream, org.omg.CSIIOP.AS_ContextSec value)
  {
    ostream.write_ushort (value.target_supports);
    ostream.write_ushort (value.target_requires);
    org.omg.CSI.OIDHelper.write (ostream, value.client_authentication_mech);
    org.omg.CSI.GSS_NT_ExportedNameHelper.write (ostream, value.target_name);
  }

}
