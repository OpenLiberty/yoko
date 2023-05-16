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
* org/omg/SecurityLevel2/SecurityManagerOperations.java .
* Error reading Messages File.
* Error reading Messages File.
* Thursday, January 14, 2010 1:08:59 AM PST
*/


/* */
public interface SecurityManagerOperations 
{

  // Process/Capsule/ORB Instance specific operations
  org.omg.Security.MechandOptions[] supported_mechanisms ();
  org.omg.SecurityLevel2.Credentials[] own_credentials ();
  org.omg.SecurityLevel2.RequiredRights required_rights_object ();
  org.omg.SecurityLevel2.PrincipalAuthenticator principal_authenticator ();
  org.omg.SecurityLevel2.AccessDecision access_decision ();
  org.omg.SecurityLevel2.AuditDecision audit_decision ();
  org.omg.SecurityLevel2.TargetCredentials get_target_credentials (org.omg.CORBA.Object obj_ref);
  void remove_own_credentials (org.omg.SecurityLevel2.Credentials creds);
  org.omg.CORBA.Policy get_security_policy (int policy_type);
} // interface SecurityManagerOperations
