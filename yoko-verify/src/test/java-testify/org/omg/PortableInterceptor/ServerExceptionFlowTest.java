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
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.omg.CORBA.Any;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.UNKNOWNHelper;
import org.omg.CORBA.portable.UnknownException;
import org.omg.CORBA.UNKNOWN;
import testify.iiop.TestORBInitializer;
import testify.iiop.annotation.ConfigureOrb.UseWithOrb;
import testify.iiop.annotation.ConfigureServer;
import testify.iiop.annotation.ConfigureServer.RemoteImpl;

import java.rmi.Remote;
import java.rmi.RemoteException;

import static org.junit.jupiter.api.Assertions.*;
import static org.omg.CORBA.CompletionStatus.COMPLETED_NO;
import static org.omg.CORBA.CompletionStatus.COMPLETED_YES;
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
    private static boolean shouldThrowUserException = false;

    /**
     * Custom exception for testing user exception handling.
     * Extends Exception (not CORBA.UserException) to allow proper RMI-IIOP serialization.
     */
    public static class TestUserException extends Exception {
        public TestUserException(String message) {
            super(message);
        }
    }

    /**
     * RMI interface that declares a user exception.
     * This allows proper testing of user exception handling through RMI-IIOP.
     */
    public interface ExceptionThrower extends Remote {
        String echo(String s) throws RemoteException, TestUserException;
    }

    static {
        Mockito.when(SI1.name()).thenReturn("SI1");
        Mockito.when(SI2.name()).thenReturn("SI2");
        Mockito.when(SI3.name()).thenReturn("SI3");
    }

    @RemoteImpl
    public static final Echo impl = ServerExceptionFlowTest::convertString;

    @RemoteImpl
    public static final ExceptionThrower exceptionThrowerImpl = ServerExceptionFlowTest::convertStringWithException;

    private static String convertString(String s) {
        targetInvoked = true;
        return '#' + s + '#';
    }

    private static String convertStringWithException(String s) throws TestUserException {
        targetInvoked = true;
        if (shouldThrowUserException) {
            throw new TestUserException("Test user exception from target");
        }
        return '#' + s + '#';
    }

    @AfterEach
    void resetMocks() {
        // Reset all mocks after each test to ensure clean state
        Mockito.reset(SI1, SI2, SI3);
        targetInvoked = false;
        shouldThrowUserException = false;
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
     * Exception in receive_request_service_contexts()
     * <p>
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

    /**
     * Exception in receive_request()
     * <p>
     * This test verifies that when SI2 throws NO_PERMISSION in receive_request():
     * 1. All receive_request_service_contexts() complete successfully
     * 2. SI1.receive_request() executes successfully
     * 3. SI2.receive_request() throws NO_PERMISSION
     * 4. SI3.receive_request() is NOT called (exception stops forward flow)
     * 5. Target object is NOT invoked
     * 6. send_exception() is called in reverse order: SI3 → SI2 → SI1
     * 7. Exception sent back to client with COMPLETED_NO
     */
    @Test
    void testExceptionInReceiveRequest(Echo stub) throws Exception {
        // Configure SI2 to throw NO_PERMISSION in receive_request()
        Mockito.doAnswer(invocation -> {
            throw new NO_PERMISSION("Test exception from SI2 in receive_request", 0, COMPLETED_NO);
        }).when(SI2).receive_request(Mockito.any(ServerRequestInfo.class));

        // Execute the call and verify exception is thrown
        RemoteException remoteEx = assertThrows(RemoteException.class, () -> stub.echo("test"));
        NO_PERMISSION corbaEx = assertInstanceOf(NO_PERMISSION.class, remoteEx.getCause());

        // Verify completion status
        assertEquals(COMPLETED_NO, corbaEx.completed, "Exception should have COMPLETED_NO status");

        // Verify target was NOT invoked
        assertFalse(targetInvoked, "Target object should NOT have been invoked");

        // Verify expected flow using Mockito InOrder to check execution order:
        var order = Mockito.inOrder(SI1, SI2, SI3);

        // All receive_request_service_contexts() complete
        order.verify(SI1).receive_request_service_contexts(Mockito.any(ServerRequestInfo.class));
        order.verify(SI2).receive_request_service_contexts(Mockito.any(ServerRequestInfo.class));
        order.verify(SI3).receive_request_service_contexts(Mockito.any(ServerRequestInfo.class));

        // receive_request() - SI1 completes, SI2 throws, SI3 not called
        order.verify(SI1).receive_request(Mockito.any(ServerRequestInfo.class));
        order.verify(SI2).receive_request(Mockito.any(ServerRequestInfo.class));

        // send_exception() called in reverse order. CORBA 3.0.3 spec section 21.3.9.2 receive_request:
        // This interception point may raise a system exception. If it does, no other Interceptors
        // receive_request operations are called. Those Interceptors on the Flow Stack are
        // popped and their send_exception interception points are called.
        order.verify(SI3).send_exception(Mockito.any(ServerRequestInfo.class));
        order.verify(SI2).send_exception(Mockito.any(ServerRequestInfo.class));
        order.verify(SI1).send_exception(Mockito.any(ServerRequestInfo.class));

        // Verify no other interactions
        order.verifyNoMoreInteractions();

        // Additional verification: SI3.receive_request() should never be called
        Mockito.verify(SI3, Mockito.never()).receive_request(Mockito.any(ServerRequestInfo.class));

        // Additional verification: No send_reply or send_other should be called
        Mockito.verify(SI1, Mockito.never()).send_reply(Mockito.any(ServerRequestInfo.class));
        Mockito.verify(SI1, Mockito.never()).send_other(Mockito.any(ServerRequestInfo.class));
        Mockito.verify(SI2, Mockito.never()).send_reply(Mockito.any(ServerRequestInfo.class));
        Mockito.verify(SI2, Mockito.never()).send_other(Mockito.any(ServerRequestInfo.class));
        Mockito.verify(SI3, Mockito.never()).send_reply(Mockito.any(ServerRequestInfo.class));
        Mockito.verify(SI3, Mockito.never()).send_other(Mockito.any(ServerRequestInfo.class));
    }

    /**
     * Target Object Throws User Exception
     * <p>
     * This test verifies that when the target object throws a user-defined exception:
     * 1. All receive_request_service_contexts() complete successfully
     * 2. All receive_request() complete successfully
     * 3. Target object is invoked and throws user exception
     * 4. send_exception() is called in reverse order: SI3 → SI2 → SI1
     * 5. Exception sent back to client with COMPLETED_YES
     * 6. Exception type and data are preserved
     */
    @Test
    void testTargetObjectThrowsUserException(ExceptionThrower stub) throws Exception {
        // Configure the target to throw a user exception
        shouldThrowUserException = true;
        
        // Execute the call and verify exception is thrown
        TestUserException userEx = assertThrows(TestUserException.class,
            () -> stub.echo("testTargetObjectThrowsUserException"));
        
        // Verify target was invoked
        assertTrue(targetInvoked, "Target object should have been invoked");
        
        // Verify the exception message
        assertEquals("Test user exception from target", userEx.getMessage());
        
        // Verify expected flow using Mockito InOrder
        var order = Mockito.inOrder(SI1, SI2, SI3);
        
        // All receive_request_service_contexts() complete
        order.verify(SI1).receive_request_service_contexts(Mockito.any(ServerRequestInfo.class));
        order.verify(SI2).receive_request_service_contexts(Mockito.any(ServerRequestInfo.class));
        order.verify(SI3).receive_request_service_contexts(Mockito.any(ServerRequestInfo.class));
        
            // All receive_request() complete
        order.verify(SI1).receive_request(Mockito.any(ServerRequestInfo.class));
        order.verify(SI2).receive_request(Mockito.any(ServerRequestInfo.class));
        order.verify(SI3).receive_request(Mockito.any(ServerRequestInfo.class));
        
        // send_exception() called in reverse order after target throws
        order.verify(SI3).send_exception(Mockito.any(ServerRequestInfo.class));
        order.verify(SI2).send_exception(Mockito.any(ServerRequestInfo.class));
        
        // Capture the ServerRequestInfo at SI1 (last interception point before wire)
        ArgumentCaptor<ServerRequestInfo> requestInfoCaptor = ArgumentCaptor.forClass(ServerRequestInfo.class);
        order.verify(SI1).send_exception(requestInfoCaptor.capture());

        ServerRequestInfo finalRequestInfo = requestInfoCaptor.getValue();
        Any exceptionAny = finalRequestInfo.sending_exception();
        assertNotNull(exceptionAny, "Exception should be available in send_exception");
        
        // CORBA 3.0.3 spec section 21.3.9.4 send_exception: When the original exception is a user exception,
        // the completion_status shall be COMPLETED_YES.
        // RMI-IIOP wraps user exceptions (checked exceptions) in org.omg.CORBA.UNKNOWN system exception
        UNKNOWN sysEx = UNKNOWNHelper.extract(exceptionAny);
        assertNotNull(sysEx, "Should be able to extract UNKNOWN system exception from Any");
        assertEquals(COMPLETED_YES, sysEx.completed,
            "User exception from target should result in COMPLETED_YES status");

        // Verify no other interactions
        order.verifyNoMoreInteractions();

        // Additional verification: No send_reply or send_other should be called
        Mockito.verify(SI1, Mockito.never()).send_reply(Mockito.any(ServerRequestInfo.class));
        Mockito.verify(SI1, Mockito.never()).send_other(Mockito.any(ServerRequestInfo.class));
        Mockito.verify(SI2, Mockito.never()).send_reply(Mockito.any(ServerRequestInfo.class));
        Mockito.verify(SI2, Mockito.never()).send_other(Mockito.any(ServerRequestInfo.class));
        Mockito.verify(SI3, Mockito.never()).send_reply(Mockito.any(ServerRequestInfo.class));
        Mockito.verify(SI3, Mockito.never()).send_other(Mockito.any(ServerRequestInfo.class));
    }
}
