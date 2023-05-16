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
package org.omg.CosTransactions;


/**
* org/omg/CosTransactions/TransIdentity.java .
* Error reading Messages File.
* Error reading Messages File.
* Thursday, January 14, 2010 1:08:59 AM PST
*/

public final class TransIdentity implements org.omg.CORBA.portable.IDLEntity
{
  public org.omg.CosTransactions.Coordinator coord = null;
  public org.omg.CosTransactions.Terminator term = null;
  public org.omg.CosTransactions.otid_t otid = null;

  public TransIdentity ()
  {
  } // ctor

  public TransIdentity (org.omg.CosTransactions.Coordinator _coord, org.omg.CosTransactions.Terminator _term, org.omg.CosTransactions.otid_t _otid)
  {
    coord = _coord;
    term = _term;
    otid = _otid;
  } // ctor

} // class TransIdentity
