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
package org.omg.CSI;


/**
* org/omg/CSI/EstablishContext.java .
* Error reading Messages File.
* Error reading Messages File.
* Thursday, January 14, 2010 1:08:58 AM PST
*/

public final class EstablishContext implements org.omg.CORBA.portable.IDLEntity
{
  public long client_context_id = (long)0;
  public org.omg.CSI.AuthorizationElement authorization_token[] = null;
  public org.omg.CSI.IdentityToken identity_token = null;
  public byte client_authentication_token[] = null;

  public EstablishContext ()
  {
  } // ctor

  public EstablishContext (long _client_context_id, org.omg.CSI.AuthorizationElement[] _authorization_token, org.omg.CSI.IdentityToken _identity_token, byte[] _client_authentication_token)
  {
    client_context_id = _client_context_id;
    authorization_token = _authorization_token;
    identity_token = _identity_token;
    client_authentication_token = _client_authentication_token;
  } // ctor

} // class EstablishContext
