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
* org/omg/Security/SecAttribute.java .
* Error reading Messages File.
* Error reading Messages File.
* Thursday, January 14, 2010 1:08:58 AM PST
*/

public final class SecAttribute implements org.omg.CORBA.portable.IDLEntity
{
  public org.omg.Security.AttributeType attribute_type = null;
  public byte defining_authority[] = null;
  public byte value[] = null;

  public SecAttribute ()
  {
  } // ctor

  public SecAttribute (org.omg.Security.AttributeType _attribute_type, byte[] _defining_authority, byte[] _value)
  {
    attribute_type = _attribute_type;
    defining_authority = _defining_authority;
    value = _value;
  } // ctor

} // class SecAttribute
