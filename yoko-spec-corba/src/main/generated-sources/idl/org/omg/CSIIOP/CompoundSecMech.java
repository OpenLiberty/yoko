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
* org/omg/CSIIOP/CompoundSecMech.java .
* Error reading Messages File.
* Error reading Messages File.
* Thursday, January 14, 2010 1:08:59 AM PST
*/

public final class CompoundSecMech implements org.omg.CORBA.portable.IDLEntity
{
  public short target_requires = (short)0;
  public org.omg.IOP.TaggedComponent transport_mech = null;
  public org.omg.CSIIOP.AS_ContextSec as_context_mech = null;
  public org.omg.CSIIOP.SAS_ContextSec sas_context_mech = null;

  public CompoundSecMech ()
  {
  } // ctor

  public CompoundSecMech (short _target_requires, org.omg.IOP.TaggedComponent _transport_mech, org.omg.CSIIOP.AS_ContextSec _as_context_mech, org.omg.CSIIOP.SAS_ContextSec _sas_context_mech)
  {
    target_requires = _target_requires;
    transport_mech = _transport_mech;
    as_context_mech = _as_context_mech;
    sas_context_mech = _sas_context_mech;
  } // ctor

} // class CompoundSecMech
