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
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.apache.yoko.util;

import org.omg.PortableInterceptor.ClientRequestInfo;
import org.omg.PortableInterceptor.IORInfo;
import org.omg.PortableInterceptor.ServerRequestInfo;

/**
 * Generic interface for handling CORBA header information (tagged components and service contexts)
 * in portable interceptors.
 *
 * @param <T> the type of data being transmitted in headers
 */
public interface InfoWrangler<T> {
    /**
     * Adds a tagged component to an IOR.
     *
     * @param info the IOR information
     */
    void addTc(IORInfo info);

    /**
     * Adds a service context to a client request.
     *
     * @param ri the client request information
     */
    void addSc(ClientRequestInfo ri);

    /**
     * Adds a service context to a server reply.
     *
     * @param ri the server request information
     */
    void addSc(ServerRequestInfo ri);

    /**
     * Reads data from a client request's tagged component.
     *
     * @param ri the client request information
     * @return the data read from the tagged component, or null/default if not present
     */
    T readData(ClientRequestInfo ri);

    /**
     * Reads data from a server request's service context.
     *
     * @param ri the server request information
     * @return the data read from the service context, or null/default if not present
     */
    T readData(ServerRequestInfo ri);

    /**
     * Stores data in a PICurrent slot.
     *
     * @param slotId the slot identifier
     * @param ri the server request information
     * @param data the data to store
     */
    void setSlot(int slotId, ServerRequestInfo ri, T data);

    /**
     * Retrieves data from a PICurrent slot.
     *
     * @param slotId the slot identifier
     * @param ri the server request information
     * @return the data retrieved from the slot
     */
    T getSlot(int slotId, ServerRequestInfo ri);
}
