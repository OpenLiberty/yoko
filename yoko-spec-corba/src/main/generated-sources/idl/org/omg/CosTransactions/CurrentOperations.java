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
* org/omg/CosTransactions/CurrentOperations.java .
* Error reading Messages File.
* Error reading Messages File.
* Thursday, January 14, 2010 1:08:59 AM PST
*/


// Current transaction
public interface CurrentOperations  extends org.omg.CORBA.CurrentOperations
{
  void begin () throws org.omg.CosTransactions.SubtransactionsUnavailable;
  void commit (boolean report_heuristics) throws org.omg.CosTransactions.NoTransaction, org.omg.CosTransactions.HeuristicMixed, org.omg.CosTransactions.HeuristicHazard;
  void rollback () throws org.omg.CosTransactions.NoTransaction;
  void rollback_only () throws org.omg.CosTransactions.NoTransaction;
  org.omg.CosTransactions.Status get_status ();
  String get_transaction_name ();
  void set_timeout (int seconds);
  int get_timeout ();
  org.omg.CosTransactions.Control get_control ();
  org.omg.CosTransactions.Control suspend ();
  void resume (org.omg.CosTransactions.Control which) throws org.omg.CosTransactions.InvalidControl;
} // interface CurrentOperations
