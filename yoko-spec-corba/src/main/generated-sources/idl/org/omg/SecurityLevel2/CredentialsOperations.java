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
* org/omg/SecurityLevel2/CredentialsOperations.java .
* Error reading Messages File.
* Error reading Messages File.
* Thursday, January 14, 2010 1:08:58 AM PST
*/


/* */
public interface CredentialsOperations 
{
  org.omg.SecurityLevel2.Credentials copy ();
  void destroy ();
  org.omg.Security.InvocationCredentialsType credentials_type ();
  org.omg.Security.AuthenticationStatus authentication_state ();
  String mechanism ();
  short accepting_options_supported ();
  void accepting_options_supported (short newAccepting_options_supported);
  short accepting_options_required ();
  void accepting_options_required (short newAccepting_options_required);
  short invocation_options_supported ();
  void invocation_options_supported (short newInvocation_options_supported);
  short invocation_options_required ();
  void invocation_options_required (short newInvocation_options_required);
  boolean get_security_feature (org.omg.Security.CommunicationDirection direction, org.omg.Security.SecurityFeature feature);
  boolean set_attributes (org.omg.Security.SecAttribute[] requested_attributes, org.omg.Security.AttributeListHolder actual_attributes);
  org.omg.Security.SecAttribute[] get_attributes (org.omg.Security.AttributeType[] attributes);
  boolean is_valid (org.omg.TimeBase.UtcTHolder expiry_time);
  boolean refresh (org.omg.CORBA.Any refresh_data);
} // interface CredentialsOperations
