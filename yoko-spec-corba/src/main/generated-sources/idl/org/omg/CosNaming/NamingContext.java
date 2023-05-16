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
* org/omg/CosNaming/NamingContext.java .
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
public interface NamingContext extends NamingContextOperations, org.omg.CORBA.Object, org.omg.CORBA.portable.IDLEntity 
{
} // interface NamingContext
