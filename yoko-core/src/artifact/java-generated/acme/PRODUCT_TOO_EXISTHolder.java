package acme;

/**
* acme/PRODUCT_TOO_EXISTHolder.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from ../idl/acme.idl
* Thursday, 25 May 2023 13:10:15 o'clock BST
*/

public final class PRODUCT_TOO_EXISTHolder implements org.omg.CORBA.portable.Streamable
{
  public acme.PRODUCT_TOO_EXIST value = null;

  public PRODUCT_TOO_EXISTHolder ()
  {
  }

  public PRODUCT_TOO_EXISTHolder (acme.PRODUCT_TOO_EXIST initialValue)
  {
    value = initialValue;
  }

  public void _read (org.omg.CORBA.portable.InputStream i)
  {
    value = acme.PRODUCT_TOO_EXISTHelper.read (i);
  }

  public void _write (org.omg.CORBA.portable.OutputStream o)
  {
    acme.PRODUCT_TOO_EXISTHelper.write (o, value);
  }

  public org.omg.CORBA.TypeCode _type ()
  {
    return acme.PRODUCT_TOO_EXISTHelper.type ();
  }

}
