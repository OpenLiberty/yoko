/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an \"AS IS\" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.omg.PortableInterceptor;

//
// IDL:omg.org/PortableInterceptor/Current:1.0
//
/***/

public interface CurrentOperations extends org.omg.CORBA.CurrentOperations
{
    //
    // IDL:omg.org/PortableInterceptor/Current/get_slot:1.0
    //
    /***/

    org.omg.CORBA.Any
    get_slot(int id)
        throws InvalidSlot;

    //
    // IDL:omg.org/PortableInterceptor/Current/set_slot:1.0
    //
    /***/

    void
    set_slot(int id,
             org.omg.CORBA.Any data)
        throws InvalidSlot;
}
