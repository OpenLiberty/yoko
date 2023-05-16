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
* org/omg/CSI/AuthorizationElement.java .
* Error reading Messages File.
* Error reading Messages File.
* Thursday, January 14, 2010 1:08:58 AM PST
*/

public final class AuthorizationElement implements org.omg.CORBA.portable.IDLEntity
{
  public int the_type = (int)0;
  public byte the_element[] = null;

  public AuthorizationElement ()
  {
  } // ctor

  public AuthorizationElement (int _the_type, byte[] _the_element)
  {
    the_type = _the_type;
    the_element = _the_element;
  } // ctor

} // class AuthorizationElement
