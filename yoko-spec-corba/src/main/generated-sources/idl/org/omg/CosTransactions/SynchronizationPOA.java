package org.omg.CosTransactions;


/**
* org/omg/CosTransactions/SynchronizationPOA.java .
* Error reading Messages File.
* Error reading Messages File.
* Thursday, January 14, 2010 1:08:59 AM PST
*/


// Inheritance from TransactionalObject is for backward compatability //
public abstract class SynchronizationPOA extends org.omg.PortableServer.Servant
 implements org.omg.CosTransactions.SynchronizationOperations, org.omg.CORBA.portable.InvokeHandler
{

  // Constructors

  private static java.util.Hashtable _methods = new java.util.Hashtable ();
  static
  {
    _methods.put ("before_completion", 0);
    _methods.put ("after_completion", 1);
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
       case 0:  // CosTransactions/Synchronization/before_completion
       {
         this.before_completion ();
         out = $rh.createReply();
         break;
       }

       case 1:  // CosTransactions/Synchronization/after_completion
       {
         org.omg.CosTransactions.Status s = org.omg.CosTransactions.StatusHelper.read (in);
         this.after_completion (s);
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
    "IDL:CosTransactions/Synchronization:1.0", 
    "IDL:CosTransactions/TransactionalObject:1.0"};

  public String[] _all_interfaces (org.omg.PortableServer.POA poa, byte[] objectId)
  {
    return (String[])__ids.clone ();
  }

  public Synchronization _this() 
  {
    return SynchronizationHelper.narrow(
    super._this_object());
  }

  public Synchronization _this(org.omg.CORBA.ORB orb) 
  {
    return SynchronizationHelper.narrow(
    super._this_object(orb));
  }


} // class SynchronizationPOA
