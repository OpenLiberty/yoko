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
* org/omg/CSIIOP/AS_ContextSec.java .
* Error reading Messages File.
* Error reading Messages File.
* Thursday, January 14, 2010 1:08:59 AM PST
*/

public final class AS_ContextSec implements org.omg.CORBA.portable.IDLEntity
{
  public short target_supports = (short)0;
  public short target_requires = (short)0;
  public byte client_authentication_mech[] = null;
  public byte target_name[] = null;

  public AS_ContextSec ()
  {
  } // ctor

  public AS_ContextSec (short _target_supports, short _target_requires, byte[] _client_authentication_mech, byte[] _target_name)
  {
    target_supports = _target_supports;
    target_requires = _target_requires;
    client_authentication_mech = _client_authentication_mech;
    target_name = _target_name;
  } // ctor

} // class AS_ContextSec
