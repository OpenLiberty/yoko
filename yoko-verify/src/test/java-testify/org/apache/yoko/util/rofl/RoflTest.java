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
package org.apache.yoko.util.rofl;

import acme.RemoteFunction;
import org.junit.jupiter.api.Test;
import org.omg.IOP.ServiceContext;
import org.omg.IOP.TaggedComponent;
import org.omg.PortableInterceptor.ClientRequestInfo;
import org.omg.PortableInterceptor.IORInfo;
import testify.iiop.TestClientRequestInterceptor;
import testify.iiop.TestIORInterceptor;
import testify.iiop.annotation.ConfigureOrb.UseWithOrb;
import testify.iiop.annotation.ConfigureServer;
import testify.iiop.annotation.ConfigureServer.RemoteImpl;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Objects;

import static org.apache.yoko.util.rofl.Rofl.RemoteOrb.IBM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static testify.hex.HexParser.HEX_STRING;

@ConfigureServer
public class RoflTest {
    @UseWithOrb
    public static class AddIbmOrbServiceContext implements TestClientRequestInterceptor, TestIORInterceptor {
        public static final byte[] PAYLOAD = HEX_STRING.parse("00BD001118000000");
        private final TaggedComponent TC = new TaggedComponent(IBM.tagComponentId, PAYLOAD);
        private final ServiceContext SC = new ServiceContext(IBM.serviceContextId, PAYLOAD);
        public void send_request(ClientRequestInfo ri) { ri.add_request_service_context(SC, true); }
        public void establish_components(IORInfo info) { info.add_ior_component(TC); }
    }

    public static final class Message implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String text;
        public Message(String text) { this.text = text; }
        private void readObject(ObjectInputStream in) throws Exception {
            // Yasf is only set when writing out.
            // All options default when reading in,
            // so the thread should have Yasf set to null.
            assertEquals(Rofl.NONE, RoflThreadLocal.get());
            in.defaultReadObject();
        }
        private void writeObject(ObjectOutputStream out) throws Exception {
            // Since we are marshalling to another Yoko ORB,
            // Yasf options should be set when writing out.
            assertEquals(IBM, RoflThreadLocal.get().type());
            out.defaultWriteObject();
        }
        public boolean equals(Object o) { return o instanceof Message && Objects.equals(text, ((Message) o).text); }
        public int hashCode() { return Objects.hashCode(text); }
        public String toString() { return String.format("MessageImpl[%s]", text); }
    }

    interface Echo extends RemoteFunction<Message, Message>{}

    @RemoteImpl
    public static final Echo REMOTE = m -> m;

    @Test
    public void sendMessage(Echo stub) throws RemoteException {
        Message m1 = new Message("Hello, world!");
        Message m2 = stub.apply(m1);
        assertEquals(m1, m2);
    }
}
