package org.omg.CSIIOP;

/**
* org/omg/CSIIOP/TLS_SEC_TRANSHolder.java .
* Error reading Messages File.
* Error reading Messages File.
* Thursday, January 14, 2010 1:08:59 AM PST
*/

public final class TLS_SEC_TRANSHolder implements org.omg.CORBA.portable.Streamable
{
  public org.omg.CSIIOP.TLS_SEC_TRANS value = null;

  public TLS_SEC_TRANSHolder ()
  {
  }

  public TLS_SEC_TRANSHolder (org.omg.CSIIOP.TLS_SEC_TRANS initialValue)
  {
    value = initialValue;
  }

  public void _read (org.omg.CORBA.portable.InputStream i)
  {
    value = org.omg.CSIIOP.TLS_SEC_TRANSHelper.read (i);
  }

  public void _write (org.omg.CORBA.portable.OutputStream o)
  {
    org.omg.CSIIOP.TLS_SEC_TRANSHelper.write (o, value);
  }

  public org.omg.CORBA.TypeCode _type ()
  {
    return org.omg.CSIIOP.TLS_SEC_TRANSHelper.type ();
  }

}
