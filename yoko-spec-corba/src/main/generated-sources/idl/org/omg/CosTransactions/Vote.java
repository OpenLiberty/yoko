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
* org/omg/CosTransactions/Vote.java .
* Error reading Messages File.
* Error reading Messages File.
* Thursday, January 14, 2010 1:08:59 AM PST
*/

public class Vote implements org.omg.CORBA.portable.IDLEntity
{
  private        int __value;
  private static int __size = 3;
  private static org.omg.CosTransactions.Vote[] __array = new org.omg.CosTransactions.Vote [__size];

  public static final int _VoteCommit = 0;
  public static final org.omg.CosTransactions.Vote VoteCommit = new org.omg.CosTransactions.Vote(_VoteCommit);
  public static final int _VoteRollback = 1;
  public static final org.omg.CosTransactions.Vote VoteRollback = new org.omg.CosTransactions.Vote(_VoteRollback);
  public static final int _VoteReadOnly = 2;
  public static final org.omg.CosTransactions.Vote VoteReadOnly = new org.omg.CosTransactions.Vote(_VoteReadOnly);

  public int value ()
  {
    return __value;
  }

  public static org.omg.CosTransactions.Vote from_int (int value)
  {
    if (value >= 0 && value < __size)
      return __array[value];
    else
      throw new org.omg.CORBA.BAD_PARAM ();
  }

  protected Vote (int value)
  {
    __value = value;
    __array[__value] = this;
  }
} // class Vote
