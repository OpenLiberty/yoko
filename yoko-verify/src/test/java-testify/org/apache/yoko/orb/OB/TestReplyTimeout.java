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

import acme.Echo;
import org.junit.jupiter.api.Test;
import org.omg.CORBA.TIMEOUT;
import testify.iiop.annotation.ConfigureOrb;
import testify.iiop.annotation.ConfigureServer;
import testify.iiop.annotation.ConfigureServer.RemoteImpl;
import testify.iiop.annotation.ConfigureServer.RemoteStub;

import java.rmi.RemoteException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static testify.util.Assertions.assertThrows;

// server orb should time out any reply after 2 ms
@ConfigureServer(serverOrb = @ConfigureOrb(props = "yoko.orb.policy.reply_timeout=2"))
public class TestReplyTimeout {
    @RemoteImpl // wait 5 seconds then return
    public static final Echo wait = s -> { assertDoesNotThrow(() -> Thread.sleep(5000)); return s; };
    @RemoteStub
    public static Echo stub;

    @Test
    void testReplyTimeout() throws Exception {
        assertThrows(RemoteException.class, () -> stub.echo("Should actually wait until timeout"), TIMEOUT.class);
    }
}
