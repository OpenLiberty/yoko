/*==============================================================================
 * Copyright 2021 IBM Corporation and others.
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
* org/omg/SecurityLevel2/ReceivedCredentialsPOA.java .
* Error reading Messages File.
* Error reading Messages File.
* Thursday, January 14, 2010 1:08:58 AM PST
*/


/* */
public abstract class ReceivedCredentialsPOA extends org.omg.PortableServer.Servant
 implements org.omg.SecurityLevel2.ReceivedCredentialsOperations, org.omg.CORBA.portable.InvokeHandler
{

  // Constructors

  private static java.util.Hashtable _methods = new java.util.Hashtable ();
  static
  {
    _methods.put ("_get_accepting_credentials", 0);
    _methods.put ("_get_association_options_used", 1);
    _methods.put ("_get_delegation_state", 2);
    _methods.put ("_get_delegation_mode", 3);
    _methods.put ("copy", 4);
    _methods.put ("destroy", 5);
    _methods.put ("_get_credentials_type", 6);
    _methods.put ("_get_authentication_state", 7);
    _methods.put ("_get_mechanism", 8);
    _methods.put ("_get_accepting_options_supported", 9);
    _methods.put ("_set_accepting_options_supported", 10);
    _methods.put ("_get_accepting_options_required", 11);
    _methods.put ("_set_accepting_options_required", 12);
    _methods.put ("_get_invocation_options_supported", 13);
    _methods.put ("_set_invocation_options_supported", 14);
    _methods.put ("_get_invocation_options_required", 15);
    _methods.put ("_set_invocation_options_required", 16);
    _methods.put ("get_security_feature", 17);
    _methods.put ("set_attributes", 18);
    _methods.put ("get_attributes", 19);
    _methods.put ("is_valid", 20);
    _methods.put ("refresh", 21);
  }

  public org.omg.CORBA.portable.OutputStream _invoke (String $method,
                                org.omg.CORBA.portable.InputStream in,
                                org.omg.CORBA.portable.ResponseHandler $rh)
  {
    org.omg.CORBA.portable.OutputStream out = null;
    java.lang.Integer __method = (java.lang.Integer)_methods.get ($method);
    if (__method == null)
      throw new org.omg.CORBA.BAD_OPERATION (0, org.omg.CORBA.CompletionStatus.COMPLETED_MAYBE);

    switch (__method.intValue ())
    {
       case 0:  // SecurityLevel2/ReceivedCredentials/_get_accepting_credentials
       {
         org.omg.SecurityLevel2.Credentials $result = null;
         $result = this.accepting_credentials ();
         out = $rh.createReply();
         org.omg.SecurityLevel2.CredentialsHelper.write (out, $result);
         break;
       }

       case 1:  // SecurityLevel2/ReceivedCredentials/_get_association_options_used
       {
         short $result = (short)0;
         $result = this.association_options_used ();
         out = $rh.createReply();
         out.write_ushort ($result);
         break;
       }

       case 2:  // SecurityLevel2/ReceivedCredentials/_get_delegation_state
       {
         org.omg.Security.DelegationState $result = null;
         $result = this.delegation_state ();
         out = $rh.createReply();
         org.omg.Security.DelegationStateHelper.write (out, $result);
         break;
       }

       case 3:  // SecurityLevel2/ReceivedCredentials/_get_delegation_mode
       {
         org.omg.Security.DelegationMode $result = null;
         $result = this.delegation_mode ();
         out = $rh.createReply();
         org.omg.Security.DelegationModeHelper.write (out, $result);
         break;
       }

       case 4:  // SecurityLevel2/Credentials/copy
       {
         org.omg.SecurityLevel2.Credentials $result = null;
         $result = this.copy ();
         out = $rh.createReply();
         org.omg.SecurityLevel2.CredentialsHelper.write (out, $result);
         break;
       }

       case 5:  // SecurityLevel2/Credentials/destroy
       {
         this.destroy ();
         out = $rh.createReply();
         break;
       }

       case 6:  // SecurityLevel2/Credentials/_get_credentials_type
       {
         org.omg.Security.InvocationCredentialsType $result = null;
         $result = this.credentials_type ();
         out = $rh.createReply();
         org.omg.Security.InvocationCredentialsTypeHelper.write (out, $result);
         break;
       }

       case 7:  // SecurityLevel2/Credentials/_get_authentication_state
       {
         org.omg.Security.AuthenticationStatus $result = null;
         $result = this.authentication_state ();
         out = $rh.createReply();
         org.omg.Security.AuthenticationStatusHelper.write (out, $result);
         break;
       }

       case 8:  // SecurityLevel2/Credentials/_get_mechanism
       {
         String $result = null;
         $result = this.mechanism ();
         out = $rh.createReply();
         out.write_string ($result);
         break;
       }

       case 9:  // SecurityLevel2/Credentials/_get_accepting_options_supported
       {
         short $result = (short)0;
         $result = this.accepting_options_supported ();
         out = $rh.createReply();
         out.write_ushort ($result);
         break;
       }

       case 10:  // SecurityLevel2/Credentials/_set_accepting_options_supported
       {
         short newAccepting_options_supported = org.omg.Security.AssociationOptionsHelper.read (in);
         this.accepting_options_supported (newAccepting_options_supported);
         out = $rh.createReply();
         break;
       }

       case 11:  // SecurityLevel2/Credentials/_get_accepting_options_required
       {
         short $result = (short)0;
         $result = this.accepting_options_required ();
         out = $rh.createReply();
         out.write_ushort ($result);
         break;
       }

       case 12:  // SecurityLevel2/Credentials/_set_accepting_options_required
       {
         short newAccepting_options_required = org.omg.Security.AssociationOptionsHelper.read (in);
         this.accepting_options_required (newAccepting_options_required);
         out = $rh.createReply();
         break;
       }

       case 13:  // SecurityLevel2/Credentials/_get_invocation_options_supported
       {
         short $result = (short)0;
         $result = this.invocation_options_supported ();
         out = $rh.createReply();
         out.write_ushort ($result);
         break;
       }

       case 14:  // SecurityLevel2/Credentials/_set_invocation_options_supported
       {
         short newInvocation_options_supported = org.omg.Security.AssociationOptionsHelper.read (in);
         this.invocation_options_supported (newInvocation_options_supported);
         out = $rh.createReply();
         break;
       }

       case 15:  // SecurityLevel2/Credentials/_get_invocation_options_required
       {
         short $result = (short)0;
         $result = this.invocation_options_required ();
         out = $rh.createReply();
         out.write_ushort ($result);
         break;
       }

       case 16:  // SecurityLevel2/Credentials/_set_invocation_options_required
       {
         short newInvocation_options_required = org.omg.Security.AssociationOptionsHelper.read (in);
         this.invocation_options_required (newInvocation_options_required);
         out = $rh.createReply();
         break;
       }

       case 17:  // SecurityLevel2/Credentials/get_security_feature
       {
         org.omg.Security.CommunicationDirection direction = org.omg.Security.CommunicationDirectionHelper.read (in);
         org.omg.Security.SecurityFeature feature = org.omg.Security.SecurityFeatureHelper.read (in);
         boolean $result = false;
         $result = this.get_security_feature (direction, feature);
         out = $rh.createReply();
         out.write_boolean ($result);
         break;
       }

       case 18:  // SecurityLevel2/Credentials/set_attributes
       {
         org.omg.Security.SecAttribute requested_attributes[] = org.omg.Security.AttributeListHelper.read (in);
         org.omg.Security.AttributeListHolder actual_attributes = new org.omg.Security.AttributeListHolder ();
         boolean $result = false;
         $result = this.set_attributes (requested_attributes, actual_attributes);
         out = $rh.createReply();
         out.write_boolean ($result);
         org.omg.Security.AttributeListHelper.write (out, actual_attributes.value);
         break;
       }

       case 19:  // SecurityLevel2/Credentials/get_attributes
       {
         org.omg.Security.AttributeType attributes[] = org.omg.Security.AttributeTypeListHelper.read (in);
         org.omg.Security.SecAttribute $result[] = null;
         $result = this.get_attributes (attributes);
         out = $rh.createReply();
         org.omg.Security.AttributeListHelper.write (out, $result);
         break;
       }

       case 20:  // SecurityLevel2/Credentials/is_valid
       {
         org.omg.TimeBase.UtcTHolder expiry_time = new org.omg.TimeBase.UtcTHolder ();
         boolean $result = false;
         $result = this.is_valid (expiry_time);
         out = $rh.createReply();
         out.write_boolean ($result);
         org.omg.Security.UtcTHelper.write (out, expiry_time.value);
         break;
       }

       case 21:  // SecurityLevel2/Credentials/refresh
       {
         org.omg.CORBA.Any refresh_data = in.read_any ();
         boolean $result = false;
         $result = this.refresh (refresh_data);
         out = $rh.createReply();
         out.write_boolean ($result);
         break;
       }

       default:
         throw new org.omg.CORBA.BAD_OPERATION (0, org.omg.CORBA.CompletionStatus.COMPLETED_MAYBE);
    }

    return out;
  } // _invoke

  // Type-specific CORBA::Object operations
  private static String[] __ids = {
    "IDL:SecurityLevel2/ReceivedCredentials:1.0", 
    "IDL:SecurityLevel2/Credentials:1.0"};

  public String[] _all_interfaces (org.omg.PortableServer.POA poa, byte[] objectId)
  {
    return (String[])__ids.clone ();
  }

  public ReceivedCredentials _this() 
  {
    return ReceivedCredentialsHelper.narrow(
    super._this_object());
  }

  public ReceivedCredentials _this(org.omg.CORBA.ORB orb) 
  {
    return ReceivedCredentialsHelper.narrow(
    super._this_object(orb));
  }


} // class ReceivedCredentialsPOA
