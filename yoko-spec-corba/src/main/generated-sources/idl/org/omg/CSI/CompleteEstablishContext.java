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
* org/omg/CSI/CompleteEstablishContext.java .
* Error reading Messages File.
* Error reading Messages File.
* Thursday, January 14, 2010 1:08:58 AM PST
*/

public final class CompleteEstablishContext implements org.omg.CORBA.portable.IDLEntity
{
  public long client_context_id = (long)0;
  public boolean context_stateful = false;
  public byte final_context_token[] = null;

  public CompleteEstablishContext ()
  {
  } // ctor

  public CompleteEstablishContext (long _client_context_id, boolean _context_stateful, byte[] _final_context_token)
  {
    client_context_id = _client_context_id;
    context_stateful = _context_stateful;
    final_context_token = _final_context_token;
  } // ctor

} // class CompleteEstablishContext
