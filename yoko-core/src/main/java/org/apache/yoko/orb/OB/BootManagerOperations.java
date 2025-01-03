/*
 * Copyright 2024 IBM Corporation and others.
 *
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
package org.apache.yoko.orb.OB;

import org.apache.yoko.orb.OB.BootManagerPackage.AlreadyExists;
import org.apache.yoko.orb.OB.BootManagerPackage.NotFound;

//
// IDL:orb.yoko.apache.org/OB/BootManager:1.0
//
/**
 *
 * Interface to manage bootstrapping of objects.
 * 
 **/

public interface BootManagerOperations
{
    //
    // IDL:orb.yoko.apache.org/OB/BootManager/add_binding:1.0
    //
    /**
     *
     * Add a new binding to the internal table.
     *
     * @param oid The object id to bind.
     *
     * @param obj The object reference.
     *
     * @exception org.apache.yoko.orb.OB.BootManagerPackage.AlreadyExists Thrown if binding already exists.
     *
     **/

    void
    add_binding(byte[] oid,
                org.omg.CORBA.Object obj)
        throws AlreadyExists;

    //
    // IDL:orb.yoko.apache.org/OB/BootManager/remove_binding:1.0
    //
    /**
     *
     * Remove a binding from the internal table.
     *
     * @param oid The object id to remove.
     *
     * @exception org.apache.yoko.orb.OB.BootManagerPackage.NotFound Thrown if no binding found.
     *
     **/

    void
    remove_binding(byte[] oid)
        throws NotFound;

    //
    // IDL:orb.yoko.apache.org/OB/BootManager/set_locator:1.0
    //
    /**
     *
     * Set the BootLocator. The BootLocator is called when a binding
     * for an object id does not exist in the internal table.
     *
     * @param locator The BootLocator reference.
     *
     * @see BootLocator
     *
     **/

    void
    set_locator(BootLocator locator);
}
