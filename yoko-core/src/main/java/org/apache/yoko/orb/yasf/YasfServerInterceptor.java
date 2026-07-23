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
package org.apache.yoko.orb.yasf;

import org.apache.yoko.io.SimplyCloseable;
import org.apache.yoko.orb.PortableInterceptor.ExtendedServerRequestInterceptor;
import org.omg.CORBA.LocalObject;
import org.omg.PortableInterceptor.ForwardRequest;
import org.omg.PortableInterceptor.ServerRequestInfo;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.apache.yoko.util.yasf.YasfWrangler.YASF_WRANGLER;
import static org.apache.yoko.util.ThreadLocalStack.YASF_THREAD_LOCAL;

public final class YasfServerInterceptor extends LocalObject implements ExtendedServerRequestInterceptor {
    private static final String NAME = YasfServerInterceptor.class.getName();

    private final int slotId;

    public YasfServerInterceptor(int slotId) {
        this.slotId = slotId;
    }

    @Override
    public void receive_request_service_contexts(ServerRequestInfo ri) throws ForwardRequest {
        // Store in slot - will be retrieved after context switch in pre_unmarshal
        YASF_WRANGLER.setSlot(slotId, ri, YASF_WRANGLER.readData(ri));
    }

    @Override
    public void pre_unmarshal(ServerRequestInfo ri) {
        @SuppressWarnings("resource") // popped in post_marshal
        SimplyCloseable ignored = YASF_THREAD_LOCAL.push(YASF_WRANGLER.getSlot(slotId, ri));
    }

    @Override
    public void post_marshal(ServerRequestInfo ri) {
        YASF_THREAD_LOCAL.pop();
    }

    @Override
    public void send_reply(ServerRequestInfo ri) {
        YASF_WRANGLER.addSc(ri);
    }

    @Override
    public void send_exception(ServerRequestInfo ri) {
        YASF_WRANGLER.addSc(ri);
    }

    @Override
    public void send_other(ServerRequestInfo ri) {
        YASF_WRANGLER.addSc(ri);
    }

    @Override
    public String name() {
        return NAME;
    }

    private void readObject(ObjectInputStream ios) throws IOException {
        throw new NotSerializableException(NAME);
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
        throw new NotSerializableException(NAME);
    }
}
