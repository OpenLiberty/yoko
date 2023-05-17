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
* org/omg/CSIIOP/CompoundSecMechanismsHelper.java .
* Error reading Messages File.
* Error reading Messages File.
* Thursday, January 14, 2010 1:08:59 AM PST
*/

abstract public class CompoundSecMechanismsHelper
{
  private static String  _id = "IDL:omg.org/CSIIOP/CompoundSecMechanisms:1.0";

  public static void insert (org.omg.CORBA.Any a, org.omg.CSIIOP.CompoundSecMech[] that)
  {
    org.omg.CORBA.portable.OutputStream out = a.create_output_stream ();
    a.type (type ());
    write (out, that);
    a.read_value (out.create_input_stream (), type ());
  }

  public static org.omg.CSIIOP.CompoundSecMech[] extract (org.omg.CORBA.Any a)
  {
    return read (a.create_input_stream ());
  }

  private static org.omg.CORBA.TypeCode __typeCode = null;
  synchronized public static org.omg.CORBA.TypeCode type ()
  {
    if (__typeCode == null)
    {
      __typeCode = org.omg.CSIIOP.CompoundSecMechHelper.type ();
      __typeCode = org.omg.CORBA.ORB.init ().create_sequence_tc (0, __typeCode);
      __typeCode = org.omg.CORBA.ORB.init ().create_alias_tc (org.omg.CSIIOP.CompoundSecMechanismsHelper.id (), "CompoundSecMechanisms", __typeCode);
    }
    return __typeCode;
  }

  public static String id ()
  {
    return _id;
  }

  public static org.omg.CSIIOP.CompoundSecMech[] read (org.omg.CORBA.portable.InputStream istream)
  {
    org.omg.CSIIOP.CompoundSecMech value[] = null;
    int _len0 = istream.read_long ();
    value = new org.omg.CSIIOP.CompoundSecMech[_len0];
    for (int _o1 = 0;_o1 < value.length; ++_o1)
      value[_o1] = org.omg.CSIIOP.CompoundSecMechHelper.read (istream);
    return value;
  }

  public static void write (org.omg.CORBA.portable.OutputStream ostream, org.omg.CSIIOP.CompoundSecMech[] value)
  {
    ostream.write_long (value.length);
    for (int _i0 = 0;_i0 < value.length; ++_i0)
      org.omg.CSIIOP.CompoundSecMechHelper.write (ostream, value[_i0]);
  }

}
