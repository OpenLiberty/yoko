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
* org/omg/Security/AttributeType.java .
* Error reading Messages File.
* Error reading Messages File.
* Thursday, January 14, 2010 1:08:58 AM PST
*/

public final class AttributeType implements org.omg.CORBA.portable.IDLEntity
{
  public org.omg.Security.ExtensibleFamily attribute_family = null;
  public int attribute_type = (int)0;

  public AttributeType ()
  {
  } // ctor

  public AttributeType (org.omg.Security.ExtensibleFamily _attribute_family, int _attribute_type)
  {
    attribute_family = _attribute_family;
    attribute_type = _attribute_type;
  } // ctor

} // class AttributeType
