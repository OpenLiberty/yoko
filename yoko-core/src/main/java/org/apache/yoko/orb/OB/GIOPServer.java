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

import org.apache.yoko.orb.OB.GIOPServerStarter.ServerState;
import org.apache.yoko.orb.OCI.Acceptor;
import org.apache.yoko.util.Assert;

import static java.lang.System.identityHashCode;
import static java.util.logging.Logger.getLogger;
import static org.apache.yoko.orb.OB.GIOPServerStarter.ServerState.ACTIVE;
import static org.apache.yoko.orb.OB.GIOPServerStarter.ServerState.CLOSED;
import static org.apache.yoko.orb.OB.GIOPServerStarter.ServerState.HOLDING;
import static org.apache.yoko.util.Assert.ensure;

import java.util.logging.Logger;

final class GIOPServer extends Server {
    static final Logger logger = getLogger(GIOPServer.class.getName());
    protected ORBInstance orbInstance_; // The ORB Instance

    protected boolean destroy_; // True if destroy() was called

    protected Acceptor acceptor_; // The acceptor

    protected OAInterface oaInterface_; // The object adapter interface

    protected GIOPServerStarter starter_; // The server starter

    // ----------------------------------------------------------------------
    // GIOPServer private and protected member implementations
    // ----------------------------------------------------------------------

    protected void finalize() throws Throwable {
        ensure(destroy_);
        ensure(starter_ == null);

        super.finalize();
    }

    // ----------------------------------------------------------------------
    // GIOPServer package member implementations
    // ----------------------------------------------------------------------

    GIOPServer(ORBInstance orbInstance,
            Acceptor acceptor, OAInterface oaInterface,
            int concModel) {
        super(concModel);
        orbInstance_ = orbInstance;
        destroy_ = false;
        acceptor_ = acceptor;
        oaInterface_ = oaInterface;

        logger.fine(() -> "GIOPServer " + identityHashCode(this) + " created for orb instance " + orbInstance_.getOrbId() + " and server " + orbInstance_.getServerId() + " identityHash=" + identityHashCode(orbInstance_));

        try {
            switch (concModel_) {
            case Threaded:
                starter_ = new GIOPServerStarterThreaded(orbInstance_, acceptor_, oaInterface_);
                break;
            }
        } catch (RuntimeException ex) {
            destroy_ = true;
            throw ex;
        }
    }

    //
    // Destroy the server
    //
    public void destroy() {
        logger.fine(() -> "Destroying GIOPServer " + identityHashCode(this) + " started for orb instance " + orbInstance_.getOrbId() + " and server " + orbInstance_.getServerId() + identityHashCode(orbInstance_));
        // Don't destroy twice
        if (destroy_)
            return;

        // Set the destroy flag
        destroy_ = true;

        // Close and remove the starter
        ensure(starter_ != null);
        starter_.setState(CLOSED);
        starter_ = null;
    }

    // Hold any new requests that arrive for the Server
    public void hold() {
        logger.fine(() -> "Holding GIOPServer " + identityHashCode(this) + " started for orb instance " + orbInstance_.getOrbId() + " and server " + orbInstance_.getServerId() + identityHashCode(orbInstance_));
        ensure(!destroy_);
        ensure(starter_ != null);
        starter_.setState(HOLDING);
    }

    // Dispatch any requests that arrive for the Server
    public void activate() {
        logger.fine(() -> "Activating GIOPServer " + identityHashCode(this) + " started for orb instance " + orbInstance_.getOrbId() + " and server " + orbInstance_.getServerId() + identityHashCode(orbInstance_));
        ensure(!destroy_);
        ensure(starter_ != null);
        starter_.setState(ACTIVE);
    }

    // returns the GIOPServerStarter interface
    public GIOPServerStarter _OB_getGIOPServerStarter() {
        return starter_;
    }
}
