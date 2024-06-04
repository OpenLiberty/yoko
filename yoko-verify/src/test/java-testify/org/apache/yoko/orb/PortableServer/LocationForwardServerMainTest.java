/*
 * Copyright 2023 IBM Corporation and others.
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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.omg.CORBA.LocalObject;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Object;
import org.omg.PortableServer.CurrentPackage.NoContext;
import org.omg.PortableServer.ForwardRequest;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAManager;
import org.omg.PortableServer.POAPackage.ObjectNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;
import org.omg.PortableServer.Servant;
import org.omg.PortableServer.ServantActivator;
import test.poa.TestLocationForward;
import test.poa.TestLocationForwardHelper;
import test.poa.TestLocationForwardPOA;
import test.poa.Test_impl;
import testify.bus.Bus;
import testify.iiop.annotation.ConfigureServer;
import testify.iiop.annotation.ConfigureServer.BeforeServer;

import static org.apache.yoko.orb.PortableServer.PolicyValue.RETAIN;
import static org.apache.yoko.orb.PortableServer.PolicyValue.UNIQUE_ID;
import static org.apache.yoko.orb.PortableServer.PolicyValue.USER_ID;
import static org.apache.yoko.orb.PortableServer.PolicyValue.USE_SERVANT_MANAGER;
import static org.apache.yoko.orb.PortableServer.PolicyValue.create_POA;
import static org.junit.Assert.assertEquals;


@ConfigureServer
public class LocationForwardServerMainTest {
    @BeforeServer
    public static void setup(ORB orb, POA root, Bus bus) throws Exception {
        POAManager mgr = root.the_POAManager();
        POA poa = create_POA("poa", root, mgr, USER_ID, UNIQUE_ID, RETAIN, USE_SERVANT_MANAGER);

        TestLocationForwardActivator activator = new TestLocationForwardActivator();
        poa.set_servant_manager(activator);

        org.omg.CORBA.Object objRef = poa.create_reference_with_id("test".getBytes(), "IDL:Test:1.0");

        TestLocationForward_impl testImpl = new TestLocationForward_impl(orb, "Server", bus);
        activator.setActivatedServant(testImpl);

        // Wait for the client reference, then forward server servant manager's requests to the client's byte array object
        // onMsg is first due to timing window as if the clientObj is already there before onMsg is called, onMsg won't ever be called
        bus.onMsg("clientObj", ior -> activator.setForwardRequest(orb.string_to_object(ior)));
        bus.put("serverObj", orb.object_to_string(objRef));

        mgr.activate();
    }

    @BeforeAll
    public static void setupClient(ORB orb, POA root, Bus bus) throws Exception {
            POAManager mgr = root.the_POAManager();
            POA poa = create_POA("poa", root, mgr, USER_ID, UNIQUE_ID, RETAIN, USE_SERVANT_MANAGER);

            TestLocationForwardActivator activator = new TestLocationForwardActivator();
            poa.set_servant_manager(activator);

            Object objRef = poa.create_reference_with_id("test".getBytes(), "IDL:Test:1.0");

            //forward client servant manager's requests to the server's byte array object
            Object servant = orb.string_to_object(bus.get("serverObj"));
            activator.setForwardRequest(servant);

            TestLocationForward_impl testImpl = new TestLocationForward_impl(orb, "Client", bus);
            activator.setActivatedServant(testImpl);

            mgr.activate();
            bus.put("clientObj", orb.object_to_string(objRef));
    }

    @Test
    void test1(ORB orb, Bus bus) {
        TestLocationForward clientObj = TestLocationForwardHelper.narrow(orb.string_to_object(bus.get("clientObj")));

        // First should be client
        clientObj.aMethod();
        assertEquals("1", bus.peek("Client"));
        assertEquals(null, bus.peek("Server"));
        clientObj.deactivate_servant();

        // Second, should be remote
        clientObj.aMethod();
        assertEquals("1", bus.peek("Client"));
        assertEquals("1", bus.peek("Server"));
        clientObj.deactivate_servant();

        // Third should be client again
        clientObj.aMethod();
        assertEquals("2", bus.peek("Client"));
        assertEquals("1", bus.peek("Server"));
        clientObj.deactivate_servant();
    }

    static final class TestLocationForwardActivator extends LocalObject implements ServantActivator {
        private boolean activate;
        private Servant servant;
        private org.omg.CORBA.Object forward;
        TestLocationForwardActivator() { activate = false; }

        public void setActivatedServant(Servant servant) { this.servant = servant; }

        public void setForwardRequest(org.omg.CORBA.Object forward) { this.forward = forward; }

        public Servant incarnate(byte[] oid, POA poa) throws ForwardRequest {
            activate = !activate;
            if (activate) return servant;
            throw new ForwardRequest(forward);
        }

        public void etherealize(byte[] oid, POA poa, Servant servant, boolean cleanup, boolean remaining) {}
    }

    static final class TestLocationForward_impl extends TestLocationForwardPOA {
        private final Test_impl delegate;
        private final Bus bus;
        private final String identifier;
        private int invocations;

        TestLocationForward_impl(ORB orb, String identifier, Bus bus) {
            delegate = new Test_impl(orb, "", false);
            this.bus = bus;
            this.identifier = identifier;
            this.invocations = 0;
        }

        public void deactivate_servant() {
            try {
                byte[] oid = delegate.current_.get_object_id();
                POA poa = delegate.current_.get_POA();
                poa.deactivate_object(oid);
            } catch (NoContext | ObjectNotActive | WrongPolicy e) { throw new RuntimeException(e); }
        }

        public void aMethod() {
            delegate.aMethod();
            bus.put(identifier, String.valueOf(++invocations));
        }
    }
}
