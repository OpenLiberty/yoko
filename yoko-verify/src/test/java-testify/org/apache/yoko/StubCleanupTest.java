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
package org.apache.yoko;

import org.apache.yoko.orb.CORBA.Delegate;
import org.apache.yoko.orb.OB.ORBInstance;
import org.apache.yoko.util.Cache;
import org.junit.jupiter.api.Test;
import org.omg.CORBA.ORB;
import org.omg.CORBA.portable.ObjectImpl;
import test.local.TestHelper;
import test.local.Test_impl;
import testify.bus.Bus;
import testify.iiop.annotation.ConfigureServer;
import testify.iiop.annotation.ConfigureServer.BeforeServer;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.time.Duration;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verify that the client ORB releases its connection to a server when the last stub using it
 * is garbage collected, and not before.
 * <p>
 * Releasing a connection does not close it immediately. It makes the connection <em>idle</em> in the ORB's
 * outbound connection cache, and idle connections are closed lazily when a later connection is requested.
 * So these tests watch the cache, and count a connection as in use if it is cached but not idle.
 * <p>
 * Stubs are created by the tests themselves (rather than injected by testify)
 * so that nothing else holds a reference to them.
 */
@ConfigureServer
public class StubCleanupTest {
    private static final String OBJECT = "object";
    /** How long to wait for garbage collection and the resulting cleanup to happen. */
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    @BeforeServer
    public static void setUp(ORB orb, Bus bus) {
        // Use the ORB's root POA (by passing null) so that _this() can implicitly activate the servant.
        bus.put(OBJECT, orb.object_to_string(new Test_impl(null)._this(orb)));
    }

    private static test.local.Test newStub(ORB orb, Bus bus) {
        return TestHelper.narrow(orb.string_to_object(bus.get(OBJECT)));
    }

    /** The client ORB's cache of outbound connections. The ORB is shared by all the tests in this class. */
    private static Cache<?, ?> connectionCache(ORB orb, Bus bus) {
        ORBInstance orbInstance = ((Delegate) ((ObjectImpl) newStub(orb, bus))._get_delegate())._OB_ORBInstance();
        return orbInstance.getOutboundConnectionCache();
    }

    /** The number of cached connections that are currently being used by at least one stub. */
    private static int connectionsInUse(Cache<?, ?> cache) {
        return cache.size() - cache.idleCount();
    }

    /** Keep requesting garbage collection until the condition is true or the timeout expires. */
    private static boolean gcUntil(BooleanSupplier condition) throws InterruptedException {
        final long deadline = System.nanoTime() + TIMEOUT.toNanos();
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) return false;
            System.gc();
            Thread.sleep(20);
        }
        return true;
    }

    /** Give the cleaner thread (and any other asynchronous cleanup) time to act, with GC running throughout. */
    private static void letCleanupHappen() throws InterruptedException {
        for (int i = 0; i < 10; i++) {
            System.gc();
            Thread.sleep(50);
        }
    }

    /** Make sure that stubs from earlier tests have been cleaned up, so that each test starts from a clean state. */
    private static void awaitNoConnectionsInUse(Cache<?, ?> cache) throws InterruptedException {
        assertTrue(gcUntil(() -> connectionsInUse(cache) == 0), "Connections from earlier tests are still in use");
    }

    private static void assertCollected(WeakReference<?> ref) throws InterruptedException {
        assertTrue(gcUntil(() -> ref.get() == null), "The stub was not garbage collected");
    }

    @Test
    void connectionIsReleasedWhenStubIsCollected(ORB orb, Bus bus) throws Exception {
        Cache<?, ?> cache = connectionCache(orb, bus);
        awaitNoConnectionsInUse(cache);

        WeakReference<Object> stub = useNewStubOnce(orb, bus);
        assertEquals(1, connectionsInUse(cache), "The stub should be using a connection");

        assertCollected(stub);
        assertTrue(gcUntil(() -> connectionsInUse(cache) == 0),
                "The connection should have been released after the stub was collected");
    }

    /** Create a stub, use it to make a call (which opens the connection), and return only a weak reference to it. */
    private static WeakReference<Object> useNewStubOnce(ORB orb, Bus bus) {
        test.local.Test stub = newStub(orb, bus);
        stub.say("hello");
        return new WeakReference<>(stub);
    }

    @Test
    void connectionIsNotReleasedWhileStubIsReachable(ORB orb, Bus bus) throws Exception {
        Cache<?, ?> cache = connectionCache(orb, bus);
        awaitNoConnectionsInUse(cache);

        test.local.Test stub = newStub(orb, bus);
        stub.say("hello");
        assertEquals(1, connectionsInUse(cache), "The stub should be using a connection");

        letCleanupHappen();

        assertEquals(1, connectionsInUse(cache), "The connection must not be released while a stub is still in use");
        stub.say("still working");
        Reference.reachabilityFence(stub);
    }

    @Test
    void connectionIsReleasedOnlyWhenTheLastStubIsCollected(ORB orb, Bus bus) throws Exception {
        Cache<?, ?> cache = connectionCache(orb, bus);
        awaitNoConnectionsInUse(cache);

        // Two stubs for the same object. An array lets us drop one without the other.
        Object[] stubs = { newStub(orb, bus), newStub(orb, bus) };
        ((test.local.Test) stubs[0]).say("hello");
        ((test.local.Test) stubs[1]).say("hello");
        assertEquals(1, cache.size(), "Both stubs should share a single connection");

        // drop the first stub
        WeakReference<Object> first = new WeakReference<>(stubs[0]);
        stubs[0] = null;
        assertCollected(first);
        letCleanupHappen();

        assertEquals(1, connectionsInUse(cache), "The connection must stay in use while the second stub is still in use");
        ((test.local.Test) stubs[1]).say("still working");

        // drop the second stub
        WeakReference<Object> second = new WeakReference<>(stubs[1]);
        stubs[1] = null;
        assertCollected(second);

        assertTrue(gcUntil(() -> connectionsInUse(cache) == 0),
                "The connection should have been released after the last stub was collected");
    }
}
