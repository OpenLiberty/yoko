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
package org.apache.yoko.util.cmsf;

import acme.RemoteFunction;
import org.junit.jupiter.api.Test;
import testify.iiop.annotation.ConfigureServer;
import testify.iiop.annotation.ConfigureServer.RemoteImpl;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ConfigureServer
public class CmsfTest {
    public static final class Message implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String text;
        public Message(String text) { this.text = text; }
        private void readObject(ObjectInputStream in) throws Exception {
            // Cmsf is only set when writing out.
            // All options default when reading in,
            // so the thread should have Cmsf set to null.
            assertEquals(1, CmsfThreadLocal.get());
            in.defaultReadObject();
        }
        private void writeObject(ObjectOutputStream out) throws Exception {
            // Since we are marshalling to another Yoko ORB,
            // Cmsf options should be set when writing out.
            assertEquals(2, CmsfThreadLocal.get());
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
