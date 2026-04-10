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

import org.omg.CORBA.ARG_IN;
import org.omg.CORBA.ARG_INOUT;
import org.omg.CORBA.ARG_OUT;
import org.omg.CORBA.Any;
import org.omg.CORBA.BAD_OPERATION;
import org.omg.CORBA.BAD_OPERATIONHelper;
import org.omg.CORBA.Bounds;
import org.omg.CORBA.NO_IMPLEMENT;
import org.omg.CORBA.NO_IMPLEMENTHelper;
import org.omg.CORBA.NVList;
import org.omg.CORBA.ORB;
import org.omg.CORBA.ServerRequest;
import org.omg.PortableInterceptor.TestInterfacePackage.s;
import org.omg.PortableInterceptor.TestInterfacePackage.sHelper;
import org.omg.PortableInterceptor.TestInterfacePackage.user;
import org.omg.PortableInterceptor.TestInterfacePackage.userHelper;
import org.omg.PortableServer.DynamicImplementation;
import org.omg.PortableServer.POA;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

final class TestInterfaceDSI_impl extends DynamicImplementation {
    private final ORB orb;
    private final int slotId;
    private final Current current;

    TestInterfaceDSI_impl(ORB orb, int slotId) {
        this.orb = orb;
        this.slotId = slotId;
        var obj = assertDoesNotThrow(() -> orb.resolve_initial_references("PICurrent"));
        current = requireNonNull(CurrentHelper.narrow(obj));
    }

    // ----------------------------------------------------------------------
    // TestInterfaceDSI_impl public member implementation
    // ----------------------------------------------------------------------

    static final String[] interfaces_ = { "IDL:TestInterface:1.0" };

    public String[] _all_interfaces(POA poa, byte[] oid) {
        return interfaces_;
    }

    public boolean _is_a(String name) {
        if (name.equals("IDL:TestInterface:1.0")) {
            return true;
        }

        return super._is_a(name);
    }

    public void invoke(ServerRequest request) {
        String name = request.operation();

        final NVList list = orb.create_list(0);
        Any any = orb.create_any();
        Any result = orb.create_any();
        switch (name) {
            case "noargs":
            case "noargs_oneway": {
                request.arguments(list);
                return;
            }
            case "systemexception": {
                request.arguments(list);
                NO_IMPLEMENTHelper.insert(result, new NO_IMPLEMENT());
                request.set_exception(result);
                return;
            }
            case "userexception": {
                request.arguments(list);
                userHelper.insert(result, new user());
                request.set_exception(result);
                return;
            }
            case "location_forward":
                throw (Error) fail();
            case "test_service_context": {
                request.arguments(list);
                Any slotData = assertDoesNotThrow(() -> current.get_slot(slotId));
                int v = slotData.extract_long();
                assertEquals(10, v);
                slotData.insert_long(20);
                assertDoesNotThrow(() -> current.set_slot(slotId, slotData));
                return;
            }
            case "_get_string_attrib":
            case "one_string_return": {
                request.arguments(list);
                result.insert_string("TEST");
                request.set_result(result);
                return;
            }
            case "_set_string_attrib": {
                any.type(orb.create_string_tc(0));
                list.add_value("", any, ARG_IN.value);
                request.arguments(list);
                any = assertDoesNotThrow(() -> list.item(0).value());
                assertEquals("TEST", any.extract_string());
                return;
            }
            case "one_string_in": {
                any.type(orb.create_string_tc(0));
                list.add_value("", any, ARG_IN.value);
                request.arguments(list);
                any = assertDoesNotThrow(() -> list.item(0).value());
                String param = any.extract_string();
                assertEquals("TEST", param);
                return;
            }
            case "one_string_inout": {
                any.type(orb.create_string_tc(0));
                list.add_value("", any, ARG_INOUT.value);
                request.arguments(list);
                any = assertDoesNotThrow(() -> list.item(0).value());
                String param = any.extract_string();
                assertEquals("TESTINOUT", param);
                any.insert_string("TEST");
                return;
            }
            case "one_string_out": {
                any.type(orb.create_string_tc(0));
                list.add_value("", any, ARG_OUT.value);
                request.arguments(list);
                any = assertDoesNotThrow(() -> list.item(0).value());
                any.insert_string("TEST");
                return;
            }
            case "_get_struct_attrib": {
                request.arguments(list);
                sHelper.insert(result, new s("TEST"));
                request.set_result(result);
                return;
            }
            case "_set_struct_attrib":
            case "one_struct_in": {
                any.type(sHelper.type());
                list.add_value("", any, ARG_IN.value);
                request.arguments(list);
                any = assertDoesNotThrow(() -> list.item(0).value());
                s param = sHelper.extract(any);
                assertEquals("TEST", param.sval);
                return;
            }
            case "one_struct_inout": {
                any.type(sHelper.type());
                list.add_value("", any, ARG_INOUT.value);
                request.arguments(list);
                any = assertDoesNotThrow(() -> list.item(0).value());
                s param = sHelper.extract(any);
                assertEquals("TESTINOUT", param.sval);
                s rc = new s();
                rc.sval = "TEST";
                sHelper.insert(any, rc);
                return;
            }
            case "one_struct_out": {
                any.type(sHelper.type());
                list.add_value("", any, ARG_OUT.value);
                request.arguments(list);
                any = assertDoesNotThrow(() -> list.item(0).value());
                s rc = new s();
                rc.sval = "TEST";
                sHelper.insert(any, rc);
                return;
            }
            case "one_struct_return": {
                request.arguments(list);
                s rc = new s();
                rc.sval = "TEST";
                sHelper.insert(result, rc);
                request.set_result(result);
                return;
            }
            default:
                System.err.println("DSI implementation: unknown operation: " + name);
                request.arguments(list);
                Any exAny = orb.create_any();
                BAD_OPERATIONHelper.insert(exAny, new BAD_OPERATION());
                request.set_exception(exAny);
        }
    }
}
