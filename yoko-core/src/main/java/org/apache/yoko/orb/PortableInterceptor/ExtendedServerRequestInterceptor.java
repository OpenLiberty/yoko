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
package org.apache.yoko.orb.PortableInterceptor;

import org.omg.PortableInterceptor.ForwardRequest;
import org.omg.PortableInterceptor.ServerRequestInfo;
import org.omg.PortableInterceptor.ServerRequestInterceptor;

/**
 * Extended server request interceptor interface that provides additional
 * interception points for setting up thread-local state after a context switch
 * and cleaning up after marshalling.
 * This is needed when the request processing switches to a different thread
 * between receive_request_service_contexts and argument deserialization,
 * and when thread-local state needs to be cleaned up after response marshalling.
 *
 * All methods have default empty implementations to simplify implementation.
 */
public interface ExtendedServerRequestInterceptor extends ServerRequestInterceptor {
    /**
     * Called after a context switch to set up thread-local state on the new thread.
     * This is invoked before argument deserialization begins, allowing interceptors
     * to retrieve data from PICurrent slots and push it onto thread locals.
     *
     * @param ri the server request info
     */
    default void pre_unmarshal(ServerRequestInfo ri) {}

    /**
     * Called after response marshalling is complete to clean up thread-local state.
     * This is invoked after the response has been marshalled, allowing interceptors
     * to clean up any thread-local state that was set up during request processing.
     *
     * @param ri the server request info
     */
    default void post_marshal(ServerRequestInfo ri) {}

    @Override
    default void receive_request_service_contexts(ServerRequestInfo ri) throws ForwardRequest {}

    @Override
    default void receive_request(ServerRequestInfo ri) throws ForwardRequest {}

    @Override
    default void send_reply(ServerRequestInfo ri) {}

    @Override
    default void send_exception(ServerRequestInfo ri) throws ForwardRequest {}

    @Override
    default void send_other(ServerRequestInfo ri) throws ForwardRequest {}

    @Override
    default void destroy() {}
}
