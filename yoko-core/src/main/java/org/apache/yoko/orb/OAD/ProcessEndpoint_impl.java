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
package org.apache.yoko.orb.OAD;

import static org.omg.CORBA.SetOverrideType.SET_OVERRIDE;

import org.apache.yoko.orb.OAD.AlreadyLinked;
import org.apache.yoko.orb.OAD.ProcessEndpoint;
import org.apache.yoko.orb.OAD.ProcessEndpointManager;
import org.apache.yoko.orb.OAD.ProcessEndpointManagerHelper;
import org.apache.yoko.orb.OAD.ProcessEndpointPOA;
import org.apache.yoko.orb.OB.ORBControl;
import org.apache.yoko.orb.OB.RETRY_ALWAYS;
import org.apache.yoko.orb.OB.RetryPolicy_impl;
import org.omg.CORBA.Policy;
import org.omg.CORBA.SystemException;
import org.omg.PortableServer.POA;

final public class ProcessEndpoint_impl extends ProcessEndpointPOA {
    private String name_;

    private String id_;

    private Policy[] pl_;

    private POA poa_;

    private ORBControl orbControl_;

    public ProcessEndpoint_impl(String name, String id,
            POA poa,
            ORBControl orbControl) {
        name_ = name;
        id_ = id;
        poa_ = poa;
        orbControl_ = orbControl;

        //
        // Create a PolicyList for RETRY_ALWAYS
        //
        pl_ = new Policy[1];
        pl_[0] = new RetryPolicy_impl(
                RETRY_ALWAYS.value, 0, 1, false);
    }

    public void reestablish_link(ProcessEndpointManager d) {
        //
        // Set the retry policy on this object
        //
        org.omg.CORBA.Object obj = d._set_policy_override(pl_,
                SET_OVERRIDE);
        ProcessEndpointManager manager = ProcessEndpointManagerHelper
                .narrow(obj);

        ProcessEndpoint cb = _this();

        //
        // Establish a new link with the ProcessEndpointManager
        //
        try {
            manager.establish_link(name_, id_, 0xFFFFFFFF, cb);
        } catch (AlreadyLinked ex) {
        } catch (SystemException ex) {
            // logger.error("connect_server failed: " + ex);
        }
    }

    public void stop() {
        orbControl_.shutdownServer(false);
    }

    public POA _default_POA() {
        return poa_;
    }
}
