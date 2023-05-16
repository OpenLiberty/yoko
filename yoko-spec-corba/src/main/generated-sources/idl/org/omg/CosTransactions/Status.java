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
* org/omg/CosTransactions/Status.java .
* Error reading Messages File.
* Error reading Messages File.
* Thursday, January 14, 2010 1:08:59 AM PST
*/


// DATATYPES
public class Status implements org.omg.CORBA.portable.IDLEntity
{
  private        int __value;
  private static int __size = 10;
  private static org.omg.CosTransactions.Status[] __array = new org.omg.CosTransactions.Status [__size];

  public static final int _StatusActive = 0;
  public static final org.omg.CosTransactions.Status StatusActive = new org.omg.CosTransactions.Status(_StatusActive);
  public static final int _StatusMarkedRollback = 1;
  public static final org.omg.CosTransactions.Status StatusMarkedRollback = new org.omg.CosTransactions.Status(_StatusMarkedRollback);
  public static final int _StatusPrepared = 2;
  public static final org.omg.CosTransactions.Status StatusPrepared = new org.omg.CosTransactions.Status(_StatusPrepared);
  public static final int _StatusCommitted = 3;
  public static final org.omg.CosTransactions.Status StatusCommitted = new org.omg.CosTransactions.Status(_StatusCommitted);
  public static final int _StatusRolledBack = 4;
  public static final org.omg.CosTransactions.Status StatusRolledBack = new org.omg.CosTransactions.Status(_StatusRolledBack);
  public static final int _StatusUnknown = 5;
  public static final org.omg.CosTransactions.Status StatusUnknown = new org.omg.CosTransactions.Status(_StatusUnknown);
  public static final int _StatusNoTransaction = 6;
  public static final org.omg.CosTransactions.Status StatusNoTransaction = new org.omg.CosTransactions.Status(_StatusNoTransaction);
  public static final int _StatusPreparing = 7;
  public static final org.omg.CosTransactions.Status StatusPreparing = new org.omg.CosTransactions.Status(_StatusPreparing);
  public static final int _StatusCommitting = 8;
  public static final org.omg.CosTransactions.Status StatusCommitting = new org.omg.CosTransactions.Status(_StatusCommitting);
  public static final int _StatusRollingBack = 9;
  public static final org.omg.CosTransactions.Status StatusRollingBack = new org.omg.CosTransactions.Status(_StatusRollingBack);

  public int value ()
  {
    return __value;
  }

  public static org.omg.CosTransactions.Status from_int (int value)
  {
    if (value >= 0 && value < __size)
      return __array[value];
    else
      throw new org.omg.CORBA.BAD_PARAM ();
  }

  protected Status (int value)
  {
    __value = value;
    __array[__value] = this;
  }
} // class Status
