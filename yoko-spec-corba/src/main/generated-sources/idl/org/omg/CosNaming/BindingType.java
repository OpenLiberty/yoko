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
package org.omg.CosNaming;


/**
* org/omg/CosNaming/BindingType.java .
* Error reading Messages File.
* Error reading Messages File.
* Thursday, January 14, 2010 1:08:58 AM PST
*/


/**
   * Specifies whether the given binding is for a object (that is not a
   * naming context) or for a naming context.
   */
public class BindingType implements org.omg.CORBA.portable.IDLEntity
{
  private        int __value;
  private static int __size = 2;
  private static org.omg.CosNaming.BindingType[] __array = new org.omg.CosNaming.BindingType [__size];

  public static final int _nobject = 0;
  public static final org.omg.CosNaming.BindingType nobject = new org.omg.CosNaming.BindingType(_nobject);
  public static final int _ncontext = 1;
  public static final org.omg.CosNaming.BindingType ncontext = new org.omg.CosNaming.BindingType(_ncontext);

  public int value ()
  {
    return __value;
  }

  public static org.omg.CosNaming.BindingType from_int (int value)
  {
    if (value >= 0 && value < __size)
      return __array[value];
    else
      throw new org.omg.CORBA.BAD_PARAM ();
  }

  protected BindingType (int value)
  {
    __value = value;
    __array[__value] = this;
  }
} // class BindingType
