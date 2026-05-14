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
package org.apache.yoko;

import acme.Echo;
import org.apache.yoko.ThreadLocalPollutionTest.ExceptionThrowingInterceptor.InterceptorException;
import org.apache.yoko.ThreadLocalPollutionTest.ExceptionThrowingInterceptor.WhenToThrow;
import org.apache.yoko.util.cmsf.CmsfThreadLocal;
import org.apache.yoko.util.rofl.RoflThreadLocal;
import org.apache.yoko.util.yasf.YasfThreadLocal;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.omg.CORBA.ORB;
import org.omg.PortableInterceptor.ClientRequestInfo;
import testify.iiop.TestClientRequestInterceptor;
import testify.iiop.annotation.ConfigureOrb;
import testify.iiop.annotation.ConfigureOrb.UseWithOrb;
import testify.iiop.annotation.ConfigureServer;
import testify.iiop.annotation.ConfigureServer.BeforeServer;
import testify.iiop.annotation.ConfigureServer.RemoteImpl;

import java.rmi.RemoteException;
import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Collections.unmodifiableMap;
import static org.apache.yoko.ThreadLocalPollutionTest.ExceptionThrowingInterceptor.WhenToThrow.NEVER;
import static org.apache.yoko.ThreadLocalPollutionTest.ExceptionThrowingInterceptor.WhenToThrow.RECEIVE_EXCEPTION;
import static org.apache.yoko.ThreadLocalPollutionTest.ExceptionThrowingInterceptor.WhenToThrow.RECEIVE_OTHER;
import static org.apache.yoko.ThreadLocalPollutionTest.ExceptionThrowingInterceptor.WhenToThrow.RECEIVE_REPLY;
import static org.apache.yoko.ThreadLocalPollutionTest.ExceptionThrowingInterceptor.WhenToThrow.SEND_POLL;
import static org.apache.yoko.ThreadLocalPollutionTest.ExceptionThrowingInterceptor.WhenToThrow.SEND_REQUEST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static testify.iiop.annotation.ConfigureOrb.UseWithOrb.InitializerScope.CLIENT;
import static testify.util.Private.invoke;

/**
 * Tests that demonstrate ThreadLocal pollution when a client interceptor
 * throws an exception from receive_reply(), preventing cleanup of CMSF, YASF, and ROFL ThreadLocals.
 *
 * @see <a href="https://github.com/OpenLiberty/yoko/issues/783">Issue #783</a>
 */
@ConfigureServer
public abstract class ThreadLocalPollutionTest {
    static String echo(String s) { return "Echo: " + s; }
    static String sleepyEcho(String s) {
        try {
            Thread.sleep(10000);
        } catch (InterruptedException ignored) {
        }
        return s;
    }

    /**
     * Minimal interceptor that throws an exception from receive_reply() to interrupt
     * the interceptor chain and prevent ThreadLocal cleanup.
     */
    @UseWithOrb(scope = CLIENT)
    public static class ExceptionThrowingInterceptor implements TestClientRequestInterceptor {
        enum WhenToThrow { NEVER, SEND_POLL, SEND_REQUEST, RECEIVE_REPLY, RECEIVE_EXCEPTION, RECEIVE_OTHER }

        static class InterceptorException extends RuntimeException {}

        private static volatile WhenToThrow throwOn = NEVER;

        @Override public String name() { return "ExceptionThrowingInterceptor"; }
        @Override public void send_poll(ClientRequestInfo ri) {
            System.err.println("ExceptionThrowingInterceptor.send_poll: throwOn=" + throwOn);
            if (throwOn == SEND_POLL) throw new InterceptorException();
        }
        @Override public void send_request(ClientRequestInfo ri) {
            System.err.println("ExceptionThrowingInterceptor.send_request: throwOn=" + throwOn);
            if (throwOn == SEND_REQUEST) throw new InterceptorException();
        }
        @Override public void receive_reply(ClientRequestInfo ri) {
            System.err.println("ExceptionThrowingInterceptor.receive_reply: throwOn=" + throwOn);
            if (throwOn == RECEIVE_REPLY) throw new InterceptorException();
        }
        @Override  public void receive_exception(ClientRequestInfo ri) {
            System.err.println("ExceptionThrowingInterceptor.receive_exception: throwOn=" + throwOn);
            if (throwOn == RECEIVE_EXCEPTION) throw new InterceptorException();
        }
        @Override public void receive_other(ClientRequestInfo ri) {
            System.err.println("ExceptionThrowingInterceptor.receive_other: throwOn=" + throwOn);
            if (throwOn == RECEIVE_OTHER) throw new InterceptorException();
        }
    }

    /** These are the interception points which can be configured to throw exceptions that would result in Exceptions */
    private final Map<WhenToThrow, Class<? extends Throwable>> expectedExceptions;

    ThreadLocalPollutionTest(ExceptionExpecter builder) { this.expectedExceptions = builder.getExpectations(); }

    @BeforeEach
    void ensureCleanState() { cleanUp(); }

    @AfterEach
    void checkForThreadPollution() {
        int cmsfDepth = invoke(CmsfThreadLocal.class, "getStackDepth");
        assertEquals(0, cmsfDepth, "CMSF stack should be empty");

        int roflDepth = invoke(RoflThreadLocal.class, "getStackDepth");
        assertEquals(0, roflDepth, "ROFL stack should be empty");

        int yasfDepth = invoke(YasfThreadLocal.class, "getStackDepth");
        assertEquals(0, yasfDepth, "Stack should be empty");
    }

    @AfterAll
    static void cleanUp() {
        CmsfThreadLocal.reset();
        YasfThreadLocal.reset();
        RoflThreadLocal.reset();
    }

    @ParameterizedTest
    @EnumSource(WhenToThrow.class)
    void testThreadLocalPollution(WhenToThrow whenToThrow, Echo service, TestInfo testInfo) throws Exception {
        //Assumptions.assumeTrue(whenToThrow == SEND_REQUEST);
        invokeService(whenToThrow, service, testInfo.getDisplayName());
    }

    @ParameterizedTest
    @EnumSource(WhenToThrow.class)
    void testStackDepthDoesNotGrowWithMultipleCalls(WhenToThrow whenToThrow, Echo service, TestInfo testInfo) throws Exception {
        for (int i = 0; i < 5; i++) invokeService(whenToThrow, service, testInfo.getDisplayName() + i);
    }

    void invokeService(WhenToThrow whenToThrow, Echo service, String payload) throws RemoteException {
        ExceptionThrowingInterceptor.throwOn = whenToThrow;
        if (expectedExceptions.containsKey(whenToThrow)) {
            assertThrows(expectedExceptions.get(whenToThrow), () -> service.echo(payload));
        } else {
            assertEquals(echo(payload), service.echo(payload));
        }
    }

    interface ExceptionExpecter {
        ExceptionSpecifier ifConfiguredToThrowOn(WhenToThrow... interceptionPoints);
        Map<WhenToThrow, Class<? extends Throwable>> getExpectations();
    }

    interface ExceptionSpecifier {
        ExceptionExpecter expect(Class<? extends Throwable> exceptionClass);
    }

    static ExceptionExpecter buildExpectations() {
        var expectations =  new EnumMap<WhenToThrow, Class<? extends Throwable>>(WhenToThrow.class);
        return new ExceptionExpecter() {
            public ExceptionSpecifier ifConfiguredToThrowOn(WhenToThrow... interceptionPoints) {
                return c -> { Stream.of(interceptionPoints).forEach(p -> expectations.put(p, c)); return this; };
            }

            public Map<WhenToThrow, Class<? extends Throwable>> getExpectations() {
                return unmodifiableMap(expectations.clone());
            }
        };
    }
}

@ConfigureServer
class ThreadLocalPollutionTestWithSimpleEcho extends ThreadLocalPollutionTest {
    @RemoteImpl
    public static final Echo REMOTE = ThreadLocalPollutionTest::echo;

    ThreadLocalPollutionTestWithSimpleEcho() {
        super(buildExpectations().ifConfiguredToThrowOn(SEND_REQUEST, RECEIVE_REPLY).expect(InterceptorException.class));
    }
}

@ConfigureServer
class ThreadLocalPollutionTestWithUncheckedException extends ThreadLocalPollutionTest {
    static class UncheckedException extends RuntimeException {}

    @RemoteImpl
    public static final Echo REMOTE = s -> { throw new UncheckedException(); };

    ThreadLocalPollutionTestWithUncheckedException() {
        super(buildExpectations()
                .ifConfiguredToThrowOn(NEVER, SEND_POLL, RECEIVE_REPLY, RECEIVE_OTHER).expect(UncheckedException.class)
                .ifConfiguredToThrowOn(SEND_REQUEST, RECEIVE_EXCEPTION).expect(InterceptorException.class));
    }
}

@ConfigureServer(clientOrb = @ConfigureOrb(props = "yoko.orb.policy.request_timeout=1"))
class ThreadLocalPollutionTestWithRequestTimeout extends ThreadLocalPollutionTest {
    @RemoteImpl
    public static final Echo REMOTE = ThreadLocalPollutionTest::sleepyEcho;

    ThreadLocalPollutionTestWithRequestTimeout() {
        super(buildExpectations()
                // Note: RECEIVE_EXCEPTION *does* result in an InterceptorException being thrown on timeout,
                // but the ORB swallows this and propagates the NO_RESPONSE from the timeout anyway.
                .ifConfiguredToThrowOn(NEVER, SEND_POLL, RECEIVE_REPLY, RECEIVE_EXCEPTION, RECEIVE_OTHER).expect(RemoteException.class)
                .ifConfiguredToThrowOn(SEND_REQUEST).expect(InterceptorException.class));
    }
}

@ConfigureServer
class ThreadLocalPollutionTestWithShutdown extends ThreadLocalPollutionTest {
    public static ORB serverOrb;
    @RemoteImpl
    public static final Echo REMOTE = s -> {
        serverOrb.shutdown(false);
        return echo(s);
    };

    @BeforeServer
    public static void stashServerOrb(ORB orb) { serverOrb = orb; }

    ThreadLocalPollutionTestWithShutdown() {
        super(buildExpectations().ifConfiguredToThrowOn(SEND_REQUEST, RECEIVE_REPLY).expect(InterceptorException.class));
    }
}
