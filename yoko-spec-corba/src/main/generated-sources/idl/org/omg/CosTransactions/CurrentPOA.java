package org.omg.CosTransactions;


/**
* org/omg/CosTransactions/CurrentPOA.java .
* Error reading Messages File.
* Error reading Messages File.
* Thursday, January 14, 2010 1:08:59 AM PST
*/


// Current transaction
public abstract class CurrentPOA extends org.omg.PortableServer.Servant
 implements org.omg.CosTransactions.CurrentOperations, org.omg.CORBA.portable.InvokeHandler
{

  // Constructors

  private static java.util.Hashtable _methods = new java.util.Hashtable ();
  static
  {
    _methods.put ("begin", 0);
    _methods.put ("commit", 1);
    _methods.put ("rollback", 2);
    _methods.put ("rollback_only", 3);
    _methods.put ("get_status", 4);
    _methods.put ("get_transaction_name", 5);
    _methods.put ("set_timeout", 6);
    _methods.put ("get_timeout", 7);
    _methods.put ("get_control", 8);
    _methods.put ("suspend", 9);
    _methods.put ("resume", 10);
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
       case 0:  // CosTransactions/Current/begin
       {
         try {
           this.begin ();
           out = $rh.createReply();
         } catch (org.omg.CosTransactions.SubtransactionsUnavailable $ex) {
           out = $rh.createExceptionReply ();
           org.omg.CosTransactions.SubtransactionsUnavailableHelper.write (out, $ex);
         }
         break;
       }

       case 1:  // CosTransactions/Current/commit
       {
         try {
           boolean report_heuristics = in.read_boolean ();
           this.commit (report_heuristics);
           out = $rh.createReply();
         } catch (org.omg.CosTransactions.NoTransaction $ex) {
           out = $rh.createExceptionReply ();
           org.omg.CosTransactions.NoTransactionHelper.write (out, $ex);
         } catch (org.omg.CosTransactions.HeuristicMixed $ex) {
           out = $rh.createExceptionReply ();
           org.omg.CosTransactions.HeuristicMixedHelper.write (out, $ex);
         } catch (org.omg.CosTransactions.HeuristicHazard $ex) {
           out = $rh.createExceptionReply ();
           org.omg.CosTransactions.HeuristicHazardHelper.write (out, $ex);
         }
         break;
       }

       case 2:  // CosTransactions/Current/rollback
       {
         try {
           this.rollback ();
           out = $rh.createReply();
         } catch (org.omg.CosTransactions.NoTransaction $ex) {
           out = $rh.createExceptionReply ();
           org.omg.CosTransactions.NoTransactionHelper.write (out, $ex);
         }
         break;
       }

       case 3:  // CosTransactions/Current/rollback_only
       {
         try {
           this.rollback_only ();
           out = $rh.createReply();
         } catch (org.omg.CosTransactions.NoTransaction $ex) {
           out = $rh.createExceptionReply ();
           org.omg.CosTransactions.NoTransactionHelper.write (out, $ex);
         }
         break;
       }

       case 4:  // CosTransactions/Current/get_status
       {
         org.omg.CosTransactions.Status $result = null;
         $result = this.get_status ();
         out = $rh.createReply();
         org.omg.CosTransactions.StatusHelper.write (out, $result);
         break;
       }

       case 5:  // CosTransactions/Current/get_transaction_name
       {
         String $result = null;
         $result = this.get_transaction_name ();
         out = $rh.createReply();
         out.write_string ($result);
         break;
       }

       case 6:  // CosTransactions/Current/set_timeout
       {
         int seconds = in.read_ulong ();
         this.set_timeout (seconds);
         out = $rh.createReply();
         break;
       }

       case 7:  // CosTransactions/Current/get_timeout
       {
         int $result = (int)0;
         $result = this.get_timeout ();
         out = $rh.createReply();
         out.write_ulong ($result);
         break;
       }

       case 8:  // CosTransactions/Current/get_control
       {
         org.omg.CosTransactions.Control $result = null;
         $result = this.get_control ();
         out = $rh.createReply();
         org.omg.CosTransactions.ControlHelper.write (out, $result);
         break;
       }

       case 9:  // CosTransactions/Current/suspend
       {
         org.omg.CosTransactions.Control $result = null;
         $result = this.suspend ();
         out = $rh.createReply();
         org.omg.CosTransactions.ControlHelper.write (out, $result);
         break;
       }

       case 10:  // CosTransactions/Current/resume
       {
         try {
           org.omg.CosTransactions.Control which = org.omg.CosTransactions.ControlHelper.read (in);
           this.resume (which);
           out = $rh.createReply();
         } catch (org.omg.CosTransactions.InvalidControl $ex) {
           out = $rh.createExceptionReply ();
           org.omg.CosTransactions.InvalidControlHelper.write (out, $ex);
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
    "IDL:CosTransactions/Current:1.0", 
    "IDL:CORBA/Current:1.0"};

  public String[] _all_interfaces (org.omg.PortableServer.POA poa, byte[] objectId)
  {
    return (String[])__ids.clone ();
  }

  public Current _this() 
  {
    return CurrentHelper.narrow(
    super._this_object());
  }

  public Current _this(org.omg.CORBA.ORB orb) 
  {
    return CurrentHelper.narrow(
    super._this_object(orb));
  }


} // class CurrentPOA
