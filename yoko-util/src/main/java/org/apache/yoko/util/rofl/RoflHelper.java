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
package org.apache.yoko.util.rofl;

import org.apache.yoko.util.rofl.Rofl.RemoteOrb;
import org.omg.CORBA.Any;
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.INTERNAL;
import org.omg.CORBA.ORB;
import org.omg.IOP.ServiceContext;
import org.omg.IOP.ServiceContextHelper;
import org.omg.IOP.TaggedComponent;
import org.omg.PortableInterceptor.ClientRequestInfo;
import org.omg.PortableInterceptor.InvalidSlot;
import org.omg.PortableInterceptor.ServerRequestInfo;

import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static org.apache.yoko.util.MinorCodes.MinorInvalidComponentId;
import static org.apache.yoko.util.MinorCodes.MinorInvalidServiceContextId;
import static org.apache.yoko.util.rofl.Rofl.RemoteOrb.BAD;
import static org.apache.yoko.util.rofl.Rofl.RemoteOrb.IBM;

public class RoflHelper {
    private static final Logger logger = Logger.getLogger(RoflHelper.class.getName());
    private final int slotId;

    public RoflHelper(int slotId) {
        this.slotId = slotId;
    }

    public void findAndSave(ServerRequestInfo requestInfo) {
        // iterate through the known RemoteOrbs
        RemoteOrb.KNOWN_REMOTE_ORBS.stream()
                // look up the service context
                .map(remoteOrb -> {
                    try {
                        return requestInfo.get_request_service_context(remoteOrb.serviceContextId);
                    } catch (BAD_PARAM e) {
                        // if it just wasn't present, hand back a null
                        if (e.minor == MinorInvalidServiceContextId) return null;
                        // any other error should be propagated
                        throw e;
                    }
                })
                // ignore any RemoteOrbs whose service context was not found
                .filter(Objects::nonNull)
                // take the first RemoteOrb (in our ordering) whose service context was present
                .findFirst()
                // if there was one, save its service context into the ROFL slot.
                .ifPresent(sc -> {
                    Any any = ORB.init().create_any();
                    ServiceContextHelper.insert(any, sc);
                    try {
                        requestInfo.set_slot(slotId, any);
                    } catch (InvalidSlot e) {
                        throw (INTERNAL) (new INTERNAL(e.getMessage())).initCause(e);
                    }
                });
    }

    public Rofl loadAndCreate(ServerRequestInfo requestInfo) {
        try {
            return Optional.of(requestInfo.get_slot(slotId))
                    .filter(any -> any.type().kind() == ServiceContextHelper.type().kind())
                    .map(ServiceContextHelper::extract)
                    .map(RoflHelper::createFromServiceContext)
                    .orElse(Rofl.NONE);
        } catch (InvalidSlot e) {
            throw (INTERNAL) (new INTERNAL(e.getMessage())).initCause(e);
        }
    }

    private static Rofl createFromServiceContext(ServiceContext serviceContext) {
        return RemoteOrb.of(serviceContext)
                .map(ro -> ro.createRofl(serviceContext))
                .orElseGet(() -> {
                    logger.warning("Failed to find ROFL for service context id:" + serviceContext.context_id);
                    return BAD.createRofl(serviceContext);
                });
    }

    public static Rofl createFromTaggedComponent(ClientRequestInfo ri) {
        return RemoteOrb.KNOWN_REMOTE_ORBS.stream()
                .map(remoteOrb -> {
                    try {
                        TaggedComponent tc = ri.get_effective_component(remoteOrb.tagComponentId);
                        return remoteOrb.createRofl(tc);
                    } catch (BAD_PARAM e) {
                        if (e.minor == MinorInvalidComponentId) return null;
                        throw e;
                    }
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(Rofl.NONE);
    }
}
