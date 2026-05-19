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
 * distributed under the License is distributed on an "AS IS" BASIS,
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
import org.omg.CORBA.CompletionStatus;
import org.omg.CORBA.LocalObject;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Policy;
import org.omg.IOP.CodecFactory;
import org.omg.IOP.CodecFactoryHelper;
import org.omg.PortableInterceptor.ORBInitInfoPackage.DuplicateName;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAManager;
import org.omg.PortableServer.ServantLocator;
import testify.bus.Bus;
import testify.bus.key.StringKey;
import testify.iiop.TestORBInitializer;
import testify.iiop.annotation.ConfigureOrb;
import testify.iiop.annotation.ConfigureOrb.UseWithOrb;
import testify.iiop.annotation.ConfigureServer;
import testify.iiop.annotation.ConfigureServer.BeforeServer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static org.apache.yoko.orb.PortableServer.PolicyValue.NON_RETAIN;
import static org.apache.yoko.orb.PortableServer.PolicyValue.NO_IMPLICIT_ACTIVATION;
import static org.apache.yoko.orb.PortableServer.PolicyValue.PERSISTENT;
import static org.apache.yoko.orb.PortableServer.PolicyValue.USER_ID;
import static org.apache.yoko.orb.PortableServer.PolicyValue.USE_SERVANT_MANAGER;
import static org.apache.yoko.orb.PortableServer.PolicyValue.create_POA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omg.CORBA.CompletionStatus.COMPLETED_NO;
import static org.omg.CORBA.SetOverrideType.ADD_OVERRIDE;
import static testify.iiop.annotation.ConfigureOrb.UseWithOrb.InitializerScope.CLIENT;
import static testify.iiop.annotation.ConfigureOrb.UseWithOrb.InitializerScope.SERVER;

/**
 * Test client-side interceptor exception handling with Flow Stack verification.
 * Implements TC-CLIENT-001 from the interceptor exception test plan.
 */
@ConfigureServer(
        clientOrb = @ConfigureOrb(props = "yoko.orb.id=client orb"),
        serverOrb = @ConfigureOrb(props = "yoko.orb.id=server orb")
)
public class ClientExceptionFlowTest {
    private static ClientProxyManager clientProxyManager;
    private static Bus clientBus;
    private static ORB clientOrb;
    private static Policy[] policies;
    static int serverSlotId;

    enum IorKey implements StringKey {IMPL} //could possibly extract IorKey from PortableInterceptorTest and use a common enum

    /**
     * Tracks interceptor execution flow for verification.
     */
    static class ExecutionTracker {
        private final List<String> events = Collections.synchronizedList(new ArrayList<>());

        void record(String event) {
            events.add(event);
        }

        List<String> getEvents() {
            return new ArrayList<>(events);
        }

        void clear() {
            events.clear();
        }
    }

    /**
     * Test interceptor that records execution and can throw exceptions.
     */
    static class TrackingInterceptor extends LocalObject implements ClientRequestInterceptor {
        private final String name;
        private final ExecutionTracker tracker;
        private boolean throwInSendRequest = false;
        private NO_PERMISSION exceptionToThrow = null;

        TrackingInterceptor(String name, ExecutionTracker tracker) {
            this.name = name;
            this.tracker = tracker;
        }

        void setThrowInSendRequest(NO_PERMISSION exception) {
            this.throwInSendRequest = true;
            this.exceptionToThrow = exception;
        }

        void clearThrow() {
            this.throwInSendRequest = false;
            this.exceptionToThrow = null;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public void destroy() {
            tracker.record(name + ".destroy()");
        }

        @Override
        public void send_request(ClientRequestInfo ri) throws ForwardRequest {
            tracker.record(name + ".send_request()");
            if (throwInSendRequest && exceptionToThrow != null) {
                tracker.record(name + ".send_request() throwing " + exceptionToThrow.getClass().getSimpleName());
                throw exceptionToThrow;
            }
        }

        @Override
        public void send_poll(ClientRequestInfo ri) {
            tracker.record(name + ".send_poll()");
        }

        @Override
        public void receive_reply(ClientRequestInfo ri) {
            tracker.record(name + ".receive_reply()");
        }

        @Override
        public void receive_other(ClientRequestInfo ri) throws ForwardRequest {
            tracker.record(name + ".receive_other()");
        }

        @Override
        public void receive_exception(ClientRequestInfo ri) throws ForwardRequest {
            tracker.record(name + ".receive_exception()");
        }
    }

    @UseWithOrb(scope = CLIENT)
    public static class ClientOrbInitializer implements TestORBInitializer {
        @Override
        public void pre_init(ORBInitInfo info) {
            clientProxyManager = new ClientProxyManager(info);
        }
    }

    @UseWithOrb(scope = SERVER)
    public static class ServerOrbInitializer implements TestORBInitializer {
        @Override
        public void pre_init(ORBInitInfo info) {
            try {
                serverSlotId = info.allocate_slot_id();
                CodecFactory codecFactory = info.codec_factory();
                info.add_server_request_interceptor(
                    new ServerTestInterceptor_impl(serverSlotId, codecFactory)
                );
            } catch (DuplicateName e) {
                throw new RuntimeException(e);
            }
        }
    }

    @BeforeServer
    public static void beforeServer(Bus serverBus, ORB serverOrb, POA rootPoa) throws Exception {
        assertNotNull(CodecFactoryHelper.narrow(serverOrb.resolve_initial_references("CodecFactory")));
        POAManager poaMgr = rootPoa.the_POAManager();

        POA persistentPOA = create_POA("persistent", rootPoa, poaMgr,
                PERSISTENT, USER_ID, USE_SERVANT_MANAGER, NON_RETAIN, NO_IMPLICIT_ACTIVATION);

        // Create servant objects
        TestInterface_impl impl = new TestInterface_impl(serverOrb, persistentPOA, serverSlotId);
        TestInterfaceDSI_impl dsiImpl = new TestInterfaceDSI_impl(serverOrb, serverSlotId);

        // Install the servant locator in the POA
        TestLocator_impl locatorImpl = new TestLocator_impl(serverOrb, impl, dsiImpl);
        ServantLocator locator = locatorImpl._this(serverOrb);
        persistentPOA.set_servant_manager(locator);

        // Create and publish the persistent object reference
        org.omg.CORBA.Object objImpl = persistentPOA.create_reference_with_id(
            "test".getBytes(),
            "IDL:TestInterface:1.0"
        );
        serverBus.put(IorKey.IMPL, serverOrb.object_to_string(objImpl));
    }

    @BeforeAll
    public static void beforeClient(Bus clientBus, ORB clientOrb) {
        ClientExceptionFlowTest.clientBus = clientBus;
        ClientExceptionFlowTest.clientOrb = clientOrb;
        policies = new Policy[0];
    }

    private TestInterface getTestInterface() {
        String impl = clientBus.get(IorKey.IMPL);
        org.omg.CORBA.Object obj = clientOrb.string_to_object(impl)._set_policy_override(policies, ADD_OVERRIDE);
        return requireNonNull(TestInterfaceHelper.narrow(obj));
    }

    @AfterEach
    public void clearClientInterceptors() {
        clientProxyManager.clearInterceptors();
    }

    /**
     * TC-CLIENT-001: Exception in send_request()
     * Verifies that when CI2 throws an exception in send_request():
     * - CI1.send_request() executes successfully
     * - CI2.send_request() throws NO_PERMISSION with COMPLETED_NO
     * - CI3.send_request() is NOT called (never pushed to Flow Stack)
     * - CI1.receive_exception() is called (only interceptors on Flow Stack)
     * - CI2.receive_exception() is NOT called (interceptor that raised exception)
     * - CI3.receive_exception() is NOT called (never pushed to Flow Stack)
     * - Exception reaches client application
     * - No network request is sent
     */
    @Test
    void testExceptionInSendRequest() {
        ExecutionTracker tracker = new ExecutionTracker();

        // Create three interceptors
        TrackingInterceptor ci1 = new TrackingInterceptor("CI1", tracker);
        TrackingInterceptor ci2 = new TrackingInterceptor("CI2", tracker);
        TrackingInterceptor ci3 = new TrackingInterceptor("CI3", tracker);

        // Configure CI2 to throw NO_PERMISSION in send_request()
        NO_PERMISSION expectedException = new NO_PERMISSION("Test exception", 0, COMPLETED_NO);
        ci2.setThrowInSendRequest(expectedException);

        // Register interceptors
        clientProxyManager.setInterceptor(0, ci1);
        clientProxyManager.setInterceptor(1, ci2);
        clientProxyManager.setInterceptor(2, ci3);

        TestInterface ti = getTestInterface();

        // Execute the call and verify exception is thrown
        NO_PERMISSION thrownException = assertThrows(NO_PERMISSION.class, ti::noargs);

        // Verify completion status
        assertEquals(COMPLETED_NO, thrownException.completed,
            "Exception should have COMPLETED_NO status");

        // Verify execution flow
        List<String> events = tracker.getEvents();

        // Expected flow:
        // 1. CI1.send_request() - executes successfully
        // 2. CI2.send_request() - throws exception
        // 3. CI3.send_request() - NOT called (never pushed to Flow Stack)
        // 4. CI1.receive_exception() - called (on Flow Stack)
        // 5. CI2.receive_exception() - NOT called (threw the exception)
        // 6. CI3.receive_exception() - NOT called (never on Flow Stack)

        assertTrue(events.contains("CI1.send_request()"), "CI1.send_request() should be called");
        assertTrue(events.contains("CI2.send_request()"), "CI2.send_request() should be called");
        assertTrue(events.contains("CI2.send_request() throwing NO_PERMISSION"), "CI2 should throw NO_PERMISSION");

        // Verify CI3.send_request() was NOT called
        assertTrue(events.stream().noneMatch(e -> e.contains("CI3.send_request")),
            "CI3.send_request() should NOT be called (never pushed to Flow Stack)");

        // Verify receive_exception() flow
        assertTrue(events.contains("CI1.receive_exception()"), "CI1.receive_exception() should be called (on Flow Stack)");

        // Verify CI2 and CI3 receive_exception() were NOT called
        assertTrue(events.stream().noneMatch(e -> e.contains("CI2.receive_exception")),
            "CI2.receive_exception() should NOT be called (interceptor that raised exception)");
        assertTrue(events.stream().noneMatch(e -> e.contains("CI3.receive_exception")),
            "CI3.receive_exception() should NOT be called (never pushed to Flow Stack)");

        // Verify no receive_reply() calls (exception path, not normal reply)
        assertTrue(events.stream().noneMatch(e -> e.contains("receive_reply")),
            "No receive_reply() should be called on exception path");

        // Verify order: send_request calls happen before receive_exception - Verify the exact sequence
        assertEquals("CI1.send_request()", events.get(0), "First event should be CI1.send_request()");
        assertEquals("CI2.send_request()", events.get(1), "Second event should be CI2.send_request()");
        assertEquals("CI2.send_request() throwing NO_PERMISSION", events.get(2), "Third event should be CI2 throwing exception");
        assertEquals("CI1.receive_exception()", events.get(3), "Fourth event should be CI1.receive_exception()");

        // Verify total number of events (should be exactly 4)
        assertEquals(4, events.size(), "Should have exactly 4 events in the execution flow");
    }
}
