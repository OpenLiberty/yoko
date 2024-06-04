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

import org.junit.jupiter.api.Test;
import org.omg.CORBA.LocalObject;
import org.omg.CORBA.OBJECT_NOT_EXIST;
import org.omg.CORBA.ORB;
import org.omg.PortableServer.AdapterActivator;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAManager;
import org.omg.PortableServer.POAPackage.AdapterAlreadyExists;
import org.omg.PortableServer.POAPackage.InvalidPolicy;
import org.omg.PortableServer.POAPackage.ObjectAlreadyActive;
import org.omg.PortableServer.POAPackage.ServantAlreadyActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;
import org.omg.PortableServer.Servant;
import org.omg.PortableServer.ServantActivator;
import test.poa.TestHelper;
import test.poa.Test_impl;
import testify.bus.Bus;
import testify.iiop.annotation.ConfigureServer;
import testify.iiop.annotation.ConfigureServer.BeforeServer;

import static org.apache.yoko.orb.PortableServer.PolicyValue.PERSISTENT;
import static org.apache.yoko.orb.PortableServer.PolicyValue.USER_ID;
import static org.apache.yoko.orb.PortableServer.PolicyValue.USE_SERVANT_MANAGER;
import static org.apache.yoko.orb.PortableServer.PolicyValue.create_POA;
import static org.junit.Assert.assertNotNull;

@ConfigureServer
public final class AdapterActivatorServerTest {
    public static final String PARENT_POA = "parentPoa";
    public static final String CHILD_POA = "childPoa";

    @BeforeServer
    public static void setup(ORB orb, POA root, Bus bus) throws Exception {
        POAManager manager = root.the_POAManager();
        assertNotNull(manager);

        // First create an object-reference to the test POA. Then destroy the POA
        // so that the adapter activator will activate the POA when necessary.
        POA parentPoa = create_POA(PARENT_POA, root, manager,  PERSISTENT, USER_ID, USE_SERVANT_MANAGER);
        POAManager childManager = parentPoa.the_POAManager();
        POA childPoa = create_POA(CHILD_POA, parentPoa, childManager, PERSISTENT, USER_ID, USE_SERVANT_MANAGER);

        org.omg.CORBA.Object ref = childPoa.create_reference_with_id(("test1").getBytes(), "IDL:Test:1.0");
        String ior = orb.object_to_string(ref);
        childPoa.destroy(true, true);
        parentPoa.destroy(true, true);

        root.the_activator(new TestRemoteAdapterActivator(orb));
        bus.put("test1", ior);
    }

    @Test
    void test1(ORB orb, Bus bus) {
        TestHelper.narrow(orb.string_to_object(bus.get("test1"))).aMethod();
    }

    // Classes for testing the adapter activator on a remote call.
    final static class TestServantActivator extends LocalObject implements ServantActivator {
        private final ORB orb;
        TestServantActivator(ORB orb) {
            this.orb = orb;
        }

        public Servant incarnate(byte[] oid, POA poa) {
            // If the user is not requesting the object "test" then fail
            if (!new String(oid).equals("test1")) throw new OBJECT_NOT_EXIST();

            // Verify that POA allows activator to explicitly activate a servant
            try {
                final Servant servant = new Test_impl(orb, "test1", false);
                poa.activate_object_with_id(oid, servant);
                return servant;
            } catch (ObjectAlreadyActive | ServantAlreadyActive | WrongPolicy ex) {
                throw new RuntimeException(ex);
            }
        }

        public void etherealize(byte[] oid, POA poa, Servant servant, boolean cleanup, boolean remaining) {
            if (!remaining) {
                // If the user is requesting the object "test" then oblige.
                if ("test1".equals(new String(oid)))
                    servant = null;
            }
        }
    }

    final static class TestRemoteAdapterActivator extends LocalObject implements AdapterActivator {
        private final TestServantActivator activator;
        TestRemoteAdapterActivator(ORB orb) {
            activator = new TestServantActivator(orb);
        }
        public boolean unknown_adapter(POA parent, String name) {
            if (!name.equals(PARENT_POA) && !name.equals(CHILD_POA)) return false;
            try {
                POA poa = create_POA(name, parent, parent.the_POAManager(), PERSISTENT, USER_ID, USE_SERVANT_MANAGER);
                if (name.equals(PARENT_POA)) poa.the_activator(this); else poa.set_servant_manager(activator);
                return true;
            }  catch (WrongPolicy | InvalidPolicy | AdapterAlreadyExists e) {
                throw new RuntimeException(e);
            }
        }
    }
}
