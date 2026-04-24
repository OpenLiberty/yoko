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

import org.hamcrest.MatcherAssert;
import org.omg.CORBA.Any;
import org.omg.CORBA.BAD_INV_ORDER;
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.INV_POLICY;
import org.omg.CORBA.LocalObject;
import org.omg.CORBA.NO_RESOURCES;
import org.omg.CORBA.ORB;
import org.omg.CORBA.ParameterMode;
import org.omg.CORBA.Policy;
import org.omg.CORBA.TCKind;
import org.omg.CORBA.TypeCode;
import org.omg.Dynamic.Parameter;
import org.omg.IOP.Codec;
import org.omg.IOP.CodecFactory;
import org.omg.IOP.ENCODING_CDR_ENCAPS;
import org.omg.IOP.Encoding;
import org.omg.IOP.ServiceContext;
import org.omg.PortableInterceptor.TestInterfacePackage.sHelper;
import org.omg.PortableInterceptor.TestInterfacePackage.userHelper;

import static org.apache.yoko.orb.OB.Util.readSysEx;
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

final class ServerTestInterceptor_impl extends LocalObject implements ServerRequestInterceptor {
    private final Codec cdrCodec;
    private final int slotId;

    ServerTestInterceptor_impl(int slotId, CodecFactory factory) {
        this.slotId = slotId;
        Encoding how = new Encoding((byte) ENCODING_CDR_ENCAPS.value, (byte) 0, (byte) 0);
        cdrCodec = assertDoesNotThrow(() -> factory.create_codec(how));
        assertNotNull(cdrCodec);
    }

    private void testArgs(ServerRequestInfo ri, boolean resultAvail) {
        String op = ri.operation();
        Parameter[] args = ri.arguments();
        if (op.startsWith("_set_") || op.startsWith("_get_")) {
            // struct or string?
            final boolean isstr = (op.contains("string"));
            if (op.startsWith("_get_")) {
                assertEquals(0, args.length);
                if (resultAvail) {
                    // Test: result
                    Any result = ri.result();
                    String str = isstr ? result.extract_string() : sHelper.extract(result).sval;
                    assertTrue(str.startsWith("TEST"));
                }
            } else {
                assertEquals(1, args.length);
                assertSame(PARAM_IN, args[0].mode);
                if (resultAvail) {
                    String str = isstr ? args[0].argument.extract_string() : sHelper.extract(args[0].argument).sval;
                    assertTrue(str.startsWith("TEST"));
                }
            }
        } else if (op.startsWith("one_")) {
            String which = op.substring(4); // Which operation?
            ParameterMode mode; // The parameter mode

            // struct or string?
            final boolean isstr = which.startsWith("string");

            which = which.substring(7); // Skip <string|struct>_

            if (which.equals("return")) {
                assertEquals(0, args.length);
                if (resultAvail) {
                    // Test: result
                    Any result = ri.result();
                    String str = isstr ? result.extract_string() : sHelper.extract(result).sval;
                    assertTrue(str.startsWith("TEST"));
                }
            } else {
                assertEquals(1, args.length);
                switch (which) {
                    case "in": mode = PARAM_IN;break;
                    case "inout": mode = PARAM_INOUT;break;
                    case "out": mode = PARAM_OUT; break;
                    default: throw (Error) fail();
                }

                assertSame(mode, args[0].mode);

                if (mode != PARAM_OUT || resultAvail) {
                    String str = isstr ? args[0].argument.extract_string() : sHelper.extract(args[0].argument).sval;
                    assertTrue(str.startsWith("TEST"));

                    if (resultAvail) {
                        // Test: result
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
            // Test: result is not available
            assertThrows(BAD_INV_ORDER.class, ri::result);
        }
    }

    private void testServiceContext(String op, ServerRequestInfo ri, boolean addContext) {
        if (op.equals("test_service_context")) {
            // Test: get_request_service_context
            final ServiceContext reqSvcCtx = assertDoesNotThrow(() -> ri.get_request_service_context(REQUEST_CONTEXT_ID.value));
            assertEquals(REQUEST_CONTEXT_ID.value, reqSvcCtx.context_id);

            // Test: get_reply_service_context
            final ServiceContext replySvcCtx = assertDoesNotThrow(() -> ri.get_reply_service_context(REPLY_CONTEXT_4_ID.value));
            Any repSvcCtxAny = assertDoesNotThrow(() -> cdrCodec.decode_value(replySvcCtx.context_data, ReplyContextHelper.type()));
            ReplyContext context = ReplyContextHelper.extract(repSvcCtxAny);
            assertEquals("reply4", context.data);
            assertEquals(114, context.val);

            if (addContext) {
                // Test: add_reply_service_context
                context.data = "reply3";
                context.val = 103;
                final Any any = ORB.init().create_any();
                ReplyContextHelper.insert(any, context);
                byte[] replyCtxData = assertDoesNotThrow(() -> cdrCodec.encode_value(any));

                replySvcCtx.context_id = REPLY_CONTEXT_3_ID.value;
                replySvcCtx.context_data = replyCtxData;

                assertDoesNotThrow(() -> ri.add_reply_service_context(replySvcCtx, false));

                // Test: add same context again (no replace)
                assertThrows(BAD_INV_ORDER.class, () -> ri.add_reply_service_context(replySvcCtx, false));

                // Test: add same context again (replace)
                assertDoesNotThrow(() -> ri.add_reply_service_context(replySvcCtx, true));

                // Test: replace context added in receive_request
                context.data = "reply4";
                context.val = 124;
                ReplyContextHelper.insert(any, context);
                replyCtxData = assertDoesNotThrow(() -> cdrCodec.encode_value(any));

                replySvcCtx.context_id = REPLY_CONTEXT_4_ID.value;
                replySvcCtx.context_data = replyCtxData;
                assertDoesNotThrow(() -> ri.add_reply_service_context(replySvcCtx, true));
            }
        } else {
            assertThrows(BAD_PARAM.class, () -> ri.get_request_service_context(REPLY_CONTEXT_1_ID.value));
            assertThrows(BAD_PARAM.class, () -> ri.get_reply_service_context(REPLY_CONTEXT_1_ID.value));
        }
    }

    public String name() { return "ServerTestInterceptor"; }

    public void destroy() {}

    public void receive_request_service_contexts(ServerRequestInfo ri) {
        try {
            // Test: get operation name
            String op = ri.operation();

            boolean oneway = (op.equals("noargs_oneway"));

            assertThrows(BAD_INV_ORDER.class, ri::arguments);
            assertThrows(BAD_INV_ORDER.class, ri::result);
            assertThrows(BAD_INV_ORDER.class, ri::exceptions);
            assertTrue(oneway != ri.response_expected());
            assertThrows(BAD_INV_ORDER.class, ri::reply_status);
            assertThrows(BAD_INV_ORDER.class, ri::forward_reference);
            assertThrows(BAD_INV_ORDER.class, ri::object_id);
            assertThrows(BAD_INV_ORDER.class, ri::adapter_id);
            assertThrows(BAD_INV_ORDER.class, ri::target_most_derived_interface);
            assertThrows(BAD_INV_ORDER.class, ri::server_id);
            assertThrows(BAD_INV_ORDER.class, ri::orb_id);
            assertThrows(BAD_INV_ORDER.class, ri::adapter_name);
            assertThrows(BAD_INV_ORDER.class, () -> ri.target_is_a(""));

            if (op.equals("test_service_context")) {
                // Test: get_request_service_context
                {
                    ServiceContext sc = ri.get_request_service_context(REQUEST_CONTEXT_ID.value);
                    assertEquals(REQUEST_CONTEXT_ID.value, sc.context_id);
                    byte[] data = new byte[sc.context_data.length];
                    System.arraycopy(sc.context_data, 0, data, 0, sc.context_data.length);

                    Any any = assertDoesNotThrow(() -> cdrCodec.decode_value(data, RequestContextHelper.type()));
                    RequestContext context = RequestContextHelper.extract(any);
                    assertEquals("request", context.data);
                    assertEquals(10, context.val);

                    // Test: PortableInterceptor::Current
                    Any slotData = ORB.init().create_any();
                    slotData.insert_long(context.val);
                    assertDoesNotThrow(() -> ri.set_slot(slotId, slotData));
                }

                // Test: add_reply_service_context
                ReplyContext context = new ReplyContext();
                context.data = "reply1";
                context.val = 101;
                Any any = ORB.init().create_any();
                ReplyContextHelper.insert(any, context);
                byte[] data = assertDoesNotThrow(() -> cdrCodec.encode_value(any));
                ServiceContext sc = new ServiceContext(REPLY_CONTEXT_1_ID.value, data);
                assertDoesNotThrow(() -> ri.add_reply_service_context(sc, false));
                // Test: add same context again (no replace)
                assertThrows(BAD_INV_ORDER.class, () -> ri.add_reply_service_context(sc, false));
                // Test: add same context again (replace)
                assertDoesNotThrow(() -> ri.add_reply_service_context(sc, true));

                // Test: add second context
                context.data = "reply4";
                context.val = 104;
                ReplyContextHelper.insert(any, context);
                data = assertDoesNotThrow(() -> cdrCodec.encode_value(any));
                sc.context_id = REPLY_CONTEXT_4_ID.value;
                sc.context_data = data;
                ri.add_reply_service_context(sc, false);
            } else {
                // Test: get_request_service_context
                assertThrows(BAD_PARAM.class, () -> ri.get_request_service_context(REQUEST_CONTEXT_ID.value));
            }

            // Test: get_reply_service_context
            assertThrows(BAD_INV_ORDER.class, () -> ri.get_reply_service_context(REPLY_CONTEXT_1_ID.value));

            // Test: sending exception is not available
            assertThrows(BAD_INV_ORDER.class, ri::sending_exception);

            // Test: get_server_policy
            Policy policy = ri.get_server_policy(MY_SERVER_POLICY_ID.value);
            MyServerPolicy myServerPolicy = MyServerPolicyHelper.narrow(policy);
            assertNotNull(myServerPolicy);
            assertEquals(10, myServerPolicy.value());

            // try to get an invalid policy type
            assertThrows(INV_POLICY.class, () -> ri.get_server_policy(-1));
        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        }
    }

    public void receive_request(ServerRequestInfo ri) {
        try {
            // Test: get operation name
            String op = ri.operation();

            boolean oneway = (op.equals("noargs_oneway"));

            // Test: Examine arguments
            testArgs(ri, false);

            // Test: result is not available
            assertThrows(BAD_INV_ORDER.class, ri::result);

            // Test: exceptions
            try {
                TypeCode[] exceptions = ri.exceptions();
                if (op.equals("userexception")) {
                    assertEquals(1, exceptions.length);
                    assertTrue(exceptions[0].equal(userHelper.type()));
                } else {
                    assertEquals(0, exceptions.length);
                }
            } catch (NO_RESOURCES ex) {
                // Expected (if servant is DSI)
            }

            // Test: response expected and oneway should be equivalent
            assertTrue(oneway != ri.response_expected());

            // Test: reply status is not available
            assertThrows(BAD_INV_ORDER.class, ri::reply_status);

            // Test: forward reference is not available
            assertThrows(BAD_INV_ORDER.class, ri::forward_reference);

            if (op.equals("test_service_context")) {
                // Test: get_request_service_context
                final ServiceContext reqSvcCtx = assertDoesNotThrow(() -> ri.get_request_service_context(REQUEST_CONTEXT_ID.value));
                assertEquals(REQUEST_CONTEXT_ID.value, reqSvcCtx.context_id);

                //
                // Test: add_reply_service_context
                //
                Any any = ORB.init().create_any();
                ReplyContextHelper.insert(any, new ReplyContext("reply2", 102));
                byte[] data = assertDoesNotThrow(() -> cdrCodec.encode_value(any));

                ServiceContext replySvcCtx = new ServiceContext(REPLY_CONTEXT_2_ID.value, data);
                assertDoesNotThrow(() -> ri.add_reply_service_context(replySvcCtx, false));
                // Test: add same context again (no replace)
                assertThrows(BAD_INV_ORDER.class, () -> ri.add_reply_service_context(replySvcCtx, false));
                // Test: add same context again (replace)
                assertDoesNotThrow(() -> ri.add_reply_service_context(replySvcCtx, true));

                // Test: replace context added in receive_request_service_context
                ReplyContextHelper.insert(any, new ReplyContext("reply4", 114));
                replySvcCtx.context_id = REPLY_CONTEXT_4_ID.value;
                replySvcCtx.context_data = assertDoesNotThrow(() -> cdrCodec.encode_value(any));
                assertDoesNotThrow(() -> ri.add_reply_service_context(replySvcCtx, true));
            } else {
                // Test: get_request_service_context
                assertThrows(BAD_PARAM.class, () -> ri.get_request_service_context(REQUEST_CONTEXT_ID.value));
            }

            // Test: get_reply_service_context
            assertThrows(BAD_INV_ORDER.class, () -> ri.get_reply_service_context(REPLY_CONTEXT_1_ID.value));
            // Test: sending exception is not available
            assertThrows(BAD_INV_ORDER.class, ri::sending_exception);

            // Test: object id is correct
            byte[] oid = ri.object_id();
            assertTrue((oid.length == 4 && (new String(oid)).equals("test"))
                        || (oid.length == 7 && (new String(oid)).equals("testDSI")));

            // Test: adapter id is correct (this is a tough one to test)
            assertTrue(ri.adapter_id().length != 0);

            // Test: servant most derived interface is correct
            assertEquals("IDL:TestInterface:1.0", ri.target_most_derived_interface());

            // Test: server id is correct
            assertEquals("", ri.server_id());

            // Test: orb id is correct
            MatcherAssert.assertThat(ri.orb_id(), is(oneOf("server orb", "collocated orb")));

            // Test: adapter name is correct
            String[] adapterName = ri.adapter_name();
            assertTrue(adapterName.length > 0);
            assertEquals("persistent", adapterName[adapterName.length - 1]);

            // Test: servant is_a is correct
            assertTrue(ri.target_is_a("IDL:TestInterface:1.0"));

            // Test: get_server_policy
            Policy policy = ri.get_server_policy(MY_SERVER_POLICY_ID.value);
            MyServerPolicy myServerPolicy = MyServerPolicyHelper.narrow(policy);
            assertNotNull(myServerPolicy);
            assertEquals(10, myServerPolicy.value());

            assertThrows(INV_POLICY.class, () -> ri.get_server_policy(-1));
        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        }
    }

    public void send_reply(ServerRequestInfo ri) {
        try {
            // Test: get operation name
            String op = ri.operation();

            // If "deactivate" then we're done
            if (op.equals("deactivate")) return;

            boolean oneway = op.equals("noargs_oneway");

            // Test: Arguments should be available
            testArgs(ri, true);

            // Test: exceptions
            try {
                TypeCode[] exceptions = ri.exceptions();
                if (op.equals("userexception")) {
                    assertEquals(1, exceptions.length);
                    assertTrue(exceptions[0].equal(userHelper.type()));
                } else {
                    assertEquals(0, exceptions.length);
                }
            } catch (NO_RESOURCES ex) {
                // Expected (if servant is DSI)
            }

            assertTrue(oneway != ri.response_expected());

            // Test: reply status is available
            assertEquals(SUCCESSFUL.value, ri.reply_status());

            // Test: forward reference is not available
            assertThrows(BAD_INV_ORDER.class, ri::forward_reference);

            // Test: get_request_service_context
            // Test: get_reply_service_context
            // Test: add_reply_service_context
            testServiceContext(op, ri, true);

            // Test: sending exception is not available
            assertThrows(BAD_INV_ORDER.class, ri::sending_exception);

            // Test: object id is correct
            byte[] oid = ri.object_id();
            assertTrue((oid.length == 4 && (new String(oid)).equals("test"))
                        || (oid.length == 7 && (new String(oid)).equals("testDSI")));

            // Test: adapter id is correct (this is a tough one to test)
            byte[] adapterId = ri.adapter_id();
            assertTrue(adapterId.length != 0);

            // Test: target_most_derived_interface raises BAD_INV_ORDER
            assertThrows(BAD_INV_ORDER.class, ri::target_most_derived_interface);

            // Test: server id is correct
            assertEquals("", ri.server_id());

            // Test: orb id is correct
            MatcherAssert.assertThat(ri.orb_id(), is(oneOf("server orb", "collocated orb")));

            // Test: adapter name is correct
            String[] adapterName = ri.adapter_name();
            assertTrue(adapterName.length > 0);
            assertEquals("persistent", adapterName[adapterName.length - 1]);

            // Test: target_is_a raises BAD_INV_ORDER
            assertThrows(BAD_INV_ORDER.class, () -> ri.target_is_a("IDL:TestInterface:1.0"));

            // Test: get_server_policy
            Policy policy = ri.get_server_policy(MY_SERVER_POLICY_ID.value);
            MyServerPolicy myServerPolicy = MyServerPolicyHelper.narrow(policy);
            assertNotNull(myServerPolicy);
            assertEquals(10, myServerPolicy.value());

            assertThrows(INV_POLICY.class, () -> ri.get_server_policy(-1));

            // Test: get_slot
            if (op.equals("test_service_context")) {
                int val;
                Any slotData = assertDoesNotThrow(() -> ri.get_slot(slotId));
                val = slotData.extract_long();
                assertEquals(20, val);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        }
    }

    public void send_other(ServerRequestInfo ri) {
        try {
            // Test: get operation name
            String op = ri.operation();

            assertEquals("location_forward", op);

            // Test: Arguments should not be available
            assertThrows(BAD_INV_ORDER.class, ri::arguments);

            // Test: exceptions
            assertThrows(BAD_INV_ORDER.class, ri::exceptions);

            // Test: response expected should be true
            assertTrue(ri.response_expected());

            // Test: reply status is available
            assertEquals(LOCATION_FORWARD.value, ri.reply_status());

            // Test: forward reference is available
            assertDoesNotThrow(ri::forward_reference);

            // Test: get_request_service_context
            // Test: get_reply_service_context
            testServiceContext(op, ri, false);

            // Test: sending exception is not available
            assertThrows(BAD_INV_ORDER.class, ri::sending_exception);

            // Test: object id is correct
            byte[] oid = ri.object_id();
            assertTrue((oid.length == 4 && (new String(oid)).equals("test"))
                        || (oid.length == 7 && (new String(oid)).equals("testDSI")));

            // Test: adapter id is correct (this is a tough one to test)
            byte[] adapterId = ri.adapter_id();
            assertTrue(adapterId.length != 0);

            // Test: target_most_derived_interface raises BAD_INV_ORDER
            assertThrows(BAD_INV_ORDER.class, ri::target_most_derived_interface);

            // Test: server id is correct
            assertEquals("", ri.server_id());

            // Test: orb id is correct
            MatcherAssert.assertThat(ri.orb_id(), is(oneOf("server orb", "collocated orb")));

            // Test: adapter name is correct
            String[] adapterName = ri.adapter_name();
            assertTrue(adapterName.length > 0);
            assertEquals("persistent", adapterName[adapterName.length - 1]);

            // Test: target_is_a raises BAD_INV_ORDER
            assertThrows(BAD_INV_ORDER.class, () -> ri.target_is_a("IDL:TestInterface:1.0"));

            // Test: get_server_policy
            Policy policy = ri.get_server_policy(MY_SERVER_POLICY_ID.value);
            MyServerPolicy myServerPolicy = MyServerPolicyHelper.narrow(policy);
            assertNotNull(myServerPolicy);
            assertEquals(10, myServerPolicy.value());

            assertThrows(INV_POLICY.class, () -> ri.get_server_policy(-1));
        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        }
    }

    public void send_exception(ServerRequestInfo ri) {
        try {
            // Test: get operation name
            String op = ri.operation();

            final boolean user;
            switch (op) {
                case "deactivate": return;
                case "userexception": user = true; break;
                case "systemexception": user = false; break;
                default: throw (Error) fail("Unexpected op name: " + op);
            }

            assertThrows(BAD_INV_ORDER.class, ri::arguments);
            assertThrows(BAD_INV_ORDER.class, ri::result);
            try {
                TypeCode[] exceptions = ri.exceptions();
                if (user) {
                    assertEquals(1, exceptions.length);
                    assertTrue(exceptions[0].equal(userHelper.type()));
                } else {
                    assertEquals(0, exceptions.length);
                }
            } catch (BAD_INV_ORDER ex) {
                fail();
            } catch (NO_RESOURCES ex) {
                // Expected (if servant is DSI)
            }

            assertTrue(ri.response_expected());
            assertEquals(user ? USER_EXCEPTION.value : SYSTEM_EXCEPTION.value, ri.reply_status());
            assertThrows(BAD_INV_ORDER.class, ri::forward_reference);
            testServiceContext(op, ri, false);
            try {
                Any any = ri.sending_exception();
                Exception e = user ? userHelper.extract(any) : readSysEx(any.create_input_stream());
            } catch (BAD_INV_ORDER ex) {
                fail();
            } catch (NO_RESOURCES ex) // TODO: remove this!
            {
            }

            // Test: object id is correct
            byte[] oid = ri.object_id();
            assertTrue((oid.length == 4 && (new String(oid)).equals("test"))
                        || (oid.length == 7 && (new String(oid)).equals("testDSI")));

            // Test: adapter id is correct (this is a tough one to test)
            byte[] adapterId = ri.adapter_id();
            assertTrue(adapterId.length != 0);

            // Test: target_most_derived_interface raises BAD_INV_ORDER
            assertThrows(BAD_INV_ORDER.class, ri::target_most_derived_interface);

            // Test: server id is correct
            assertEquals("", ri.server_id());

            // Test: orb id is correct
            // Test: orb id is correct
            MatcherAssert.assertThat(ri.orb_id(), is(oneOf("server orb", "collocated orb")));

            // Test: adapter name is correct
            String[] adapterName = ri.adapter_name();
            assertTrue(adapterName.length > 0);
            assertEquals("persistent", adapterName[adapterName.length - 1]);

            // Test: target_is_a raises BAD_INV_ORDER
            assertThrows(BAD_INV_ORDER.class, () -> ri.target_is_a("IDL:TestInterface:1.0"));

            // Test: get_server_policy
            Policy policy = ri.get_server_policy(MY_SERVER_POLICY_ID.value);
            MyServerPolicy myServerPolicy = MyServerPolicyHelper.narrow(policy);
            assertNotNull(myServerPolicy);
            assertEquals(10, myServerPolicy.value());

            assertThrows(INV_POLICY.class, () -> ri.get_server_policy(-1));
        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        }
    }
}
