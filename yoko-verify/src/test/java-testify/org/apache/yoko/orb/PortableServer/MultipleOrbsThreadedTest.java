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
import org.omg.PortableServer.Servant;
import test.poa.TestHelper;
import test.poa.TestInfo;
import test.poa.TestPOA;
import test.poa.TestServer;
import test.poa.TestServerHelper;
import test.poa.TestServer_impl;
import testify.bus.Bus;
import testify.iiop.annotation.ConfigureOrb;
import testify.iiop.annotation.ConfigureServer;
import testify.iiop.annotation.ConfigureServer.BeforeServer;
import testify.iiop.annotation.ConfigureServer.RemoteImpl;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.apache.yoko.util.Arrays.NO_STRINGS;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Verify that a server process can host several ORBs using the threaded concurrency model,
 * and that each ORB can shut itself down from within a request without affecting the others.
 */
@ConfigureServer(serverOrb = @ConfigureOrb(props = {"yoko.orb.conc_model=threaded", "yoko.orb.oa.conc_model=threaded"}))
public class MultipleOrbsThreadedTest {
    private static final String SERVER = "server";
    /** The number of ORBs in the server process, in addition to the one testify provides. */
    private static final int NUM_ORBS = 5;
    /** The number of calls after which each object shuts down its own ORB. */
    private static final int CALLS_BEFORE_SHUTDOWN = 3;

    /** The additional ORBs in the server process. */
    private static final List<ORB> extraOrbs = new ArrayList<>();

    /** Shuts down its own ORB after a fixed number of method calls. */
    static final class TestOrb_impl extends TestPOA {
        private final ORB orb;
        private int count = 0;

        TestOrb_impl(ORB orb) { this.orb = orb; }

        public void aMethod() {
            if (++count == CALLS_BEFORE_SHUTDOWN) orb.shutdown(false);
        }
    }

    /** Allows the client to clean up the additional ORBs, which testify knows nothing about. */
    public interface ExtraOrbs extends Remote {
        void destroy() throws RemoteException;
    }

    @RemoteImpl
    public static final ExtraOrbs extraOrbsCleaner = () -> extraOrbs.forEach(ORB::destroy);

    @BeforeServer
    public static void setUp(ORB orb, Bus bus) throws Exception {
        TestInfo[] info = new TestInfo[NUM_ORBS];
        for (int i = 0; i < NUM_ORBS; i++) {
            Properties props = new Properties();
            props.put("org.omg.CORBA.ORBClass", "org.apache.yoko.orb.CORBA.ORB");
            props.put("org.omg.CORBA.ORBSingletonClass", "org.apache.yoko.orb.CORBA.ORBSingleton");
            props.put("yoko.orb.id", "orb" + (i + 1)); // each ORB needs a distinct ID
            ORB extraOrb = ORB.init(NO_STRINGS, props);
            extraOrbs.add(extraOrb);

            // Use the real root POA (not the child POA testify injects) so servants can be activated in it.
            POA rootPOA = POAHelper.narrow(extraOrb.resolve_initial_references("RootPOA"));
            rootPOA.the_POAManager().activate();
            Servant servant = new TestOrb_impl(extraOrb);
            info[i] = new TestInfo(TestHelper.narrow(rootPOA.id_to_reference(rootPOA.activate_object(servant))), "");
        }
        bus.put(SERVER, orb.object_to_string(new TestServer_impl(orb, info)._this(orb)));
    }

    @Test
    void eachOrbServesItsObject(ORB clientOrb, Bus bus) {
        TestServer server = TestServerHelper.narrow(clientOrb.string_to_object(bus.get(SERVER)));
        assertNotNull(server);
        TestInfo[] info = server.get_info();

        // Interleave the calls to the different ORBs.
        // Each ORB shuts itself down during its last call, which must still succeed.
        assertDoesNotThrow(() -> {
            for (int call = 0; call < CALLS_BEFORE_SHUTDOWN; call++) {
                for (TestInfo i : info) i.obj.aMethod();
            }
        });
    }

    @AfterAll
    static void destroyExtraOrbs(ExtraOrbs extraOrbs) throws Exception {
        extraOrbs.destroy();
    }
}
