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
import org.omg.PortableServer.AdapterActivator;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAManager;
import org.omg.PortableServer.POAPackage.AdapterAlreadyExists;
import org.omg.PortableServer.POAPackage.AdapterNonExistent;
import org.omg.PortableServer.POAPackage.InvalidPolicy;
import testify.iiop.annotation.ConfigureOrb;

import static org.apache.yoko.orb.PortableServer.PolicyValue.create_POA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@ConfigureOrb
public class AdapterActivatorTest {
    @Test
    void testAdapterActivator(POA root) throws Exception {
        POAManager rootMgr = root.the_POAManager();
        assertNotNull(rootMgr);

        TestAdapterActivator activator = new TestAdapterActivator();
        root.the_activator(activator);

        // Test: Activator and successful creation
        activator.reset("poa1", true);
        POA poa = root.find_POA("poa1", true);

        assertNotNull(poa);
        assertTrue(activator.invoked());
        String str = poa.the_name();
        assertEquals("poa1", str);
        POA parent = poa.the_parent();
        assertNotNull(parent);
        assertTrue(parent._is_equivalent(root));

        // Test: Activator and unsuccessful creation
        activator.reset("poa2", false);
        assertThrows(AdapterNonExistent.class, () -> root.find_POA("poa2", true));
        assertTrue(activator.invoked());

        // Test: Make sure activator isn't called when POA already exists
        activator.reset("poa1", true);
        poa = root.find_POA("poa1", true);

        assertFalse(activator.invoked());

        // Test: Disable adapter activator and make sure it isn't invoked
        root.the_activator(null);
        activator.reset("poa2", false);
        assertThrows(AdapterNonExistent.class, () -> root.find_POA("poa2", true));
        assertFalse(activator.invoked());

        poa.destroy(true, true);
    }

    final static class TestAdapterActivator extends LocalObject implements AdapterActivator {
        private String expectedName;
        private boolean create;
        private boolean invoked;

        TestAdapterActivator() {
            create = false;
            invoked = false;
        }

        void reset(String name, boolean create) {
            expectedName = name;
            this.create = create;
            invoked = false;
        }

        boolean invoked() {
            return invoked;
        }

        public boolean unknown_adapter(POA parent, String name) {
            assertEquals(name, expectedName);
            invoked = true;
            if (create) try {
                create_POA(name, parent, parent.the_POAManager());
            } catch (AdapterAlreadyExists | InvalidPolicy ex) {
                fail(ex.getMessage());
            }
            return create;
        }
    }
}
