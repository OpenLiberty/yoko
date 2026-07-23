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
package org.apache.yoko.orb.cmsf;

import org.apache.yoko.io.SimplyCloseable;
import org.apache.yoko.util.cmsf.Cmsf;
import org.omg.CORBA.LocalObject;
import org.omg.PortableInterceptor.ClientRequestInfo;
import org.omg.PortableInterceptor.ClientRequestInterceptor;
import org.omg.PortableInterceptor.ForwardRequest;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.apache.yoko.util.ThreadLocalStack.CMSF_THREAD_LOCAL;
import static org.apache.yoko.util.cmsf.CmsfWrangler.CMSF_WRANGLER;

public final class CmsfClientInterceptor extends LocalObject implements ClientRequestInterceptor {
    private static final String NAME = CmsfClientInterceptor.class.getName();

    @Override
    public void send_request(ClientRequestInfo ri) throws ForwardRequest {
        Cmsf cmsf = CMSF_WRANGLER.readData(ri);
        CMSF_WRANGLER.addSc(ri);
        @SuppressWarnings("resource") // popped in receive_*
        SimplyCloseable ignored = CMSF_THREAD_LOCAL.push(cmsf);
    }

    @Override
    public void send_poll(ClientRequestInfo ri) {
    }

    @Override
    public void receive_reply(ClientRequestInfo ri) {
        CMSF_THREAD_LOCAL.pop();
    }

    @Override
    public void receive_exception(ClientRequestInfo ri) {
        CMSF_THREAD_LOCAL.pop();
    }

    @Override
    public void receive_other(ClientRequestInfo ri) {
        CMSF_THREAD_LOCAL.pop();
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void destroy() {
    }

    private void readObject(ObjectInputStream ios) throws IOException {
        throw new NotSerializableException(NAME);
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
        throw new NotSerializableException(NAME);
    }
}
