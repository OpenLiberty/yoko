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
package org.omg.Security;


/**
* org/omg/Security/ExtensibleFamily.java .
* Error reading Messages File.
* Error reading Messages File.
* Thursday, January 14, 2010 1:08:58 AM PST
*/

public final class ExtensibleFamily implements org.omg.CORBA.portable.IDLEntity
{
  public short family_definer = (short)0;
  public short family = (short)0;

  public ExtensibleFamily ()
  {
  } // ctor

  public ExtensibleFamily (short _family_definer, short _family)
  {
    family_definer = _family_definer;
    family = _family;
  } // ctor

} // class ExtensibleFamily
