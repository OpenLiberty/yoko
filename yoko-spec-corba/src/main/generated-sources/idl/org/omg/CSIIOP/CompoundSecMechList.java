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
* org/omg/CSIIOP/CompoundSecMechList.java .
* Error reading Messages File.
* Error reading Messages File.
* Thursday, January 14, 2010 1:08:59 AM PST
*/

public final class CompoundSecMechList implements org.omg.CORBA.portable.IDLEntity
{
  public boolean stateful = false;
  public org.omg.CSIIOP.CompoundSecMech mechanism_list[] = null;

  public CompoundSecMechList ()
  {
  } // ctor

  public CompoundSecMechList (boolean _stateful, org.omg.CSIIOP.CompoundSecMech[] _mechanism_list)
  {
    stateful = _stateful;
    mechanism_list = _mechanism_list;
  } // ctor

} // class CompoundSecMechList
