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

import acme.Echo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.omg.CORBA.NO_PERMISSION;
import testify.iiop.TestORBInitializer;
import testify.iiop.annotation.ConfigureOrb.UseWithOrb;
import testify.iiop.annotation.ConfigureServer;
import testify.iiop.annotation.ConfigureServer.RemoteImpl;

import java.rmi.RemoteException;

import static org.junit.jupiter.api.Assertions.*;
import static org.omg.CORBA.CompletionStatus.COMPLETED_NO;
import static testify.iiop.annotation.ConfigureOrb.UseWithOrb.InitializerScope.SERVER;

/**
 * Test server-side interceptor exception handling with Flow Stack verification.
 */
@ConfigureServer
public class ServerExceptionFlowTest {
    private static final ServerRequestInterceptor SI1 = Mockito.mock(ServerRequestInterceptor.class, "SI1");
    private static final ServerRequestInterceptor SI2 = Mockito.mock(ServerRequestInterceptor.class, "SI2");
    private static final ServerRequestInterceptor SI3 = Mockito.mock(ServerRequestInterceptor.class, "SI3");
    
    private static boolean targetInvoked = false;

    static {
        Mockito.when(SI1.name()).thenReturn("SI1");
        Mockito.when(SI2.name()).thenReturn("SI2");
        Mockito.when(SI3.name()).thenReturn("SI3");
    }

    @RemoteImpl
    public static final Echo impl = ServerExceptionFlowTest::convertString;

    private static String convertString(String s) {
        targetInvoked = true;
        return '#' + s + '#';
    }

    @AfterEach
    void resetMocks() {
        // Reset all mocks after each test to ensure clean state
        Mockito.reset(SI1, SI2, SI3);
        targetInvoked = false;
    }

    @UseWithOrb(scope = SERVER)
    public static class ServerOrbInitializer implements TestORBInitializer {
        @Override
        public void pre_init(ORBInitInfo info) {
            assertDoesNotThrow(() -> info.add_server_request_interceptor(SI1));
            assertDoesNotThrow(() -> info.add_server_request_interceptor(SI2));
            assertDoesNotThrow(() -> info.add_server_request_interceptor(SI3));
        }
    }

    /**
     * TC-SERVER-001: Exception in receive_request_service_contexts()
     * 
     * This test verifies that when SI2 throws NO_PERMISSION in receive_request_service_contexts():
     * 1. SI1.receive_request_service_contexts() executes successfully
     * 2. SI2.receive_request_service_contexts() throws NO_PERMISSION
     * 3. SI3.receive_request_service_contexts() is NOT called (never pushed to Flow Stack)
     * 4. SI1.send_exception() is called (only interceptors on Flow Stack)
     * 5. SI2.send_exception() is NOT called (interceptor that raised exception)
     * 6. SI3.send_exception() is NOT called (never pushed to Flow Stack)
     * 7. Exception sent back to client with COMPLETED_NO
     * 8. Target object is never invoked
     */
    @Test
    void testExceptionInReceiveRequestServiceContexts(Echo stub) throws Exception {
        // Configure SI2 to throw NO_PERMISSION in receive_request_service_contexts()
        Mockito.doAnswer(invocation -> {
            throw new NO_PERMISSION("Test exception from SI2", 0, COMPLETED_NO);
        }).when(SI2).receive_request_service_contexts(Mockito.any(ServerRequestInfo.class));

        // Execute the call and verify exception is thrown
        RemoteException remoteEx = assertThrows(RemoteException.class, () -> stub.echo("test"));
        NO_PERMISSION corbaEx = assertInstanceOf(NO_PERMISSION.class, remoteEx.getCause());

        // Verify completion status
        assertEquals(COMPLETED_NO, corbaEx.completed, "Exception should have COMPLETED_NO status");

        // Verify target was NOT invoked
        assertFalse(targetInvoked, "Target object should NOT have been invoked");

        // Verify expected flow using Mockito InOrder to check execution order:
        var order = Mockito.inOrder(SI1, SI2, SI3);
        // 1. SI1.receive_request_service_contexts() executes successfully
        order.verify(SI1).receive_request_service_contexts(Mockito.any(ServerRequestInfo.class));
        // 2. SI2.receive_request_service_contexts() throws NO_PERMISSION
        order.verify(SI2).receive_request_service_contexts(Mockito.any(ServerRequestInfo.class));
        // 3. SI1.send_exception() is called (only interceptor on Flow Stack)
        order.verify(SI1).send_exception(Mockito.any(ServerRequestInfo.class));
        // 4. Verify no other interactions (SI3 never called, SI2.send_exception() not called)
        order.verifyNoMoreInteractions();
        
        // Additional verification: SI3 should never be called at all
        Mockito.verify(SI3, Mockito.never()).receive_request_service_contexts(Mockito.any(ServerRequestInfo.class));
        Mockito.verify(SI3, Mockito.never()).receive_request(Mockito.any(ServerRequestInfo.class));
        Mockito.verify(SI3, Mockito.never()).send_reply(Mockito.any(ServerRequestInfo.class));
        Mockito.verify(SI3, Mockito.never()).send_exception(Mockito.any(ServerRequestInfo.class));
        Mockito.verify(SI3, Mockito.never()).send_other(Mockito.any(ServerRequestInfo.class));
        
        // Additional verification: SI2.send_exception() should not be called
        Mockito.verify(SI2, Mockito.never()).send_exception(Mockito.any(ServerRequestInfo.class));
        
        // Additional verification: No other server interception points should be called
        Mockito.verify(SI1, Mockito.never()).receive_request(Mockito.any(ServerRequestInfo.class));
        Mockito.verify(SI1, Mockito.never()).send_reply(Mockito.any(ServerRequestInfo.class));
        Mockito.verify(SI1, Mockito.never()).send_other(Mockito.any(ServerRequestInfo.class));
        Mockito.verify(SI2, Mockito.never()).receive_request(Mockito.any(ServerRequestInfo.class));
        Mockito.verify(SI2, Mockito.never()).send_reply(Mockito.any(ServerRequestInfo.class));
        Mockito.verify(SI2, Mockito.never()).send_other(Mockito.any(ServerRequestInfo.class));
    }
}
