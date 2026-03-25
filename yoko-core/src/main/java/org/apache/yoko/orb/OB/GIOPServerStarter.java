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
import org.apache.yoko.orb.OCI.ConnectorInfo;
import org.apache.yoko.orb.OCI.Transport;
import org.apache.yoko.util.Assert;
import org.omg.CORBA.SystemException;

import java.util.Vector;
import java.util.logging.Logger;

import static org.apache.yoko.logging.VerboseLogging.CONN_IN_LOG;
import static org.apache.yoko.orb.OB.GIOPServerStarter.ServerState.CLOSED;
import static org.apache.yoko.orb.OB.GIOPServerStarter.ServerState.HOLDING;

abstract class GIOPServerStarter {
    static final Logger logger = Logger.getLogger(GIOPServerStarter.class.getName());
    
    protected final ORBInstance orbInstance_; // The ORBInstance

    protected final Acceptor acceptor_; // The acceptor

    protected final OAInterface oaInterface_; // The OA interface

    protected final Vector<GIOPConnection> connections_ = new Vector<>(); // Workers

    enum ServerState {
        ACTIVE,
        HOLDING,
        CLOSED;
        public boolean cannotTransitionTo(ServerState next) {
            if (this == next) return true;
            if (this == HOLDING) return false;
            return this.compareTo(next) > 0;
        }
    }

    protected ServerState serverState;

    // ----------------------------------------------------------------------
    // GIOPServer private and protected member implementation
    // ----------------------------------------------------------------------

    protected void finalize() throws Throwable {
        Assert.ensure(serverState == CLOSED);

        super.finalize();
    }

    //
    // Emit a trace message when closing the acceptor
    //
    protected void logCloseAcceptor() {
        CONN_IN_LOG.fine(() -> "stopped accepting connections\n" + acceptor_.get_info().describe());
    }

    protected void reapWorkers() {
        for (int i = 0; i < connections_.size();) {
            GIOPConnection connection = connections_.elementAt(i);
            if (connection.destroyed())
                connections_.removeElementAt(i);
            else
                ++i;
        }
    }

    // ----------------------------------------------------------------------
    // GIOPServer public member implementation
    // ----------------------------------------------------------------------

    GIOPServerStarter(ORBInstance orbInstance,
            Acceptor acceptor, OAInterface oaInterface) {
        orbInstance_ = orbInstance;
        acceptor_ = acceptor;
        oaInterface_ = oaInterface;
        serverState = HOLDING; // Must be holding initially

        try {
            // Trace acceptor creation
            CONN_IN_LOG.fine(() -> "accepting connections\n" + acceptor_.get_info().describe());

            // Start listening
            acceptor_.listen();
        } catch (SystemException ex) {
            acceptor_.close();
            serverState = CLOSED;
            throw ex;
        }
    }

    // given a host/port this will search the workers of this
    // GIOPServerStarter for an inbound connection transport
    // which matches the specific connection information.
    // It returns null if not found.
    public synchronized GIOPConnection getMatchingConnection(ConnectorInfo connInfo) {
        // reap the workers first since we don't want to return a destroyed transport
        reapWorkers();

        // iterate the workers
        for (int i = 0; i < connections_.size(); i++) {
            GIOPConnection worker = connections_.elementAt(i);

            // we only want to find inbound connections
            if (worker.isOutbound())
                continue;

            Transport transport = worker.transport();

            if (transport != null && transport.get_info().endpoint_alias_match(connInfo))
                return worker;
        }

        // we never found a match for the transport
        return null;
    }

    public abstract void setState(ServerState state);
}
