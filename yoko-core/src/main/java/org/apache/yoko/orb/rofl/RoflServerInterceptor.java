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
package org.apache.yoko.orb.rofl;

import org.apache.yoko.io.SimplyCloseable;
import org.apache.yoko.orb.PortableInterceptor.ExtendedServerRequestInterceptor;
import org.apache.yoko.util.rofl.RoflHelper;
import org.omg.CORBA.LocalObject;
import org.omg.PortableInterceptor.ForwardRequest;
import org.omg.PortableInterceptor.ServerRequestInfo;

import static org.apache.yoko.util.ThreadLocalStack.ROFL_THREAD_LOCAL;

public class RoflServerInterceptor extends LocalObject implements ExtendedServerRequestInterceptor {
    private static final String NAME = RoflServerInterceptor.class.getName();
    private final RoflHelper roflHelper;

    public RoflServerInterceptor(int slotId) {
        this.roflHelper = new RoflHelper(slotId);
    }
    public void receive_request_service_contexts(ServerRequestInfo ri) throws ForwardRequest {
        ROFL_THREAD_LOCAL.reset();
        // Store in slot - will be retrieved after context switch in pre_unmarshal
        roflHelper.findAndSave(ri);
    }

    public void pre_unmarshal(ServerRequestInfo ri) {
        @SuppressWarnings("resource") // popped in post_marshal
        SimplyCloseable ignored = ROFL_THREAD_LOCAL.push(roflHelper.loadAndCreate(ri));
    }

    public void post_marshal(ServerRequestInfo ri) {
        ROFL_THREAD_LOCAL.pop();
    }

    public String name() { return NAME; }
}
