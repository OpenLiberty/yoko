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
package org.apache.yoko.orb.PortableServer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.omg.CORBA.ORB;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import test.poa.TestInfo;
import test.poa.TestServer;
import test.poa.TestServerHelper;
import test.poa.TestServer_impl;
import test.poa.Test_impl;
import testify.bus.Bus;
import testify.iiop.annotation.ConfigureServer;
import testify.iiop.annotation.ConfigureServer.BeforeServer;
import testify.iiop.annotation.ConfigureServer.RemoteImpl;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.apache.yoko.util.Arrays.NO_STRINGS;

/**
 * Verify that a single process can host more than one ORB, each serving its own objects.
 * The server process has two ORBs: the one testify provides, and a second one created here.
 */
@ConfigureServer
public class MultipleOrbsTest {
    private static final String SERVER_1 = "server1";
    private static final String SERVER_2 = "server2";

    /** The second ORB in the server process. */
    private static ORB orb2;

    /** Allows the client to clean up the second ORB, which testify knows nothing about. */
    public interface SecondOrb extends Remote {
        void destroy() throws RemoteException;
    }

    @RemoteImpl
    public static final SecondOrb secondOrb = () -> {
        orb2.shutdown(true);
        orb2.destroy();
    };

    @BeforeServer
    public static void setUp(ORB orb1, Bus bus) throws Exception {
        Properties props = new Properties();
        props.put("org.omg.CORBA.ORBClass", "org.apache.yoko.orb.CORBA.ORB");
        props.put("org.omg.CORBA.ORBSingletonClass", "org.apache.yoko.orb.CORBA.ORBSingleton");
        orb2 = ORB.init(NO_STRINGS, props);
        assertNotSame(orb1, orb2, "A second, distinct ORB is needed");

        bus.put(SERVER_1, publishServer(orb1));
        bus.put(SERVER_2, publishServer(orb2));
    }

    /** Create an object and a server for it in the given ORB, and return the stringified reference to the server. */
    private static String publishServer(ORB orb) throws Exception {
        // Use the real root POA (not the child POA testify injects) so that _this() can implicitly activate servants.
        POA rootPOA = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
        rootPOA.the_POAManager().activate();

        TestInfo info = new TestInfo();
        info.obj = new Test_impl(orb, rootPOA)._this(orb);
        info.except_id = "";
        return orb.object_to_string(new TestServer_impl(orb, new TestInfo[]{info})._this(orb));
    }

    private static TestServer server(ORB clientOrb, Bus bus, String key) {
        TestServer server = TestServerHelper.narrow(clientOrb.string_to_object(bus.get(key)));
        assertNotNull(server);
        return server;
    }

    @Test
    void bothOrbsServeTheirObjects(ORB clientOrb, Bus bus) {
        TestInfo[] info1 = server(clientOrb, bus, SERVER_1).get_info();
        TestInfo[] info2 = server(clientOrb, bus, SERVER_2).get_info();

        // interleave calls to the two ORBs
        assertDoesNotThrow(() -> {
            info1[0].obj.aMethod();
            info2[0].obj.aMethod();
            info1[0].obj.aMethod();
            info2[0].obj.aMethod();
        });
    }

    @AfterAll
    static void destroySecondOrb(SecondOrb secondOrb) throws Exception {
        secondOrb.destroy();
    }
}
