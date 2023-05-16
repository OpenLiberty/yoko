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
package org.omg.Security;


/**
* org/omg/Security/IntervalTHelper.java .
* Error reading Messages File.
* Error reading Messages File.
* Thursday, January 14, 2010 1:08:58 AM PST
*/

abstract public class IntervalTHelper
{
  private static String  _id = "IDL:omg.org/Security/IntervalT:1.0";

  public static void insert (org.omg.CORBA.Any a, org.omg.TimeBase.IntervalT that)
  {
    org.omg.CORBA.portable.OutputStream out = a.create_output_stream ();
    a.type (type ());
    write (out, that);
    a.read_value (out.create_input_stream (), type ());
  }

  public static org.omg.TimeBase.IntervalT extract (org.omg.CORBA.Any a)
  {
    return read (a.create_input_stream ());
  }

  private static org.omg.CORBA.TypeCode __typeCode = null;
  synchronized public static org.omg.CORBA.TypeCode type ()
  {
    if (__typeCode == null)
    {
      __typeCode = org.omg.TimeBase.IntervalTHelper.type ();
      __typeCode = org.omg.CORBA.ORB.init ().create_alias_tc (org.omg.Security.IntervalTHelper.id (), "IntervalT", __typeCode);
    }
    return __typeCode;
  }

  public static String id ()
  {
    return _id;
  }

  public static org.omg.TimeBase.IntervalT read (org.omg.CORBA.portable.InputStream istream)
  {
    org.omg.TimeBase.IntervalT value = null;
    value = org.omg.TimeBase.IntervalTHelper.read (istream);
    return value;
  }

  public static void write (org.omg.CORBA.portable.OutputStream ostream, org.omg.TimeBase.IntervalT value)
  {
    org.omg.TimeBase.IntervalTHelper.write (ostream, value);
  }

}
