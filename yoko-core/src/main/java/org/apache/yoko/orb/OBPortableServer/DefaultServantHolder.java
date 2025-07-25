/*
 * Copyright 2025 IBM Corporation and others.
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
package org.apache.yoko.orb.OBPortableServer;

import org.apache.yoko.orb.PortableServer.PoaCurrentImpl;
import org.omg.PortableServer.CurrentPackage.NoContext;
import org.omg.PortableServer.Servant;

import static org.apache.yoko.util.Assert.fail;

class DefaultServantHolder {
    private boolean destroyed_;

    Servant servant_;

    //
    // Destroy the default servant
    //
    synchronized void destroy() {
        servant_ = null;
    }

    //
    // Set the default servant
    //
    synchronized public void setDefaultServant(
            Servant servant) {
        servant_ = servant;
    }

    //
    // Retrieve the default servant
    //
    synchronized public Servant getDefaultServant() {
        return servant_;
    }

    //
    // Retrieve the ObjectId associated with the servant, if necessary
    //
    synchronized byte[] servantToId(Servant servant,
                                    PoaCurrentImpl poaCurrent) {
        if (servant != servant_)
            return null;

        if (poaCurrent._OB_inUpcall() && poaCurrent._OB_getServant() == servant) {
            try {
                return poaCurrent.get_object_id();
            } catch (NoContext ex) {
                throw fail(ex); // TODO:
                                                                    // Internal
                                                                    // error
            }
        }
        return null;
    }
}
