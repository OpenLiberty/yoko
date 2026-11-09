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
package org.apache.yoko.orb.CosNaming;

import org.apache.yoko.orb.CosNaming.tnaming.TransientNameService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.omg.CORBA.ORB;
import org.omg.CosNaming.NamingContextExt;
import testify.bus.Bus;
import testify.iiop.annotation.ConfigureServer;
import testify.iiop.annotation.ConfigureServer.BeforeServer;
import testify.iiop.annotation.ConfigureServer.RemoteImpl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.rmi.Remote;
import java.rmi.RemoteException;

import static test.tnaming.NameServiceChecks.checkBoundReferences;
import static test.tnaming.NameServiceChecks.checkIterators;
import static test.tnaming.NameServiceChecks.createBindingsOverWhichToIterate;
import static test.tnaming.NameServiceChecks.narrow;
import static test.tnaming.NameServiceChecks.publishTestObjects;
import static test.tnaming.NameServiceChecks.removeBindingsOverWhichToIterate;

/**
 * Test the stand-alone transient name service, which runs in its own ORB, separate from the ORB hosting the test objects.
 * The test objects are bound into the name service remotely.
 */
@ConfigureServer
public class StandaloneNameServiceTest {
    private static final String NAME_SERVICE_URL = "nameServiceUrl";

    /** The stand-alone name service, which runs in the server process but in its own ORB. */
    private static TransientNameService service;

    /** Allows the client to shut down the name service, which testify knows nothing about. */
    public interface NameServiceStopper extends Remote {
        void stop() throws Exception;
    }

    @RemoteImpl
    public static final NameServiceStopper stopper = () -> service.close();

    @BeforeServer
    public static void setUp(ORB serverOrb, Bus bus) throws Exception {
        final int port = findFreePort();
        service = new TransientNameService("localhost", port);
        service.run();
        final String url = "corbaloc::localhost:" + port + "/NameService";
        bus.put(NAME_SERVICE_URL, url);
        publishTestObjects(serverOrb, narrow(serverOrb.string_to_object(url)), bus);
    }

    private static int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            return socket.getLocalPort();
        }
    }

    private static NamingContextExt nameService(ORB clientOrb, Bus bus) {
        return narrow(clientOrb.string_to_object(bus.get(NAME_SERVICE_URL)));
    }

    @Test
    void testBoundReferences(ORB clientOrb, Bus bus) throws Exception {
        checkBoundReferences(clientOrb, nameService(clientOrb, bus), bus);
    }

    @Test
    void testIterators(ORB clientOrb, Bus bus) throws Exception {
        NamingContextExt root = nameService(clientOrb, bus);
        createBindingsOverWhichToIterate(clientOrb, root);
        try {
            checkIterators(root);
        } finally {
            removeBindingsOverWhichToIterate(root);
        }
    }

    @AfterAll
    static void stopNameService(NameServiceStopper stopper) throws Exception {
        stopper.stop();
    }
}
