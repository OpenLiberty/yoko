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
* org/omg/CosTransactions/PropagationContext.java .
* Error reading Messages File.
* Error reading Messages File.
* Thursday, January 14, 2010 1:08:59 AM PST
*/

public final class PropagationContext implements org.omg.CORBA.portable.IDLEntity
{
  public int timeout = (int)0;
  public org.omg.CosTransactions.TransIdentity current = null;
  public org.omg.CosTransactions.TransIdentity parents[] = null;
  public org.omg.CORBA.Any implementation_specific_data = null;

  public PropagationContext ()
  {
  } // ctor

  public PropagationContext (int _timeout, org.omg.CosTransactions.TransIdentity _current, org.omg.CosTransactions.TransIdentity[] _parents, org.omg.CORBA.Any _implementation_specific_data)
  {
    timeout = _timeout;
    current = _current;
    parents = _parents;
    implementation_specific_data = _implementation_specific_data;
  } // ctor

} // class PropagationContext
