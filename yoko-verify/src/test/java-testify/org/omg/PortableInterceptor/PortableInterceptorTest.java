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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.omg.CORBA.Any;
import org.omg.CORBA.BAD_INV_ORDER;
import org.omg.CORBA.INV_POLICY;
import org.omg.CORBA.NO_IMPLEMENT;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Object;
import org.omg.CORBA.Policy;
import org.omg.CORBA.Request;
import org.omg.CORBA.StringHolder;
import org.omg.CORBA.SystemException;
import org.omg.IOP.Codec;
import org.omg.IOP.CodecFactory;
import org.omg.IOP.CodecFactoryHelper;
import org.omg.IOP.CodecFactoryPackage.UnknownEncoding;
import org.omg.IOP.CodecPackage.InvalidTypeForEncoding;
import org.omg.IOP.ENCODING_CDR_ENCAPS;
import org.omg.IOP.Encoding;
import org.omg.IOP.TaggedComponent;
import org.omg.PortableInterceptor.ORBInitInfoPackage.DuplicateName;
import org.omg.PortableInterceptor.TestInterfacePackage.s;
import org.omg.PortableInterceptor.TestInterfacePackage.sHelper;
import org.omg.PortableInterceptor.TestInterfacePackage.sHolder;
import org.omg.PortableInterceptor.TestInterfacePackage.user;
import org.omg.PortableInterceptor.TestInterfacePackage.userHelper;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAManager;
import org.omg.PortableServer.ServantLocator;
import testify.annotation.Logging;
import testify.bus.Bus;
import testify.bus.key.StringKey;
import testify.iiop.TestIORInterceptor;
import testify.iiop.TestORBInitializer;
import testify.iiop.annotation.ConfigureOrb.UseWithOrb;
import testify.iiop.annotation.ConfigureServer;
import testify.iiop.annotation.ConfigureServer.BeforeServer;

import static java.util.Objects.requireNonNull;
import static org.apache.yoko.orb.PortableServer.PolicyValue.NON_RETAIN;
import static org.apache.yoko.orb.PortableServer.PolicyValue.NO_IMPLICIT_ACTIVATION;
import static org.apache.yoko.orb.PortableServer.PolicyValue.PERSISTENT;
import static org.apache.yoko.orb.PortableServer.PolicyValue.USER_ID;
import static org.apache.yoko.orb.PortableServer.PolicyValue.USE_SERVANT_MANAGER;
import static org.apache.yoko.orb.PortableServer.PolicyValue.create_POA;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.omg.CORBA.SetOverrideType.ADD_OVERRIDE;
import static org.omg.CORBA.TCKind.tk_string;
import static org.omg.CORBA.TCKind.tk_void;
import static org.omg.PortableInterceptor.PortableInterceptorTest.IorKey.DSI_IMPL;
import static org.omg.PortableInterceptor.PortableInterceptorTest.IorKey.IMPL;
import static testify.iiop.annotation.ConfigureOrb.OrbId.CLIENT_ORB;
import static testify.iiop.annotation.ConfigureOrb.OrbId.SERVER_ORB;

@ConfigureServer
public class PortableInterceptorTest {
    static ClientProxyManager clientProxyManager;
    private static Policy[] policies;
    private static Bus clientBus;
    private static ORB clientOrb;
    static int clientSlotId;
    static int serverSlotId;

    @UseWithOrb(CLIENT_ORB)
    public static class ClientOrbInitializer implements TestORBInitializer {
        @Override
        public void pre_init(ORBInitInfo info) {
            clientSlotId = info.allocate_slot_id();
            assertTrue(clientSlotId >= 0);
            System.out.println("### registering client policy factory");
            PolicyFactory pf = new MyClientPolicyFactory_impl();
            info.register_policy_factory(MY_CLIENT_POLICY_ID.value, pf);
            clientProxyManager = new ClientProxyManager(info);
        }
    }

    @UseWithOrb(SERVER_ORB)
    public static class ServerOrbInitializer implements TestORBInitializer {
        public void pre_init(ORBInitInfo info) {
            info.register_policy_factory(MY_SERVER_POLICY_ID.value, new MyServerPolicyFactory_impl());
            try {
                serverSlotId = info.allocate_slot_id();
                info.add_server_request_interceptor(new ServerTestInterceptor_impl(serverSlotId, info.codec_factory()));
            } catch (DuplicateName e) {
                fail(e);
            }
        }
    }

    @UseWithOrb(SERVER_ORB)
    public static class IorInterceptor implements TestIORInterceptor {
        private Codec cdrCodec;

        @Override
        public void pre_init(ORBInitInfo info) {
            int id = info.allocate_slot_id();
            assertTrue(id >= 0);
            CodecFactory factory = info.codec_factory();

            Encoding how = new Encoding((byte) ENCODING_CDR_ENCAPS.value, (byte) 0, (byte) 0);

            try {
                cdrCodec = factory.create_codec(how);
            } catch (UnknownEncoding e) {
                throw new RuntimeException(e);
            }
        }

        public void establish_components(IORInfo info) {
            try {
                final MyServerPolicy policy;
                {
                    Policy p = info.get_effective_policy(MY_SERVER_POLICY_ID.value);
                    if (p == null) return;
                    policy = MyServerPolicyHelper.narrow(p);
                }
                MyComponent content = new MyComponent();
                content.val = policy.value();
                Any any = ORB.init().create_any();
                MyComponentHelper.insert(any, content);

                byte[] encoding = null;
                try {
                    encoding = cdrCodec.encode_value(any);
                } catch (InvalidTypeForEncoding ex) {
                    throw new RuntimeException();
                }

                TaggedComponent component = new TaggedComponent(MY_COMPONENT_ID.value, encoding);
                info.add_ior_component(component);
            } catch (INV_POLICY ex) {
                return;
            }
        }
    }

    enum IorKey implements StringKey {IMPL, DSI_IMPL}

    @BeforeServer
    public static void beforeServer(Bus serverBus, ORB serverOrb, POA rootPoa) throws Exception {
        assertNotNull(CodecFactoryHelper.narrow(serverOrb.resolve_initial_references("CodecFactory")));
        POAManager poaMgr = rootPoa.the_POAManager();
        Any mySvrPolVal = serverOrb.create_any();
        mySvrPolVal.insert_long(10);
        Policy mySvrPol = serverOrb.create_policy(MY_SERVER_POLICY_ID.value, mySvrPolVal);
        POA persistentPOA = create_POA("persistent", rootPoa, poaMgr,
                PERSISTENT, USER_ID, USE_SERVANT_MANAGER, NON_RETAIN, NO_IMPLICIT_ACTIVATION,
                poa -> mySvrPol);

        // Create implementation objects
        TestInterface_impl impl = new TestInterface_impl(serverOrb, persistentPOA, serverSlotId);
        TestInterfaceDSI_impl dsiImpl = new TestInterfaceDSI_impl(serverOrb, serverSlotId);

        // Install the servant locator in the POA
        TestLocator_impl locatorImpl = new TestLocator_impl(serverOrb, impl, dsiImpl);
        ServantLocator locator = locatorImpl._this(serverOrb);
        persistentPOA.set_servant_manager(locator);

        // Create and publish the refs
        org.omg.CORBA.Object objImpl = persistentPOA.create_reference_with_id("test".getBytes(), "IDL:TestInterface:1.0");
        org.omg.CORBA.Object objDSIImpl = persistentPOA.create_reference_with_id("testDSI".getBytes(), "IDL:TestInterface:1.0");
        serverBus.put(IMPL, serverOrb.object_to_string(objImpl));
        serverBus.put(DSI_IMPL, serverOrb.object_to_string(objDSIImpl));
    }

    @BeforeAll
    public static void beforeClient(Bus clientBus, ORB clientOrb) throws Exception {
        PortableInterceptorTest.clientBus = clientBus;
        PortableInterceptorTest.clientOrb = clientOrb;
        // Test: Create a policy set on the object-reference
        Any any = clientOrb.create_any();
        any.insert_long(10);
        policies = new Policy[]{ clientOrb.create_policy(MY_CLIENT_POLICY_ID.value, any) };
    }

    private static TestInterface getTestInterface(IorKey key) {
        String impl = clientBus.get(key);
        Object obj = clientOrb.string_to_object(impl)._set_policy_override(policies, ADD_OVERRIDE);
        return requireNonNull(TestInterfaceHelper.narrow(obj));
    }

    @AfterEach
    public void clearClientInterceptors() { clientProxyManager.clearInterceptors(); }

    @Test void simpleTest(ORB orb) {}

    @ParameterizedTest(name = "{displayName}[{index}] - {0}")
    @EnumSource(IorKey.class)
    void testTranslation(IorKey key) {
        var ti = getTestInterface(key);
        // Set up the correct interceptor
        TranslateCallInterceptor_impl i0 = new TranslateCallInterceptor_impl();
        TranslateCallInterceptor_impl i1 = new TranslateCallInterceptor_impl();
        TranslateCallInterceptor_impl i2 = new TranslateCallInterceptor_impl();

        clientProxyManager.setInterceptor(0, i0);
        clientProxyManager.setInterceptor(1, i1);
        clientProxyManager.setInterceptor(2, i2);

        i0.throwOnRequest(new NO_PERMISSION());
        assertThrows(NO_PERMISSION.class, ti::noargs);
        i0.noThrowOnRequest();

        i0.throwOnReply(new NO_PERMISSION());
        assertThrows(NO_PERMISSION.class, ti::noargs);
        i0.noThrowOnReply();

        i1.throwOnReply(new NO_PERMISSION());
        i0.expectException(new NO_PERMISSION());
        assertThrows(NO_PERMISSION.class, ti::noargs);
        i1.noThrowOnReply();

        i0.expectException(new NO_PERMISSION());
        i1.expectException(new BAD_INV_ORDER());
        i1.throwOnException(new NO_PERMISSION());
        i2.throwOnRequest(new BAD_INV_ORDER());
        assertThrows(NO_PERMISSION.class, ti::noargs);
        i2.noThrowOnRequest();

        i2.throwOnReply(new BAD_INV_ORDER());
        assertThrows(NO_PERMISSION.class, ti::noargs);
    }

    @ParameterizedTest
    @EnumSource(IorKey.class)
    @Logging("yoko.verbose.giop.out")
    void testInvocation(IorKey key) {
        TestInterface ti = getTestInterface(key);
        Object obj = assertDoesNotThrow(() -> clientOrb.resolve_initial_references("PICurrent"));
        Current pic = CurrentHelper.narrow(obj);
        Any slotData = ORB.init().create_any();
        slotData.insert_long(10);
        assertDoesNotThrow(() -> pic.set_slot(clientSlotId, slotData));

        // Set up the correct interceptor
        CallInterceptor_impl impl = new CallInterceptor_impl(clientOrb, clientSlotId);
        clientProxyManager.setInterceptor(0, impl);
        int num = 0;

        ti.noargs();
        assertEquals(++num, impl._OB_numReq());

        ti.noargs_oneway();
        assertEquals(++num, impl._OB_numReq());

        assertThrows(user.class, ti::userexception);
        assertEquals(++num, impl._OB_numReq());

        assertThrows(SystemException.class, ti::systemexception);
        assertEquals(++num, impl._OB_numReq());

        ti.test_service_context();
        assertEquals(++num, impl._OB_numReq());

        assertThrows(NO_IMPLEMENT.class, ti::location_forward);
        assertEquals(++num, impl._OB_numReq());

        //
        // Test simple attribute
        //
        ti.string_attrib("TEST");
        assertEquals(++num, impl._OB_numReq());
        String satt = ti.string_attrib();
        assertEquals("TEST", satt);
        assertEquals(++num, impl._OB_numReq());

        //
        // Test in, inout and out simple parameters
        //
        ti.one_string_in("TEST");
        assertEquals(++num, impl._OB_numReq());

        StringHolder spinout = new StringHolder("TESTINOUT");
        ti.one_string_inout(spinout);
        assertEquals("TEST", spinout.value);
        assertEquals(++num, impl._OB_numReq());

        StringHolder spout = new StringHolder();
        ti.one_string_out(spout);
        assertEquals("TEST", spout.value);
        assertEquals(++num, impl._OB_numReq());

        String sprc = ti.one_string_return();
        assertEquals("TEST", sprc);
        assertEquals(++num, impl._OB_numReq());

        // Test struct attribute
        s ss = new s();
        ss.sval = "TEST";
        ti.struct_attrib(ss);
        assertEquals(++num, impl._OB_numReq());
        s ssatt = ti.struct_attrib();
        assertEquals("TEST", ssatt.sval);
        assertEquals(++num, impl._OB_numReq());

        //
        // Test in, inout and out struct parameters
        //
        ti.one_struct_in(ss);
        assertEquals(++num, impl._OB_numReq());

        sHolder sinout = new sHolder(new s("TESTINOUT"));
        ti.one_struct_inout(sinout);
        assertEquals("TEST", sinout.value.sval);
        assertEquals(++num, impl._OB_numReq());

        sHolder sout = new sHolder();
        ti.one_struct_out(sout);
        assertEquals("TEST", sout.value.sval);
        assertEquals(++num, impl._OB_numReq());

        s ssrc = ti.one_struct_return();
        assertEquals("TEST", ssrc.sval);
        assertEquals(++num, impl._OB_numReq());

        // Test: PortableInterceptor::Current still has the same value
        Any slotData2 = null;
        slotData2 = assertDoesNotThrow(() -> pic.get_slot(clientSlotId));
        int v = slotData2.extract_long();
        assertEquals(10, v);
    }

    @ParameterizedTest
    @EnumSource(IorKey.class)
    @Logging("yoko.verbose.giop.out")
    void testDynamicInvocation(IorKey key) {
        TestInterface ti = getTestInterface(key);
        Current pic;
        pic = assertDoesNotThrow(() -> CurrentHelper.narrow(clientOrb.resolve_initial_references("PICurrent")));

        Any slotData = clientOrb.create_any();
        slotData.insert_long(10);

        assertDoesNotThrow(() -> pic.set_slot(clientSlotId, slotData));

        // Set up the correct interceptor
        CallInterceptor_impl impl = new CallInterceptor_impl(clientOrb, clientSlotId);
        clientProxyManager.setInterceptor(0, impl);
        int num = 0;

        Request req;
        req = ti._request("noargs");
        req.invoke();
        assertEquals(++num, impl._OB_numReq());

        req = ti._request("noargs_oneway");
        req.send_oneway();
        assertEquals(++num, impl._OB_numReq());

        req = ti._request("userexception");
        req.exceptions().add(userHelper.type());
        req.invoke();
        assertEquals(++num, impl._OB_numReq());

        req = ti._request("systemexception");
        assertThrows(NO_IMPLEMENT.class, req::invoke); // raised by remote servante
        assertEquals(++num, impl._OB_numReq());

        req = ti._request("location_forward");
        assertThrows(NO_IMPLEMENT.class, req::invoke); // raised by local interceptor
        assertEquals(++num, impl._OB_numReq());

        // Test in, inout and out simple parameters
        {
            req = ti._request("one_string_in");
            req.set_return_type(clientOrb.get_primitive_tc(tk_void));
            req.add_in_arg().insert_string("TEST");
            req.invoke();
            assertEquals(++num, impl._OB_numReq());

            req = ti._request("one_string_inout");
            req.set_return_type(clientOrb.get_primitive_tc(tk_void));
            Any inOutAny = req.add_inout_arg();
            String sp = "TESTINOUT";
            inOutAny.insert_string(sp);
            req.invoke();
            String sprc = inOutAny.extract_string();
            assertEquals("TEST", sprc);
            assertEquals(++num, impl._OB_numReq());

            req = ti._request("one_string_out");
            req.set_return_type(clientOrb.get_primitive_tc(tk_void));
            Any outAny = req.add_out_arg();
            outAny.insert_string("");
            req.invoke();
            sprc = outAny.extract_string();
            assertEquals("TEST", sprc);
            assertEquals(++num, impl._OB_numReq());

            req = ti._request("one_string_return");
            req.set_return_type(clientOrb.get_primitive_tc(tk_string));
            req.invoke();
            sprc = req.return_value().extract_string();
            assertEquals("TEST", sprc);
            assertEquals(++num, impl._OB_numReq());
        }

        //
        // Test in, inout and out struct parameters
        //
        {
            s ss = new s();
            ss.sval = "TEST";
            req = ti._request("one_struct_in");
            req.set_return_type(clientOrb.get_primitive_tc(tk_void));
            sHelper.insert(req.add_in_arg(), ss);
            req.invoke();
            assertEquals(++num, impl._OB_numReq());

            ss.sval = "TESTINOUT";
            req = ti._request("one_struct_inout");
            req.set_return_type(clientOrb.get_primitive_tc(tk_void));
            Any inOutAny = req.add_inout_arg();
            sHelper.insert(inOutAny, ss);
            req.invoke();
            s ssrc = sHelper.extract(inOutAny);
            assertEquals("TEST", ssrc.sval);
            assertEquals(++num, impl._OB_numReq());

            req = ti._request("one_struct_out");
            req.set_return_type(clientOrb.get_primitive_tc(tk_void));
            Any outAny = req.add_out_arg();
            outAny.type(sHelper.type());
            req.invoke();
            ssrc = sHelper.extract(outAny);
            assertEquals("TEST", ssrc.sval);
            assertEquals(++num, impl._OB_numReq());

            req = ti._request("one_struct_return");
            req.set_return_type(sHelper.type());
            req.invoke();
            ssrc = sHelper.extract(req.return_value());
            assertEquals("TEST", ssrc.sval);
            assertEquals(++num, impl._OB_numReq());
        }

        //
        // Test: PortableInterceptor::Current still has the same value
        //
        Any slotData2 = assertDoesNotThrow(() -> pic.get_slot(clientSlotId));
        int v = slotData2.extract_long();
        assertEquals(10, v);

        //
        // Test: ASYNC calls
        //
        {
            slotData.insert_long(10);
            assertDoesNotThrow(() -> pic.set_slot(clientSlotId, slotData));

            req = ti._request("noargs");
            req.send_deferred();
            assertEquals(++num, impl._OB_numReq());

            slotData2 = assertDoesNotThrow(() -> pic.get_slot(clientSlotId));

            v = slotData2.extract_long();
            assertEquals(10, v);

            slotData.insert_long(11);
            assertDoesNotThrow(() -> pic.set_slot(clientSlotId, slotData));

            assertDoesNotThrow(req::get_response);

            slotData2 = assertDoesNotThrow(() -> pic.get_slot(clientSlotId));
            v = slotData2.extract_long();
            assertEquals(11, v);
        }

        {
            slotData.insert_long(10);
            assertDoesNotThrow(() -> pic.set_slot(clientSlotId, slotData));

            req = ti._request("userexception");
            req.exceptions().add(userHelper.type());
            req.send_deferred();
            assertEquals(++num, impl._OB_numReq());

            slotData2 = assertDoesNotThrow(() -> pic.get_slot(clientSlotId));

            v = slotData2.extract_long();
            assertEquals(10, v);

            slotData.insert_long(11);
            assertDoesNotThrow(() -> pic.set_slot(clientSlotId, slotData));

            assertDoesNotThrow(req::get_response);

            slotData2 = assertDoesNotThrow(() -> pic.get_slot(clientSlotId));
            v = slotData2.extract_long();
            assertEquals(11, v);
        }

        {
            slotData.insert_long(10);
            assertDoesNotThrow(() -> pic.set_slot(clientSlotId, slotData));

            req = ti._request("systemexception");
            req.send_deferred();
            assertEquals(++num, impl._OB_numReq());

            slotData2 = assertDoesNotThrow(() -> pic.get_slot(clientSlotId));

            v = slotData2.extract_long();
            assertEquals(10, v);

            slotData.insert_long(11);
            assertDoesNotThrow(() -> pic.set_slot(clientSlotId, slotData));

            assertThrows(NO_IMPLEMENT.class, req::get_response);

            slotData2 = assertDoesNotThrow(() -> pic.get_slot(clientSlotId));
            v = slotData2.extract_long();
            assertEquals(11, v);
        }

        {
            slotData.insert_long(10);
            assertDoesNotThrow(() -> pic.set_slot(clientSlotId, slotData));

            req = ti._request("location_forward");
            req.send_deferred();
            assertEquals(++num, impl._OB_numReq());

            slotData2 = assertDoesNotThrow(() -> pic.get_slot(clientSlotId));

            v = slotData2.extract_long();
            assertEquals(10, v);

            slotData.insert_long(11);
            assertDoesNotThrow(() -> pic.set_slot(clientSlotId, slotData));

            assertThrows(NO_IMPLEMENT.class, req::get_response);

            slotData2 = assertDoesNotThrow(() -> pic.get_slot(clientSlotId));
            v = slotData2.extract_long();
            assertEquals(11, v);
        }

        clientProxyManager.clearInterceptors();
    }
}
