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
* org/omg/CSIIOP/SECIOP_SEC_TRANS.java .
* Error reading Messages File.
* Error reading Messages File.
* Thursday, January 14, 2010 1:08:59 AM PST
*/

public final class SECIOP_SEC_TRANS implements org.omg.CORBA.portable.IDLEntity
{
  public short target_supports = (short)0;
  public short target_requires = (short)0;
  public byte mech_oid[] = null;
  public byte target_name[] = null;
  public org.omg.CSIIOP.TransportAddress addresses[] = null;

  public SECIOP_SEC_TRANS ()
  {
  } // ctor

  public SECIOP_SEC_TRANS (short _target_supports, short _target_requires, byte[] _mech_oid, byte[] _target_name, org.omg.CSIIOP.TransportAddress[] _addresses)
  {
    target_supports = _target_supports;
    target_requires = _target_requires;
    mech_oid = _mech_oid;
    target_name = _target_name;
    addresses = _addresses;
  } // ctor

} // class SECIOP_SEC_TRANS
