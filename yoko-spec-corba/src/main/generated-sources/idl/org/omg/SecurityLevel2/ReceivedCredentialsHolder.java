package org.omg.SecurityLevel2;

/**
* org/omg/SecurityLevel2/ReceivedCredentialsHolder.java .
* Error reading Messages File.
* Error reading Messages File.
* Thursday, January 14, 2010 1:08:58 AM PST
*/


/* */
public final class ReceivedCredentialsHolder implements org.omg.CORBA.portable.Streamable
{
  public org.omg.SecurityLevel2.ReceivedCredentials value = null;

  public ReceivedCredentialsHolder ()
  {
  }

  public ReceivedCredentialsHolder (org.omg.SecurityLevel2.ReceivedCredentials initialValue)
  {
    value = initialValue;
  }

  public void _read (org.omg.CORBA.portable.InputStream i)
  {
    value = org.omg.SecurityLevel2.ReceivedCredentialsHelper.read (i);
  }

  public void _write (org.omg.CORBA.portable.OutputStream o)
  {
    org.omg.SecurityLevel2.ReceivedCredentialsHelper.write (o, value);
  }

  public org.omg.CORBA.TypeCode _type ()
  {
    return org.omg.SecurityLevel2.ReceivedCredentialsHelper.type ();
  }

}
