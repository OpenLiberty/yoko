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
* org/omg/SecurityLevel2/_CurrentStub.java .
* Error reading Messages File.
* Error reading Messages File.
* Thursday, January 14, 2010 1:08:59 AM PST
*/


/* */
public class _CurrentStub extends org.omg.CORBA.portable.ObjectImpl implements org.omg.SecurityLevel2.Current
{


  // Thread specific
  public org.omg.SecurityLevel2.ReceivedCredentials received_credentials ()
  {
            org.omg.CORBA.portable.InputStream $in = null;
            try {
                org.omg.CORBA.portable.OutputStream $out = _request ("_get_received_credentials", true);
                $in = _invoke ($out);
                org.omg.SecurityLevel2.ReceivedCredentials $result = org.omg.SecurityLevel2.ReceivedCredentialsHelper.read ($in);
                return $result;
            } catch (org.omg.CORBA.portable.ApplicationException $ex) {
                $in = $ex.getInputStream ();
                String _id = $ex.getId ();
                throw new org.omg.CORBA.MARSHAL (_id);
            } catch (org.omg.CORBA.portable.RemarshalException $rm) {
                return received_credentials (        );
            } finally {
                _releaseReply ($in);
            }
  } // received_credentials


  // thread specific operations
  public org.omg.Security.SecAttribute[] get_attributes (org.omg.Security.AttributeType[] ttributes)
  {
            org.omg.CORBA.portable.InputStream $in = null;
            try {
                org.omg.CORBA.portable.OutputStream $out = _request ("get_attributes", true);
                org.omg.Security.AttributeTypeListHelper.write ($out, ttributes);
                $in = _invoke ($out);
                org.omg.Security.SecAttribute $result[] = org.omg.Security.AttributeListHelper.read ($in);
                return $result;
            } catch (org.omg.CORBA.portable.ApplicationException $ex) {
                $in = $ex.getInputStream ();
                String _id = $ex.getId ();
                throw new org.omg.CORBA.MARSHAL (_id);
            } catch (org.omg.CORBA.portable.RemarshalException $rm) {
                return get_attributes (ttributes        );
            } finally {
                _releaseReply ($in);
            }
  } // get_attributes

  // Type-specific CORBA::Object operations
  private static String[] __ids = {
    "IDL:SecurityLevel2/Current:1.0", 
    "IDL:omg.org/SecurityLevel1/Current:1.0", 
    "IDL:CORBA/Current:1.0"};

  public String[] _ids ()
  {
    return (String[])__ids.clone ();
  }

  private void readObject (java.io.ObjectInputStream s) throws java.io.IOException
  {
     String str = s.readUTF ();
     String[] args = null;
     java.util.Properties props = null;
     org.omg.CORBA.Object obj = org.omg.CORBA.ORB.init (args, props).string_to_object (str);
     org.omg.CORBA.portable.Delegate delegate = ((org.omg.CORBA.portable.ObjectImpl) obj)._get_delegate ();
     _set_delegate (delegate);
  }

  private void writeObject (java.io.ObjectOutputStream s) throws java.io.IOException
  {
     String[] args = null;
     java.util.Properties props = null;
     String str = org.omg.CORBA.ORB.init (args, props).object_to_string (this);
     s.writeUTF (str);
  }
} // class _CurrentStub
