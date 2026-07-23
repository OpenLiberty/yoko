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
package testify.iiop.annotation;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.omg.CORBA.ORB;
import org.omg.PortableInterceptor.ORBInitInfo;
import testify.iiop.TestORBInitializer;
import testify.iiop.annotation.ConfigureOrb.UseWithOrb;
import testify.iiop.annotation.ConfigureServer.BeforeServer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static testify.iiop.annotation.ConfigureOrb.UseWithOrb.InitializerScope.CLIENT;
import static testify.iiop.annotation.ConfigureOrb.UseWithOrb.InitializerScope.SERVER;

@ConfigureServer(
        clientOrb = @ConfigureOrb(args = "base client orb"),
        serverOrb = @ConfigureOrb(args = "base server orb")
)
abstract class TestConfigureServerBase {
    static ORB clientOrb, serverOrb;
    static String clientOrbId, serverOrbId;

    @BeforeServer
    public static void recordServerOrb(ORB orb) {
        serverOrb = orb;
        System.out.println("### server ORB = " + serverOrb);
    }

    @BeforeAll
    public static void recordClientOrb(ORB orb) {
        clientOrb = orb;
        System.out.println("### client ORB = " + clientOrb);
    }

    @AfterAll
    public static void scrub() {
        clientOrb = serverOrb = null;
        clientOrbId = serverOrbId = null;
    }

    @UseWithOrb(scope = CLIENT)
    public static class ClientOrbInitializer implements TestORBInitializer {
        public void pre_init(ORBInitInfo info) {
            System.err.println("ClientOrbInitializer.pre_init() called with info:\n" + info);
            Thread.dumpStack();
            clientOrbId = info.arguments()[0];
        }
    }

    @UseWithOrb(scope = SERVER)
    public static class ServerOrbInitializer implements TestORBInitializer {
        public void pre_init(ORBInitInfo info) {
            System.err.println("ServerOrbInitializer.pre_init() called with info:\n" + info);
            Thread.dumpStack();
            serverOrbId = info.arguments()[0];
        }
    }
}

@ConfigureServer(
        separation = ConfigureServer.Separation.INTER_ORB,
        clientOrb = @ConfigureOrb(args = "inter-orb client orb"),
        serverOrb = @ConfigureOrb(args = "inter-orb server orb")
)
class TestConfigureServerInterOrb extends TestConfigureServerBase {
    @Test
    void testServerOrbIsDistinct() {
        assertEquals("inter-orb client orb", clientOrbId);
        assertEquals("inter-orb server orb", serverOrbId);
        assertNotNull(clientOrb);
        assertNotNull(serverOrb);
        assertNotSame(clientOrb, serverOrb, "Server ORB should be distinct from client ORB");
    }
}

@ConfigureServer(
        separation = ConfigureServer.Separation.INTER_PROCESS,
        clientOrb = @ConfigureOrb(args = "inter-process client orb"),
        serverOrb = @ConfigureOrb(args = "inter-process server orb")
)
class TestConfigureServerInterProcess extends TestConfigureServerBase {
    @Test
    void testServerOrbIsNull() {
        // When we run inter process, the server-side stuff runs in the remote process, and none of the fields should be set locally
        assertNull(serverOrbId);
        assertNull(serverOrb, "Server ORB should be unavailable in client process");
        assertNotNull(clientOrb);
        assertEquals("inter-process client orb", clientOrbId);
    }
}

@ConfigureServer(
        separation = ConfigureServer.Separation.COLLOCATED,
        serverOrb = @ConfigureOrb(args = "collocated orb"),
        clientOrb = @ConfigureOrb(args = "other text")
)
class TestConfigureServerCollocated extends TestConfigureServerBase {
    @Test
    void testServerOrbIsNotDistinct() {
        assertEquals("collocated orb", clientOrbId);
        assertEquals("collocated orb", serverOrbId);
        assertNotNull(clientOrb);
        assertSame(clientOrb, serverOrb, "Server ORB should be identical to client ORB");
    }
}
