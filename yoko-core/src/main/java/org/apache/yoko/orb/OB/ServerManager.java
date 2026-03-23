/*
 * Copyright 2026 IBM Corporation and others.
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

import org.apache.yoko.orb.OCI.Acceptor;
import org.apache.yoko.util.Assert;

import static java.util.logging.Logger.getLogger;

import java.util.Enumeration;
import java.util.Vector;
import java.util.logging.Logger;

public final class ServerManager {
    static final Logger logger = getLogger(ServerManager.class.getName());
    
    private boolean destroy_; // if destroy() was called

    private CollocatedServer collocatedServer_; // The collocated server

    private Vector allServers_ = new Vector(); // all other
                                                                    // servers

    // ----------------------------------------------------------------------
    // ServerManager private and protected member implementations
    // ----------------------------------------------------------------------

    protected void finalize() throws Throwable {
        Assert.ensure(destroy_);
        Assert.ensure(allServers_.isEmpty());
        Assert.ensure(collocatedServer_ == null);

        super.finalize();
    }

    // ----------------------------------------------------------------------
    // ServerManager public member implementations
    // ----------------------------------------------------------------------

    public ServerManager(ORBInstance orbInstance,
            Acceptor[] acceptors,
            OAInterface oaInterface, int concModel) {
        destroy_ = false;

        //
        // Create a server for each acceptor, and the collocated server
        //
        for (int i = 0; i < acceptors.length; i++) {
            GIOPServer server = new GIOPServer(orbInstance, acceptors[i],
                    oaInterface, concModel);
            allServers_.addElement(server);
        }
        collocatedServer_ = new CollocatedServer(oaInterface, concModel);
        allServers_.addElement(collocatedServer_);
    }

    public synchronized void destroy() {
        //
        // Don't destroy twice
        //
        if (destroy_)
            return;

        //
        // Set the destroy flag
        //
        destroy_ = true;

        //
        // Destroy all servers
        //
        Enumeration e = allServers_.elements();
        while (e.hasMoreElements())
            ((Server) e.nextElement()).destroy();

        allServers_.removeAllElements();
        collocatedServer_ = null;
    }

    public synchronized void hold() {
        logger.fine(() -> "Holding all servers");
        Enumeration e = allServers_.elements();
        while (e.hasMoreElements()) {
            ((Server) e.nextElement()).hold();
        }
    }

    public synchronized void activate() {
        logger.fine(() -> "Activating all servers");
        Enumeration e = allServers_.elements();
        while (e.hasMoreElements()) {
            ((Server) e.nextElement()).activate();
        }
    }

    public synchronized CollocatedServer getCollocatedServer() {
        return collocatedServer_;
    }

    public synchronized Server[] getServers() {
        Server[] servers = new Server[allServers_
                .size()];

        for (int i = 0; i < allServers_.size(); i++) {
            servers[i] = (Server) allServers_.elementAt(i);
        }
        return servers;
    }

}
