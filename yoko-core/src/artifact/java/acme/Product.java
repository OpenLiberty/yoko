/*==============================================================================
 * Copyright 2022, 2023 IBM Corporation and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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
package acme;


/**
* acme/Product.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from ../idl/acme.idl
* Wednesday, 10 May 2023 17:36:07 o'clock BST
*/

public final class Product implements org.omg.CORBA.portable.IDLEntity
{
  public String name = null;
  public float price = (float)0;

  public Product ()
  {
  } // ctor

  public Product (String _name, float _price)
  {
    name = _name;
    price = _price;
  } // ctor

} // class Product
