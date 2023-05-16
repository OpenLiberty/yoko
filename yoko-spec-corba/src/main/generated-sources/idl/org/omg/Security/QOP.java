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
* org/omg/Security/QOP.java .
* Error reading Messages File.
* Error reading Messages File.
* Thursday, January 14, 2010 1:08:58 AM PST
*/


// for an object reference and used to protect messages
public class QOP implements org.omg.CORBA.portable.IDLEntity
{
  private        int __value;
  private static int __size = 4;
  private static org.omg.Security.QOP[] __array = new org.omg.Security.QOP [__size];

  public static final int _SecQOPNoProtection = 0;
  public static final org.omg.Security.QOP SecQOPNoProtection = new org.omg.Security.QOP(_SecQOPNoProtection);
  public static final int _SecQOPIntegrity = 1;
  public static final org.omg.Security.QOP SecQOPIntegrity = new org.omg.Security.QOP(_SecQOPIntegrity);
  public static final int _SecQOPConfidentiality = 2;
  public static final org.omg.Security.QOP SecQOPConfidentiality = new org.omg.Security.QOP(_SecQOPConfidentiality);
  public static final int _SecQOPIntegrityAndConfidentiality = 3;
  public static final org.omg.Security.QOP SecQOPIntegrityAndConfidentiality = new org.omg.Security.QOP(_SecQOPIntegrityAndConfidentiality);

  public int value ()
  {
    return __value;
  }

  public static org.omg.Security.QOP from_int (int value)
  {
    if (value >= 0 && value < __size)
      return __array[value];
    else
      throw new org.omg.CORBA.BAD_PARAM ();
  }

  protected QOP (int value)
  {
    __value = value;
    __array[__value] = this;
  }
} // class QOP
