package org.omg.CosTransactions;


/**
* org/omg/CosTransactions/TerminatorPOA.java .
* Error reading Messages File.
* Error reading Messages File.
* Thursday, January 14, 2010 1:08:59 AM PST
*/

public abstract class TerminatorPOA extends org.omg.PortableServer.Servant
 implements org.omg.CosTransactions.TerminatorOperations, org.omg.CORBA.portable.InvokeHandler
{

  // Constructors

  private static java.util.Hashtable _methods = new java.util.Hashtable ();
  static
  {
    _methods.put ("commit", 0);
    _methods.put ("rollback", 1);
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
       case 0:  // CosTransactions/Terminator/commit
       {
         try {
           boolean report_heuristics = in.read_boolean ();
           this.commit (report_heuristics);
           out = $rh.createReply();
         } catch (org.omg.CosTransactions.HeuristicMixed $ex) {
           out = $rh.createExceptionReply ();
           org.omg.CosTransactions.HeuristicMixedHelper.write (out, $ex);
         } catch (org.omg.CosTransactions.HeuristicHazard $ex) {
           out = $rh.createExceptionReply ();
           org.omg.CosTransactions.HeuristicHazardHelper.write (out, $ex);
         }
         break;
       }

       case 1:  // CosTransactions/Terminator/rollback
       {
         this.rollback ();
         out = $rh.createReply();
         break;
       }

       default:
         throw new org.omg.CORBA.BAD_OPERATION (0, org.omg.CORBA.CompletionStatus.COMPLETED_MAYBE);
    }

    return out;
  } // _invoke

  // Type-specific CORBA::Object operations
  private static String[] __ids = {
    "IDL:CosTransactions/Terminator:1.0"};

  public String[] _all_interfaces (org.omg.PortableServer.POA poa, byte[] objectId)
  {
    return (String[])__ids.clone ();
  }

  public Terminator _this() 
  {
    return TerminatorHelper.narrow(
    super._this_object());
  }

  public Terminator _this(org.omg.CORBA.ORB orb) 
  {
    return TerminatorHelper.narrow(
    super._this_object(orb));
  }


} // class TerminatorPOA
