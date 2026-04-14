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
package org.omg.PortableInterceptor;

import org.omg.CORBA.Any;
import org.omg.CORBA.NO_IMPLEMENT;
import org.omg.CORBA.ORB;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.CORBA.StringHolder;
import org.omg.PortableInterceptor.TestInterfacePackage.s;
import org.omg.PortableInterceptor.TestInterfacePackage.sHolder;
import org.omg.PortableInterceptor.TestInterfacePackage.user;
import org.omg.PortableServer.POA;

import java.util.Objects;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

final class TestInterface_impl extends TestInterfacePOA {
    private final POA poa;
    private final Current current;
    private final int slotId;

    TestInterface_impl(ORB orb, POA poa, int slotId) {
        this.poa = poa;
        var obj = assertDoesNotThrow(() -> orb.resolve_initial_references("PICurrent"));
        current = requireNonNull(CurrentHelper.narrow(obj));
        this.slotId = slotId;
    }

    public void noargs() {}

    public void noargs_oneway() {}

    public void systemexception() { throw new NO_IMPLEMENT(); }

    public void userexception() throws user { throw new user(); }

    public void location_forward() { fail(); }

    public void test_service_context() {
        // Test: get_slot
        Any slotData = assertDoesNotThrow(() -> current.get_slot(slotId));
        int v = slotData.extract_long();
        assertEquals(10, v);
        slotData.insert_long(20);
        assertDoesNotThrow(() -> current.set_slot(slotId, slotData));
    }

    public String string_attrib() {return "TEST"; }

    public void string_attrib(String param) { assertEquals("TEST", param); }

    public void one_string_in(String param) { assertEquals("TEST", param); }

    public void one_string_inout(StringHolder param) {
        assertEquals("TESTINOUT", param.value);
        param.value = "TEST";
    }

    public void one_string_out(StringHolder param) { param.value = "TEST"; }

    public String one_string_return() { return "TEST"; }

    public s struct_attrib() { return new s("TEST"); }

    public void struct_attrib(s param) { assertEquals("TEST", param.sval); }

    public void one_struct_in(s param) { assertEquals("TEST", param.sval); }

    public void one_struct_inout(sHolder param) { param.value.sval = "TEST"; }

    public void one_struct_out(sHolder param) { param.value = new s("TEST"); }

    public s one_struct_return() { return new s("TEST"); }

    public POA _default_POA() { return poa; }
}
