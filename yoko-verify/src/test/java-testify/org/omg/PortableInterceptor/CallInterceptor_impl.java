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

import org.apache.yoko.orb.OB.Util;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.omg.CORBA.Any;
import org.omg.CORBA.BAD_INV_ORDER;
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.LocalObject;
import org.omg.CORBA.NO_IMPLEMENT;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Object;
import org.omg.CORBA.ParameterMode;
import org.omg.CORBA.Policy;
import org.omg.CORBA.TCKind;
import org.omg.CORBA.TypeCode;
import org.omg.Dynamic.Parameter;
import org.omg.IOP.Codec;
import org.omg.IOP.CodecFactory;
import org.omg.IOP.CodecFactoryHelper;
import org.omg.IOP.CodecPackage.FormatMismatch;
import org.omg.IOP.CodecPackage.TypeMismatch;
import org.omg.IOP.ENCODING_CDR_ENCAPS;
import org.omg.IOP.Encoding;
import org.omg.IOP.ServiceContext;
import org.omg.IOP.TAG_INTERNET_IOP;
import org.omg.IOP.TaggedComponent;
import org.omg.IOP.TaggedProfile;
import org.omg.PortableInterceptor.TestInterfacePackage.s;
import org.omg.PortableInterceptor.TestInterfacePackage.sHelper;
import org.omg.PortableInterceptor.TestInterfacePackage.userHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.oneOf;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.omg.CORBA.ParameterMode.PARAM_IN;
import static org.omg.CORBA.ParameterMode.PARAM_INOUT;
import static org.omg.CORBA.ParameterMode.PARAM_OUT;

final class CallInterceptor_impl extends LocalObject implements ClientRequestInterceptor {
    private final Codec cdrCodec;
    private final Current pic;
    private final int slotId;
    private int req;

    void testArgs(ClientRequestInfo ri, boolean resultAvail) {
        String op = ri.operation();
        Parameter[] args = ri.arguments();
        if (op.startsWith("_set_") || op.startsWith("_get_")) {
            boolean isstr; // struct or string?
            isstr = (op.contains("string"));
            if (op.startsWith("_get_")) {
                assertEquals(0, args.length);
                if (resultAvail) {
                    Any result = ri.result();
                    if (isstr) {
                        String str = result.extract_string();
                        assertTrue(str.startsWith("TEST"));
                    } else {
                        s sp = sHelper.extract(result);
                        assertTrue(sp.sval.startsWith("TEST"));
                    }
                }
            } else {
                assertEquals(1, args.length);
                assertSame(PARAM_IN, args[0].mode);
                if (resultAvail) {
                    if (isstr) {
                        String str = args[0].argument.extract_string();
                        assertTrue(str.startsWith("TEST"));
                    } else {
                        s sp = sHelper.extract(args[0].argument);
                        assertTrue(sp.sval.startsWith("TEST"));
                    }
                }
            }
        } else if (op.startsWith("one_")) {
            String which = op.substring(4); // Which operation?
            boolean isstr; // struct or string?
            ParameterMode mode; // The parameter mode

            // if(which.startsWith("struct"))
            isstr = which.startsWith("string");

            which = which.substring(7); // Skip <string|struct>_

            if (which.equals("return")) {
                assertEquals(0, args.length);
                if (resultAvail) {
                    Any result = ri.result();
                    if (isstr) {
                        String str = result.extract_string();
                        assertTrue(str.startsWith("TEST"));
                    } else {
                        s sp = sHelper.extract(result);
                        assertTrue(sp.sval.startsWith("TEST"));
                    }
                }
            } else {
                assertEquals(1, args.length);
                switch (which) {
                    case "in": mode = PARAM_IN; break;
                    case "inout": mode = PARAM_INOUT; break;
                    case "out": mode = PARAM_OUT; break;
                    default: throw (Error) fail("Unexpected value: " + which);
                }

                assertSame(mode, args[0].mode);

                if (mode != PARAM_OUT || resultAvail) {
                    if (isstr) {
                        String str = args[0].argument.extract_string();
                        assertTrue(str.startsWith("TEST"));
                    } else {
                        s sp = sHelper.extract(args[0].argument);
                        assertTrue(sp.sval.startsWith("TEST"));
                    }

                    if (resultAvail) {
                        Any result = ri.result();
                        TypeCode tc = result.type();
                        assertSame(TCKind.tk_void, tc.kind());
                    }
                }
            }
        } else {
            assertEquals(0, args.length);
        }
        if (!resultAvail) {
            try {
                Any result = ri.result();
                fail();
            } catch (BAD_INV_ORDER ex) {
                // Expected
            }
        }
    }

    CallInterceptor_impl(ORB orb, int slotId) {
        CodecFactory factory = assertDoesNotThrow(() -> CodecFactoryHelper.narrow(orb.resolve_initial_references("CodecFactory")));
        assertNotNull(factory);

        Encoding how = new Encoding((byte) ENCODING_CDR_ENCAPS.value, (byte) 0, (byte) 0);
        cdrCodec = assertDoesNotThrow(() -> factory.create_codec(how));
        assertNotNull(cdrCodec);

        pic = assertDoesNotThrow(() -> CurrentHelper.narrow(orb.resolve_initial_references("PICurrent")));
        assertNotNull(pic);

        this.slotId = slotId;
    }

    public String name() {
        return "CRI";
    }

    public void destroy() {}

    public void send_request(ClientRequestInfo ri) {
        req++;
        ri.request_id();
        String op = ri.operation();
        boolean oneway = op.equals("noargs_oneway");
        testArgs(ri, false);
        assertThrows(BAD_INV_ORDER.class, ri::result);

        TypeCode[] exceptions = ri.exceptions();
        if (op.equals("userexception")) {
            assertEquals(1, exceptions.length);
            assertTrue(exceptions[0].equal(userHelper.type()));
        } else {
            assertEquals(0, exceptions.length);
        }

        // Test: oneway and response expected are equivalent
        assertTrue((oneway && !ri.response_expected()) || (!oneway && ri.response_expected()));

        assertNotNull(ri.target());
        assertNotNull(ri.effective_target());
        assertEquals(TAG_INTERNET_IOP.value, ri.effective_profile().tag);

        assertThrows(BAD_INV_ORDER.class, ri::reply_status);
        assertThrows(BAD_INV_ORDER.class, ri::received_exception);
        assertThrows(BAD_INV_ORDER.class, ri::forward_reference);

        // Test: test get_effective_component
        TaggedComponent componentEncoding = ri.get_effective_component(MY_COMPONENT_ID.value);
        byte[] componentData = componentEncoding.component_data;
        Any componentAny = assertDoesNotThrow(() -> cdrCodec.decode_value(componentData, MyComponentHelper.type()));

        MyComponent component = MyComponentHelper.extract(componentAny);
        assertEquals(10, component.val);

        Policy policy = ri.get_request_policy(MY_CLIENT_POLICY_ID.value);
        MyClientPolicy myClientPolicy = MyClientPolicyHelper.narrow(policy);
        assertNotNull(myClientPolicy);
        assertEquals(10, myClientPolicy.value());

        assertThrows(BAD_PARAM.class, () -> ri.get_request_service_context(REQUEST_CONTEXT_ID.value));
        assertThrows(BAD_INV_ORDER.class, () -> ri.get_reply_service_context(REQUEST_CONTEXT_ID.value));

        if (op.equals("test_service_context")) {
            RequestContext context = new RequestContext();
            context.data = "request";

            Any slotData = assertDoesNotThrow(() -> ri.get_slot(slotId));
            context.val = slotData.extract_long();
            assertEquals(10, context.val);

            Any any = ORB.init().create_any();
            RequestContextHelper.insert(any, context);
            byte[] data = assertDoesNotThrow(() -> cdrCodec.encode_value(any));

            ServiceContext sc = new ServiceContext(REQUEST_CONTEXT_ID.value, data);
            ri.add_request_service_context(sc, false);

            assertDoesNotThrow(() -> ri.get_request_service_context(REQUEST_CONTEXT_ID.value));
        } else {
            try {
                ri.get_request_service_context(REQUEST_CONTEXT_ID.value);
                fail();
            } catch (BAD_PARAM ex) {
                // Expected
            }
        }

        Any slotData = assertDoesNotThrow(() -> pic.get_slot(slotId));
        assertEquals(TCKind._tk_null, slotData.type().kind().value());

        Any newSlotData = ORB.init().create_any();
        newSlotData.insert_long(15);
        assertDoesNotThrow(() -> pic.set_slot(slotId, newSlotData));
    }

    public void send_poll(ClientRequestInfo ri) {
        fail();
    }

    public void receive_reply(ClientRequestInfo ri) {
        ri.request_id();

        String op = ri.operation();

        boolean oneway = op.equals("noargs_oneway");

        testArgs(ri, true);

        TypeCode[] exceptions = ri.exceptions();
        if (op.equals("userexception")) {
            assertEquals(1, exceptions.length);
            assertTrue(exceptions[0].equal(userHelper.type()));
        } else {
            assertEquals(0, exceptions.length);
        }

        assertTrue(oneway != ri.response_expected());

        assertNotNull(ri.target());

        assertNotNull(ri.effective_target());

        TaggedProfile effectiveProfile = ri.effective_profile();
        assertEquals(TAG_INTERNET_IOP.value, effectiveProfile.tag);

        TaggedComponent componentEncoding = ri.get_effective_component(MY_COMPONENT_ID.value);
        byte[] componentData = componentEncoding.component_data;
        Any componentAny = assertDoesNotThrow(() -> cdrCodec.decode_value(componentData, MyComponentHelper.type()));

        MyComponent component = MyComponentHelper.extract(componentAny);
        assertEquals(10, component.val);

        Policy policy = ri.get_request_policy(MY_CLIENT_POLICY_ID.value);
        MyClientPolicy myClientPolicy = MyClientPolicyHelper.narrow(policy);
        assertNotNull(myClientPolicy);
        assertEquals(10, myClientPolicy.value());

        assertEquals(SUCCESSFUL.value, ri.reply_status());

        assertThrows(BAD_INV_ORDER.class, ri::received_exception);
        assertThrows(BAD_INV_ORDER.class, ri::forward_reference);

        if (op.equals("test_service_context")) {
            assertDoesNotThrow(() -> ri.get_request_service_context(REQUEST_CONTEXT_ID.value));

            ServiceContext sc1 = assertDoesNotThrow(() -> ri.get_reply_service_context(REPLY_CONTEXT_1_ID.value));
            assertEquals(REPLY_CONTEXT_1_ID.value, sc1.context_id);
            Any any1 = assertDoesNotThrow( () -> cdrCodec.decode_value(sc1.context_data, ReplyContextHelper.type()));
            ReplyContext context1 = ReplyContextHelper.extract(any1);
            assertEquals("reply1", context1.data);
            assertEquals(101, context1.val);

            ServiceContext sc2 = assertDoesNotThrow(() -> ri.get_reply_service_context(REPLY_CONTEXT_2_ID.value));
            assertEquals(REPLY_CONTEXT_2_ID.value, sc2.context_id);
            Any any2 = assertDoesNotThrow(() -> cdrCodec.decode_value(sc2.context_data, ReplyContextHelper.type()));
            ReplyContext context2 = ReplyContextHelper.extract(any2);
            assertEquals("reply2", context2.data);
            assertEquals(102, context2.val);

            ServiceContext sc3 = assertDoesNotThrow(() -> ri.get_reply_service_context(REPLY_CONTEXT_3_ID.value));
            assertEquals(REPLY_CONTEXT_3_ID.value, sc3.context_id);
            Any any3 = assertDoesNotThrow(() -> cdrCodec.decode_value(sc3.context_data, ReplyContextHelper.type()));
            ReplyContext context3 = ReplyContextHelper.extract(any3);
            assertEquals("reply3", context3.data);
            assertEquals(103, context3.val);

            ServiceContext sc4 = assertDoesNotThrow(() -> ri.get_reply_service_context(REPLY_CONTEXT_4_ID.value));
            assertEquals(REPLY_CONTEXT_4_ID.value, sc4.context_id);
            Any any4 = assertDoesNotThrow(() -> cdrCodec.decode_value(sc4.context_data, ReplyContextHelper.type()));
            ServiceContext sc = ri.get_reply_service_context(REPLY_CONTEXT_4_ID.value);
            assertEquals(REPLY_CONTEXT_4_ID.value, sc.context_id);
            ReplyContext context4 = ReplyContextHelper.extract(any4);
            assertEquals("reply4", context4.data);
            assertEquals(124, context4.val);
        } else {
            assertThrows(BAD_PARAM.class, () -> ri.get_reply_service_context(REPLY_CONTEXT_1_ID.value));
            assertThrows(BAD_PARAM.class, () -> ri.get_request_service_context(REQUEST_CONTEXT_ID.value));
        }

        ServiceContext sc = new ServiceContext(REQUEST_CONTEXT_ID.value, null);
        assertThrows(BAD_INV_ORDER.class, () -> ri.add_request_service_context(sc, false));

        Any slotData = assertDoesNotThrow(() -> pic.get_slot(slotId));
        int v = slotData.extract_long();
        assertEquals(15, v);

        Any newSlotData = ORB.init().create_any();
        newSlotData.insert_long(16);
        assertDoesNotThrow(() -> pic.set_slot(slotId, newSlotData));
    }

    public void receive_other(ClientRequestInfo ri) {
        ri.request_id();

        String op = ri.operation();
        assertEquals("location_forward", op);

        try {
            ri.arguments();
        } catch (BAD_INV_ORDER ex) {
            // Expected
        }

        TypeCode[] exceptions = ri.exceptions();
        if (op.equals("userexception")) {
            assertEquals(1, exceptions.length);
            assertTrue(exceptions[0].equal(userHelper.type()));
        } else {
            assertEquals(0, exceptions.length);
        }

        assertTrue(ri.response_expected());

        assertNotNull(ri.target());

        Object effectiveTarget = ri.effective_target();
        assertNotNull(effectiveTarget);

        assertEquals(TAG_INTERNET_IOP.value, ri.effective_profile().tag);

        TaggedComponent componentEncoding = ri.get_effective_component(MY_COMPONENT_ID.value);
        byte[] componentData = componentEncoding.component_data;
        Any componentAny = null;
        try {
            componentAny = cdrCodec.decode_value(componentData, MyComponentHelper.type());
        } catch (TypeMismatch | FormatMismatch ex) {
            fail();
        }

        MyComponent component = MyComponentHelper.extract(componentAny);
        assertEquals(10, component.val);

        Policy policy = ri.get_request_policy(MY_CLIENT_POLICY_ID.value);
        MyClientPolicy myClientPolicy = MyClientPolicyHelper.narrow(policy);
        assertNotNull(myClientPolicy);
        assertEquals(10, myClientPolicy.value());

        assertEquals(LOCATION_FORWARD.value, ri.reply_status());

        try {
            Any rc = ri.received_exception();
            fail();
        } catch (BAD_INV_ORDER ex) {
            // Expected
        }

        try {
            ri.forward_reference();
        } catch (BAD_INV_ORDER ex) {
            fail();
        }

        try {
            ri.get_request_service_context(REQUEST_CONTEXT_ID.value);
            fail();
        } catch (BAD_PARAM ex) {
            // Expected
        }

        try {
            ri.get_reply_service_context(REPLY_CONTEXT_1_ID.value);
            fail();
        } catch (BAD_PARAM ex) {
            // Expected
        }

        try {
            ri.add_request_service_context(new ServiceContext(REQUEST_CONTEXT_ID.value, null), false);
        } catch (BAD_INV_ORDER ex) {
            // Expected
        }

        Any slotData = null;
        try {
            slotData = pic.get_slot(slotId);
        } catch (InvalidSlot ex) {
            fail();
        }
        int v = slotData.extract_long();
        assertEquals(15, v);

        Any newSlotData = ORB.init().create_any();
        newSlotData.insert_long(16);
        try {
            pic.set_slot(slotId, newSlotData);
        } catch (InvalidSlot ex) {
            fail();
        }

        // Eat the location forward
        throw new NO_IMPLEMENT();
    }

    public void receive_exception(ClientRequestInfo ri) {
        ri.request_id();

        String op = ri.operation();
        // Log the exception details
        try {
            Any receivedException = ri.received_exception();
        } catch (Exception e) {
        }

        assertThat(op, is(oneOf("systemexception", "userexception")));

        boolean user = op.equals("userexception");

        try {
            Parameter[] args = ri.arguments();
        } catch (BAD_INV_ORDER ex) {
            // Expected
        }

        TypeCode[] exceptions = ri.exceptions();
        if (op.equals("userexception")) {
            assertEquals(1, exceptions.length);
            assertTrue(exceptions[0].equal(userHelper.type()));
        } else {
            assertEquals(0, exceptions.length);
        }

        assertTrue(ri.response_expected());
        assertNotNull(ri.target());
        assertNotNull(ri.effective_target());
        assertEquals(TAG_INTERNET_IOP.value, ri.effective_profile().tag);

        TaggedComponent componentEncoding = ri.get_effective_component(MY_COMPONENT_ID.value);
        byte[] componentData = componentEncoding.component_data;
        Any componentAny = null;
        try {
            componentAny = cdrCodec.decode_value(componentData, MyComponentHelper.type());
        } catch (FormatMismatch | TypeMismatch ex) {
            fail();
        }
        MyComponent component = MyComponentHelper.extract(componentAny);
        assertEquals(10, component.val);

        Policy policy = ri.get_request_policy(MY_CLIENT_POLICY_ID.value);
        MyClientPolicy myClientPolicy = MyClientPolicyHelper.narrow(policy);
        assertNotNull(myClientPolicy);
        assertEquals(10, myClientPolicy.value());

        assertEquals(user ? USER_EXCEPTION.value : SYSTEM_EXCEPTION.value, ri.reply_status());

        try {
            Any rc = ri.received_exception();
            if (!user) {
                Util.readSysEx(rc.create_input_stream());
            } else {
                assertEquals(userHelper.id(), ri.received_exception_id());
                userHelper.extract(rc);
            }
        } catch (BAD_INV_ORDER ex) {
            fail();
        }

        try {
            ri.forward_reference();
            fail();
        } catch (BAD_INV_ORDER ex) {
            // Expected
        }

        try {
            ri.get_reply_service_context(REPLY_CONTEXT_1_ID.value);
            fail();
        } catch (BAD_PARAM ex) {
            // Expected
        }

        try {
            ServiceContext sc = ri.get_request_service_context(REQUEST_CONTEXT_ID.value);
            fail();
        } catch (BAD_PARAM ex) {
            // Expected
        }

        try {
            ServiceContext sc = new ServiceContext();
            sc.context_id = REQUEST_CONTEXT_ID.value;
            ri.add_request_service_context(sc, false);
        } catch (BAD_INV_ORDER ex) {
            // Expected
        }

        Any slotData;
        try {
            slotData = pic.get_slot(slotId);
        } catch (InvalidSlot ex) {
            throw (Error) fail();
        }
        assertEquals(15, slotData.extract_long());

        Any newSlotData = ORB.init().create_any();
        newSlotData.insert_long(16);
        try {
            pic.set_slot(slotId, newSlotData);
        } catch (InvalidSlot ex) {
            fail();
        }
    }

    int _OB_numReq() {
        return req;
    }
}
