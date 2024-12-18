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
package org.apache.yoko.orb.IMR;

import org.omg.PortableInterceptor.ObjectReferenceTemplate;

//
// IDL:orb.yoko.apache.org/IMR/ActiveState:1.0
//
/***/

public interface ActiveStateOperations
{
    //
    // IDL:orb.yoko.apache.org/IMR/ActiveState/set_status:1.0
    //
    /**
     *
     * Called to update the server status
     *
     * @param id The server id
     * @param status The new server status
     *
     **/

    void
    set_status(String id,
               ServerStatus status);

    //
    // IDL:orb.yoko.apache.org/IMR/ActiveState/poa_create:1.0
    //
    /**
     *
     * Called when a POA is created
     *
     * @param state The POA state
     * @param poa_tmpl The POAs ORT
     * @return The IMRs ORT
     *
     * @exception org.apache.yoko.orb.IMR._NoSuchPOA If a record for the POA does not exist
     *
     **/

    ObjectReferenceTemplate
    poa_create(POAStatus state,
               ObjectReferenceTemplate poa_tmpl)
        throws _NoSuchPOA;

    //
    // IDL:orb.yoko.apache.org/IMR/ActiveState/poa_status_update:1.0
    //
    /**
     *
     * Called when POA Manager changes state.
     *
     * @param poas List of affected POAs
     * @param state The POA Manager state
     *
     **/

    void
    poa_status_update(String[][] poas,
                      POAStatus state);
}
