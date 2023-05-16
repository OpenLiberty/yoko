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
* org/omg/SecurityLevel2/PrincipalAuthenticatorOperations.java .
* Error reading Messages File.
* Error reading Messages File.
* Thursday, January 14, 2010 1:08:58 AM PST
*/


/* */
public interface PrincipalAuthenticatorOperations 
{
  int[] get_supported_authen_methods (String mechanism);
  org.omg.Security.AuthenticationStatus authenticate (int method, String mechanism, String security_name, org.omg.CORBA.Any auth_data, org.omg.Security.SecAttribute[] privileges, org.omg.SecurityLevel2.CredentialsHolder creds, org.omg.CORBA.AnyHolder continuation_data, org.omg.CORBA.AnyHolder auth_specific_data);
  org.omg.Security.AuthenticationStatus continue_authentication (org.omg.CORBA.Any response_data, org.omg.SecurityLevel2.Credentials creds, org.omg.CORBA.AnyHolder continuation_data, org.omg.CORBA.AnyHolder auth_specific_data);
} // interface PrincipalAuthenticatorOperations
