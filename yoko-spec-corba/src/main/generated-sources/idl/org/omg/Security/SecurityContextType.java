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
* org/omg/Security/SecurityContextType.java .
* Error reading Messages File.
* Error reading Messages File.
* Thursday, January 14, 2010 1:08:58 AM PST
*/


// Type of SecurityContext
public class SecurityContextType implements org.omg.CORBA.portable.IDLEntity
{
  private        int __value;
  private static int __size = 2;
  private static org.omg.Security.SecurityContextType[] __array = new org.omg.Security.SecurityContextType [__size];

  public static final int _SecClientSecurityContext = 0;
  public static final org.omg.Security.SecurityContextType SecClientSecurityContext = new org.omg.Security.SecurityContextType(_SecClientSecurityContext);
  public static final int _SecServerSecurityContext = 1;
  public static final org.omg.Security.SecurityContextType SecServerSecurityContext = new org.omg.Security.SecurityContextType(_SecServerSecurityContext);

  public int value ()
  {
    return __value;
  }

  public static org.omg.Security.SecurityContextType from_int (int value)
  {
    if (value >= 0 && value < __size)
      return __array[value];
    else
      throw new org.omg.CORBA.BAD_PARAM ();
  }

  protected SecurityContextType (int value)
  {
    __value = value;
    __array[__value] = this;
  }
} // class SecurityContextType
