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
* org/omg/Security/DelegationMode.java .
* Error reading Messages File.
* Error reading Messages File.
* Thursday, January 14, 2010 1:08:58 AM PST
*/


// Delegation mode which can be administered
public class DelegationMode implements org.omg.CORBA.portable.IDLEntity
{
  private        int __value;
  private static int __size = 3;
  private static org.omg.Security.DelegationMode[] __array = new org.omg.Security.DelegationMode [__size];

  public static final int _SecDelModeNoDelegation = 0;
  public static final org.omg.Security.DelegationMode SecDelModeNoDelegation = new org.omg.Security.DelegationMode(_SecDelModeNoDelegation);
  public static final int _SecDelModeSimpleDelegation = 1;
  public static final org.omg.Security.DelegationMode SecDelModeSimpleDelegation = new org.omg.Security.DelegationMode(_SecDelModeSimpleDelegation);
  public static final int _SecDelModeCompositeDelegation = 2;
  public static final org.omg.Security.DelegationMode SecDelModeCompositeDelegation = new org.omg.Security.DelegationMode(_SecDelModeCompositeDelegation);

  public int value ()
  {
    return __value;
  }

  public static org.omg.Security.DelegationMode from_int (int value)
  {
    if (value >= 0 && value < __size)
      return __array[value];
    else
      throw new org.omg.CORBA.BAD_PARAM ();
  }

  protected DelegationMode (int value)
  {
    __value = value;
    __array[__value] = this;
  }
} // class DelegationMode
