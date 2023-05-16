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
package org.omg.CosNaming;


/**
* org/omg/CosNaming/NamingContextOperations.java .
* Error reading Messages File.
* Error reading Messages File.
* Thursday, January 14, 2010 1:08:58 AM PST
*/


/** 
   * A naming context is an object that contains a set of name bindings in 
   * which each name is unique. Different names can be bound to an object 
   * in the same or different contexts at the same time. <p>
   * 
   * See <a href=" http://www.omg.org/corba/sectrans.htm#nam">CORBA COS 
   * Naming Specification.</a>
   */
public interface NamingContextOperations 
{

  /**
       * Creates a binding of a name and an object in the naming context.
       * Naming contexts that are bound using bind do not participate in name
       * resolution when compound names are passed to be resolved. 
       * 
       * @param n Name of the object <p>
       * 
       * @param obj The Object to bind with the given name<p>
       * 
       * @exception org.omg.CosNaming.NamingContextPackage.NotFound Indicates the name does not identify a binding.<p>
       * 
       * @exception org.omg.CosNaming.NamingContextPackage.CannotProceed Indicates that the implementation has
       * given up for some reason. The client, however, may be able to 
       * continue the operation at the returned naming context.<p>
       * 
       * @exception org.omg.CosNaming.NamingContextPackage.InvalidName Indicates that the name is invalid. <p>
       *
       * @exception org.omg.CosNaming.NamingContextPackage.AlreadyBound Indicates an object is already 
       * bound to the specified name.<p>
       */
  void bind (org.omg.CosNaming.NameComponent[] n, org.omg.CORBA.Object obj) throws org.omg.CosNaming.NamingContextPackage.NotFound, org.omg.CosNaming.NamingContextPackage.CannotProceed, org.omg.CosNaming.NamingContextPackage.InvalidName, org.omg.CosNaming.NamingContextPackage.AlreadyBound;

  /**
       * Names an object that is a naming context. Naming contexts that
       * are bound using bind_context() participate in name resolution 
       * when compound names are passed to be resolved.
       * 
       * @param n Name of the object <p>
       * 
       * @param nc NamingContect object to bind with the given name <p>
       * 
       * @exception org.omg.CosNaming.NamingContextPackage.NotFound Indicates the name does not identify a binding.<p>
       * 
       * @exception org.omg.CosNaming.NamingContextPackage.CannotProceed Indicates that the implementation has
       * given up for some reason. The client, however, may be able to 
       * continue the operation at the returned naming context.<p>
       * 
       * @exception org.omg.CosNaming.NamingContextPackage.InvalidName Indicates that the name is invalid. <p>
       *
       * @exception org.omg.CosNaming.NamingContextPackage.AlreadyBound Indicates an object is already 
       * bound to the specified name.<p>
       */
  void bind_context (org.omg.CosNaming.NameComponent[] n, org.omg.CosNaming.NamingContext nc) throws org.omg.CosNaming.NamingContextPackage.NotFound, org.omg.CosNaming.NamingContextPackage.CannotProceed, org.omg.CosNaming.NamingContextPackage.InvalidName, org.omg.CosNaming.NamingContextPackage.AlreadyBound;

  /**
       * Creates a binding of a name and an object in the naming context
       * even if the name is already bound in the context. Naming contexts 
       * that are bound using rebind do not participate in name resolution 
       * when compound names are passed to be resolved.
       * 
       * @param  n Name of the object <p>
       * 
       * @parm obj The Object to rebind with the given name <p>
       * 
       * @exception org.omg.CosNaming.NamingContextPackage.NotFound Indicates the name does not identify a binding.<p>
       * 
       * @exception org.omg.CosNaming.NamingContextPackage.CannotProceed Indicates that the implementation has
       * given up for some reason. The client, however, may be able to 
       * continue the operation at the returned naming context.<p>
       * 
       * @exception org.omg.CosNaming.NamingContextPackage.InvalidName Indicates that the name is invalid. <p>
       */
  void rebind (org.omg.CosNaming.NameComponent[] n, org.omg.CORBA.Object obj) throws org.omg.CosNaming.NamingContextPackage.NotFound, org.omg.CosNaming.NamingContextPackage.CannotProceed, org.omg.CosNaming.NamingContextPackage.InvalidName;

  /** 
       * Creates a binding of a name and a naming context in the naming
       * context even if the name is already bound in the context. Naming 
       * contexts that are bound using rebind_context() participate in name 
       * resolution when compound names are passed to be resolved.
       * 
       * @param n Name of the object <p>
       * 
       * @param nc NamingContect object to rebind with the given name <p>
       * 
       * @exception org.omg.CosNaming.NamingContextPackage.NotFound Indicates the name does not identify a binding.<p>
       * 
       * @exception org.omg.CosNaming.NamingContextPackage.CannotProceed Indicates that the implementation has
       * given up for some reason. The client, however, may be able to 
       * continue the operation at the returned naming context.<p>
       * 
       * @exception org.omg.CosNaming.NamingContextPackage.InvalidName Indicates that the name is invalid. <p>
       */
  void rebind_context (org.omg.CosNaming.NameComponent[] n, org.omg.CosNaming.NamingContext nc) throws org.omg.CosNaming.NamingContextPackage.NotFound, org.omg.CosNaming.NamingContextPackage.CannotProceed, org.omg.CosNaming.NamingContextPackage.InvalidName;

  /** 
       * The resolve operation is the process of retrieving an object
       * bound to a name in a given context. The given name must exactly 
       * match the bound name. The naming service does not return the type 
       * of the object. Clients are responsible for "narrowing" the object 
       * to the appropriate type. That is, clients typically cast the returned 
       * object from Object to a more specialized interface.
       * 
       * @param n Name of the object <p>
       * 
       * @exception org.omg.CosNaming.NamingContextPackage.NotFound Indicates the name does not identify a binding.<p>
       * 
       * @exception org.omg.CosNaming.NamingContextPackage.CannotProceed Indicates that the implementation has
       * given up for some reason. The client, however, may be able to 
       * continue the operation at the returned naming context.<p>
       * 
       * @exception org.omg.CosNaming.NamingContextPackage.InvalidName Indicates that the name is invalid. <p>
       */
  org.omg.CORBA.Object resolve (org.omg.CosNaming.NameComponent[] n) throws org.omg.CosNaming.NamingContextPackage.NotFound, org.omg.CosNaming.NamingContextPackage.CannotProceed, org.omg.CosNaming.NamingContextPackage.InvalidName;

  /** 
       * The unbind operation removes a name binding from a context.
       * 
       * @param n Name of the object <p>
       * 
       * @exception org.omg.CosNaming.NamingContextPackage.NotFound Indicates the name does not identify a binding.<p>
       * 
       * @exception org.omg.CosNaming.NamingContextPackage.CannotProceed Indicates that the implementation has
       * given up for some reason. The client, however, may be able to 
       * continue the operation at the returned naming context.<p>
       * 
       * @exception org.omg.CosNaming.NamingContextPackage.InvalidName Indicates that the name is invalid. <p>
       */
  void unbind (org.omg.CosNaming.NameComponent[] n) throws org.omg.CosNaming.NamingContextPackage.NotFound, org.omg.CosNaming.NamingContextPackage.CannotProceed, org.omg.CosNaming.NamingContextPackage.InvalidName;

  /**
       * The list operation allows a client to iterate through a set of
       * bindings in a naming context. <p>
       * 
       * The list operation returns at most the requested number of
       * bindings in BindingList bl. 
       * <ul>
       * <li>If the naming context contains additional 
       * bindings, the list operation returns a BindingIterator with the 
       * additional bindings. 
       * <li>If the naming context does not contain additional 
       * bindings, the binding iterator is a nil object reference.
       * </ul>
       * 
       * @param how_many the maximum number of bindings to return <p>
       * 
       * @param bl the returned list of bindings <p>
       * 
       * @param bi the returned binding iterator <p>
       */
  void list (int how_many, org.omg.CosNaming.BindingListHolder bl, org.omg.CosNaming.BindingIteratorHolder bi);

  /**
       * This operation returns a naming context implemented by the same
       * naming server as the context on which the operation was invoked. 
       * The new context is not bound to any name.
       */
  org.omg.CosNaming.NamingContext new_context ();

  /**
       * This operation creates a new context and binds it to the name
       * supplied as an argument. The newly-created context is implemented 
       * by the same naming server as the context in which it was bound (that 
       * is, the naming server that implements the context denoted by the 
       * name argument excluding the last component).
       * 
       * @param n Name of the object <p>
       * 
       * @exception org.omg.CosNaming.NamingContextPackage.NotFound Indicates the name does not identify a binding.<p>
       * 
       * @exception org.omg.CosNaming.NamingContextPackage.AlreadyBound Indicates an object is already 
       * bound to the specified name.<p>
       * 
       * @exception org.omg.CosNaming.NamingContextPackage.CannotProceed Indicates that the implementation has
       * given up for some reason. The client, however, may be able to 
       * continue the operation at the returned naming context.<p>
       * 
       * @exception org.omg.CosNaming.NamingContextPackage.InvalidName Indicates that the name is invalid. <p>
       */
  org.omg.CosNaming.NamingContext bind_new_context (org.omg.CosNaming.NameComponent[] n) throws org.omg.CosNaming.NamingContextPackage.NotFound, org.omg.CosNaming.NamingContextPackage.AlreadyBound, org.omg.CosNaming.NamingContextPackage.CannotProceed, org.omg.CosNaming.NamingContextPackage.InvalidName;

  /**
       * The destroy operation deletes a naming context. If the naming 
       * context contains bindings, the NotEmpty exception is raised.
       * 
       * @exception org.omg.CosNaming.NamingContextPackage.NotEmpty Indicates that the Naming Context contains bindings.
       */
  void destroy () throws org.omg.CosNaming.NamingContextPackage.NotEmpty;
} // interface NamingContextOperations
