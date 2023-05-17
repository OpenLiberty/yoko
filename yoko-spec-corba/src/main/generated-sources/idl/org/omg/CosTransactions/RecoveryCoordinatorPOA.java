/*==============================================================================
 * Copyright 2021 IBM Corporation and others.
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
* org/omg/CosTransactions/RecoveryCoordinatorPOA.java .
* Error reading Messages File.
* Error reading Messages File.
* Thursday, January 14, 2010 1:08:59 AM PST
*/

public abstract class RecoveryCoordinatorPOA extends org.omg.PortableServer.Servant
 implements org.omg.CosTransactions.RecoveryCoordinatorOperations, org.omg.CORBA.portable.InvokeHandler
{

  // Constructors

  private static java.util.Hashtable _methods = new java.util.Hashtable ();
  static
  {
    _methods.put ("replay_completion", 0);
  }

  public org.omg.CORBA.portable.OutputStream _invoke (String $method,
                                org.omg.CORBA.portable.InputStream in,
                                org.omg.CORBA.portable.ResponseHandler $rh)
  {
    org.omg.CORBA.portable.OutputStream out = null;
    java.lang.Integer __method = (java.lang.Integer)_methods.get ($method);
    if (__method == null)
      throw new org.omg.CORBA.BAD_OPERATION (0, org.omg.CORBA.CompletionStatus.COMPLETED_MAYBE);

    switch (__method.intValue ())
    {
       case 0:  // CosTransactions/RecoveryCoordinator/replay_completion
       {
         try {
           org.omg.CosTransactions.Resource r = org.omg.CosTransactions.ResourceHelper.read (in);
           org.omg.CosTransactions.Status $result = null;
           $result = this.replay_completion (r);
           out = $rh.createReply();
           org.omg.CosTransactions.StatusHelper.write (out, $result);
         } catch (org.omg.CosTransactions.NotPrepared $ex) {
           out = $rh.createExceptionReply ();
           org.omg.CosTransactions.NotPreparedHelper.write (out, $ex);
         }
         break;
       }

       default:
         throw new org.omg.CORBA.BAD_OPERATION (0, org.omg.CORBA.CompletionStatus.COMPLETED_MAYBE);
    }

    return out;
  } // _invoke

  // Type-specific CORBA::Object operations
  private static String[] __ids = {
    "IDL:CosTransactions/RecoveryCoordinator:1.0"};

  public String[] _all_interfaces (org.omg.PortableServer.POA poa, byte[] objectId)
  {
    return (String[])__ids.clone ();
  }

  public RecoveryCoordinator _this() 
  {
    return RecoveryCoordinatorHelper.narrow(
    super._this_object());
  }

  public RecoveryCoordinator _this(org.omg.CORBA.ORB orb) 
  {
    return RecoveryCoordinatorHelper.narrow(
    super._this_object(orb));
  }


} // class RecoveryCoordinatorPOA
