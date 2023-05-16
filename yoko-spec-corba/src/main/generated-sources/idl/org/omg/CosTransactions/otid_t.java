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
* org/omg/CosTransactions/otid_t.java .
* Error reading Messages File.
* Error reading Messages File.
* Thursday, January 14, 2010 1:08:59 AM PST
*/

public final class otid_t implements org.omg.CORBA.portable.IDLEntity
{
  public int formatID = (int)0;

  /*format identifier. 0 is OSI TP */
  public int bqual_length = (int)0;
  public byte tid[] = null;

  public otid_t ()
  {
  } // ctor

  public otid_t (int _formatID, int _bqual_length, byte[] _tid)
  {
    formatID = _formatID;
    bqual_length = _bqual_length;
    tid = _tid;
  } // ctor

} // class otid_t
