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
import org.omg.CORBA.BAD_INV_ORDER;
import org.omg.CORBA.NO_PERMISSION;
import testify.iiop.TestORBInitializer;
import testify.iiop.annotation.ConfigureOrb.UseWithOrb;
import testify.iiop.annotation.ConfigureServer;
import testify.iiop.annotation.ConfigureServer.RemoteImpl;

import java.lang.reflect.UndeclaredThrowableException;
import java.rmi.RemoteException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.omg.CORBA.CompletionStatus.*;
import static testify.iiop.annotation.ConfigureOrb.UseWithOrb.InitializerScope.CLIENT;
import static testify.iiop.annotation.ConfigureOrb.UseWithOrb.InitializerScope.SERVER;

/**
 * Test client-side interceptor exception handling with Flow Stack verification.
 */
@ConfigureServer
public class ClientExceptionFlowTest {
    private static final ClientRequestInterceptor CI1 = Mockito.mock(ClientRequestInterceptor.class, "CI1");
    private static final ClientRequestInterceptor CI2 = Mockito.mock(ClientRequestInterceptor.class, "CI2");
    private static final ClientRequestInterceptor CI3 = Mockito.mock(ClientRequestInterceptor.class, "CI3");
    private static final ServerRequestInterceptor SI = Mockito.mock(ServerRequestInterceptor.class, "SI");

    static {
        Mockito.when(CI1.name()).thenReturn("CI1");
        Mockito.when(CI2.name()).thenReturn("CI2");
        Mockito.when(CI3.name()).thenReturn("CI3");
        Mockito.when(SI.name()).thenReturn("SI");
    }

    @RemoteImpl
    public static final Echo impl = ClientExceptionFlowTest::convertString;

    private static String convertString(String s) { return '#' + s + '#'; }

    @AfterEach
    void resetMocks() {
        // Reset all mocks after each test to ensure clean state
        Mockito.reset(CI1, CI2, CI3, SI);
    }

    @UseWithOrb(scope = CLIENT)
    public static class ClientOrbInitializer implements TestORBInitializer {
        @Override
        public void pre_init(ORBInitInfo info) {
            assertDoesNotThrow(() -> info.add_client_request_interceptor(CI1));
            assertDoesNotThrow(() -> info.add_client_request_interceptor(CI2));
            assertDoesNotThrow(() -> info.add_client_request_interceptor(CI3));
        }
    }

    @UseWithOrb(scope = SERVER)
    public static class ServerOrbInitializer implements TestORBInitializer {
        @Override
        public void pre_init(ORBInitInfo info) {
            assertDoesNotThrow(() -> info.add_server_request_interceptor(SI));
        }
    }

    /**
     * Exception in send_request()
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
    void testExceptionInSendRequest(Echo stub) throws Exception {
        // Configure CI2 to throw NO_PERMISSION in send_request() using lambda
        Mockito.doAnswer(invocation -> {
            // A well-behaved interceptor would only throw with COMPLETED_NO,
            // but Yoko should also *ensure* the status is COMPLETED_NO.
            throw new NO_PERMISSION("Test exception", 0, COMPLETED_MAYBE);
        }).when(CI2).send_request(Mockito.any(ClientRequestInfo.class));

        // Execute the call and verify exception is thrown
        RemoteException remoteEx = assertThrows(RemoteException.class, () -> stub.echo("should throw"));
        NO_PERMISSION corbaEx = assertInstanceOf(NO_PERMISSION.class, remoteEx.getCause());

        // Verify completion status
        assertEquals(COMPLETED_NO, corbaEx.completed, "Exception should have COMPLETED_NO status");

        // Verify expected flow using Mockito InOrder (chained) to check execution order:
        var order = Mockito.inOrder(CI1, CI2, CI3, SI);
        order.verify(CI1).send_request(Mockito.any(ClientRequestInfo.class));  // 1. CI1.send_request()
        order.verify(CI2).send_request(Mockito.any(ClientRequestInfo.class));  // 2. CI2.send_request() throws
        order.verify(CI1).receive_exception(Mockito.any(ClientRequestInfo.class)); // 3. CI1.receive_exception()
        order.verifyNoMoreInteractions(); // Verify no other interactions occurred on any of the interceptors
    }

    /**
     * Exception Translation in Interceptor Chain
     * Verifies that an interceptor can translate exceptions in receive_exception():
     * - CI1.send_request() executes successfully
     * - CI2.send_request() throws NO_PERMISSION
     * - CI3.send_request() is NOT called (never pushed to Flow Stack)
     * - CI1.receive_exception() receives NO_PERMISSION, throws BAD_INV_ORDER
     * - CI2.receive_exception() is NOT called (interceptor that raised exception)
     * - CI3.receive_exception() is NOT called (never pushed to Flow Stack)
     * - Client receives BAD_INV_ORDER (not NO_PERMISSION)
     */
    @Test
    void testExceptionTranslationInInterceptorChain(Echo stub) throws Exception {
        // Configure CI2 to throw NO_PERMISSION in send_request()
        Mockito.doAnswer(invocation -> {
            throw new NO_PERMISSION("Original exception from CI2", 0, COMPLETED_MAYBE);
        }).when(CI2).send_request(Mockito.any(ClientRequestInfo.class));

        // Configure CI1 to translate NO_PERMISSION to BAD_INV_ORDER in receive_exception()
        Mockito.doAnswer(invocation -> {
            // Translate to BAD_INV_ORDER (don't verify received exception to avoid assertion errors in mock)
            throw new org.omg.CORBA.BAD_INV_ORDER("Translated exception from CI1", 0, COMPLETED_MAYBE);
        }).when(CI1).receive_exception(Mockito.any(ClientRequestInfo.class));

        // Execute the call and verify BAD_INV_ORDER is thrown (not NO_PERMISSION)
        Throwable thrown = assertThrows(Throwable.class, () -> stub.echo("should throw"));
        // Unwrap the exception to get the actual CORBA exception
        Throwable cause = unwrapException(thrown);
        BAD_INV_ORDER corbaEx = assertInstanceOf(BAD_INV_ORDER.class, cause);

        // Test the BAD_INV_ORDER has COMPLETED_NO status
        assertEquals(COMPLETED_NO, corbaEx.completed, "BAD_INV_ORDER should have COMPLETED_NO status");

        // Test the BAD_INV_ORDER has NO_PERMISSION as a suppressed exception
        Throwable[] suppressed = corbaEx.getSuppressed();
        assertEquals(1, suppressed.length, "BAD_INV_ORDER should have exactly one suppressed exception");
        NO_PERMISSION suppressedEx = assertInstanceOf(NO_PERMISSION.class, suppressed[0],
            "Suppressed exception should be NO_PERMISSION");

        // Test the NO_PERMISSION from CI2 has COMPLETED_NO status - corrected from by send_request exception handling
        assertEquals(COMPLETED_NO, suppressedEx.completed, "Original NO_PERMISSION from CI2 should have COMPLETED_NO status");

        // Verify expected flow using Mockito InOrder to check execution order:
        var order = Mockito.inOrder(CI1, CI2, CI3, SI);
        order.verify(CI1).send_request(Mockito.any(ClientRequestInfo.class));  // 1. CI1.send_request()
        order.verify(CI2).send_request(Mockito.any(ClientRequestInfo.class));  // 2. CI2.send_request() throws NO_PERMISSION
        order.verify(CI1).receive_exception(Mockito.any(ClientRequestInfo.class)); // 3. CI1.receive_exception() translates to BAD_INV_ORDER
        order.verifyNoMoreInteractions(); // Verify CI2.receive_exception() and CI3 are NOT called
    }

    /**
     * Unwraps exception wrappers to get the underlying CORBA exception.
     * Handles RemoteException and UndeclaredThrowableException wrappers.
     */
    private static Throwable unwrapException(Throwable thrown) {
        if (thrown instanceof RemoteException) {
            return thrown.getCause();
        }
        if (thrown instanceof UndeclaredThrowableException) {
            return ((UndeclaredThrowableException) thrown).getUndeclaredThrowable();
        }
        return thrown;
    }

    /**
     * Exception in receive_reply()
     * Verifies that when CI2 throws an exception in receive_reply():
     * - Request completes successfully on server
     * - receive_reply() called in reverse order: CI3, CI2, CI1
     * - CI3.receive_reply() executes successfully
     * - CI2.receive_reply() throws NO_PERMISSION with COMPLETED_YES
     * - CI1.receive_reply() is NOT called
     * - receive_exception() called on remaining Flow Stack: CI1
     * - CI1.receive_exception() called with NO_PERMISSION
     * - CI2.receive_exception() is NOT called (interceptor that raised exception)
     * - CI3.receive_exception() is NOT called (already completed successfully)
     * - Exception reaches client
     */
    @Test
    void testExceptionInReceiveReply(Echo stub) throws Exception {
        // Configure CI2 to throw NO_PERMISSION in receive_reply()
        Mockito.doAnswer(invocation -> {
            // A well-behaved interceptor would throw with COMPLETED_YES in receive_reply(),
            // but Yoko should also *ensure* the status is COMPLETED_YES.
            throw new NO_PERMISSION("Test exception in receive_reply", 0, COMPLETED_MAYBE);
        }).when(CI2).receive_reply(Mockito.any(ClientRequestInfo.class));

        // Execute the call and verify exception is thrown
        RemoteException remoteEx = assertThrows(RemoteException.class, () -> stub.echo("test"));
        NO_PERMISSION corbaEx = assertInstanceOf(NO_PERMISSION.class, remoteEx.getCause());

        // Verify completion status - should be COMPLETED_YES since server completed successfully
        assertEquals(COMPLETED_YES, corbaEx.completed, "Exception should have COMPLETED_YES status");

        // Verify expected flow using Mockito InOrder to check execution order:
        var order = Mockito.inOrder(CI1, CI2, CI3, SI);
        // 1. Client send_request() called in forward order
        order.verify(CI1).send_request(Mockito.any(ClientRequestInfo.class));
        order.verify(CI2).send_request(Mockito.any(ClientRequestInfo.class));
        order.verify(CI3).send_request(Mockito.any(ClientRequestInfo.class));
        // 2. Server interceptor processes request successfully
        order.verify(SI).receive_request_service_contexts(Mockito.any(ServerRequestInfo.class));
        order.verify(SI).receive_request(Mockito.any(ServerRequestInfo.class));
        // 3. Server sends reply successfully
        order.verify(SI).send_reply(Mockito.any(ServerRequestInfo.class));
        // 4. Client receive_reply() called in reverse order until CI2 throws
        order.verify(CI3).receive_reply(Mockito.any(ClientRequestInfo.class));
        order.verify(CI2).receive_reply(Mockito.any(ClientRequestInfo.class)); // throws here
        // 5. CI1.receive_reply() is NOT called
        // 6. receive_exception() called on remaining Flow Stack (only CI1)
        order.verify(CI1).receive_exception(Mockito.any(ClientRequestInfo.class));
        order.verifyNoMoreInteractions(); // Verify CI1.receive_reply(), CI2.receive_exception(), CI3.receive_exception(), and SI.send_exception() are NOT called
    }

}
