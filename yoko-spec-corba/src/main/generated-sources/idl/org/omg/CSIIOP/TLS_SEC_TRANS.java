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
* org/omg/CSIIOP/TLS_SEC_TRANS.java .
* Error reading Messages File.
* Error reading Messages File.
* Thursday, January 14, 2010 1:08:59 AM PST
*/

public final class TLS_SEC_TRANS implements org.omg.CORBA.portable.IDLEntity
{
  public short target_supports = (short)0;
  public short target_requires = (short)0;
  public org.omg.CSIIOP.TransportAddress addresses[] = null;

  public TLS_SEC_TRANS ()
  {
  } // ctor

  public TLS_SEC_TRANS (short _target_supports, short _target_requires, org.omg.CSIIOP.TransportAddress[] _addresses)
  {
    target_supports = _target_supports;
    target_requires = _target_requires;
    addresses = _addresses;
  } // ctor

} // class TLS_SEC_TRANS
