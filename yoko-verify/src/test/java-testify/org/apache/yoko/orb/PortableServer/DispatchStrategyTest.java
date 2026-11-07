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
package org.apache.yoko.orb.PortableServer;

import org.apache.yoko.orb.OB.DispatchRequest;
import org.apache.yoko.orb.OB.DispatchStrategy;
import org.apache.yoko.orb.OB.DispatchStrategyFactoryHelper;
import org.apache.yoko.orb.OBPortableServer.POAHelper;
import org.junit.jupiter.api.Test;
import org.omg.CORBA.Any;
import org.omg.CORBA.LocalObject;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Policy;
import test.poa.TestHelper;
import test.poa.TestPOA;
import testify.bus.Bus;
import testify.iiop.annotation.ConfigureOrb;
import testify.iiop.annotation.ConfigureServer;
import testify.iiop.annotation.ConfigureServer.BeforeServer;
import testify.iiop.annotation.ConfigureServer.RemoteImpl;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Test the POA thread dispatch strategies by making several concurrent calls
 * on an object in a POA using each strategy.
 * The servants record any violation of their strategy's contract,
 * and each test asks the server whether any violation occurred.
 */
@ConfigureServer(serverOrb = @ConfigureOrb(props = {"yoko.orb.conc_model=threaded", "yoko.orb.oa.conc_model=threaded"}))
public class DispatchStrategyTest {
    private static final String SAME_THREAD = "sameThread";
    private static final String THREAD_PER_REQUEST = "threadPerRequest";
    private static final String THREAD_POOL_1 = "threadPool1";
    private static final String THREAD_POOL_2 = "threadPool2";
    private static final String CUSTOM = "custom";

    /** The number of concurrent requests sent to each object. */
    private static final int REQUESTS_PER_OBJECT = 4;

    abstract static class AbstractTest extends TestPOA {
        private volatile boolean failed = false;

        final boolean failed() { return failed; }

        void fail() { failed = true; }
    }

    abstract static class AbstractTestPool extends AbstractTest {
        private final Set<Thread> threadSet = new HashSet<>();
        private final int maxSize;

        AbstractTestPool(int maxSize) { this.maxSize = maxSize; }

        public final synchronized void aMethod() {
            final Thread thisThread = Thread.currentThread();
            if (threadSet.contains(thisThread)) return;
            if (threadSet.size() < maxSize) {
                threadSet.add(thisThread);
                return;
            }
            fail();
        }
    }

    /** Same thread dispatch strategy: every request must be handled by the same thread. */
    static final class TestSameThread_impl extends AbstractTestPool {
        TestSameThread_impl() { super(1); }
    }

    /** Thread per request dispatch strategy: no thread may handle more than one request. */
    static final class TestThreadPerReq_impl extends AbstractTest {
        private final Set<Thread> threadSet = new HashSet<>();

        public synchronized void aMethod() {
            if (!threadSet.add(Thread.currentThread())) fail();
        }
    }

    /** Thread pool dispatch strategy: requests may only be handled by the (two) pool threads. */
    static final class TestThreadPool_impl extends AbstractTestPool {
        TestThreadPool_impl() { super(2); }
    }

    /** Simple custom dispatch strategy (uses same thread). */
    static final class CustDispatchStrategy extends LocalObject implements DispatchStrategy {
        public int id() { return 4; } // Some unique number
        public Any info() { return ORB.init().create_any(); }
        public void dispatch(DispatchRequest request) { request.invoke(); }
    }

    static AbstractTest sameThreadServant;
    static AbstractTest threadPerRequestServant;
    static AbstractTest threadPoolServant;
    static AbstractTest customServant;

    /** Lets the client ask the server whether the servant for a given object has seen a violation of its strategy. */
    public interface Checker extends Remote {
        boolean violated(String objectKey) throws RemoteException;
    }

    @RemoteImpl
    public static final Checker checker = key -> {
        switch (key) {
        case SAME_THREAD: return sameThreadServant.failed();
        case THREAD_PER_REQUEST: return threadPerRequestServant.failed();
        case THREAD_POOL_1: case THREAD_POOL_2: return threadPoolServant.failed();
        case CUSTOM: return customServant.failed();
        default: throw new IllegalArgumentException(key);
        }
    };

    @BeforeServer
    public static void setUp(ORB orb, Bus bus) throws Exception {
        // Use the real root POA (not the child POA that testify injects) so we can create sibling POAs
        // with the Yoko-specific dispatch strategy policies.
        var rootPOA = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
        var manager = rootPOA.the_POAManager();

        var dsf = DispatchStrategyFactoryHelper.narrow(orb.resolve_initial_references("DispatchStrategyFactory"));
        var sameThread = dsf.create_same_thread_strategy();
        var threadPerRequest = dsf.create_thread_per_request_strategy();
        var threadPool = dsf.create_thread_pool_strategy(dsf.create_thread_pool(2));
        var custom = new CustDispatchStrategy();

        // Create POAs with threaded dispatch strategy policies:
        // - same thread
        // - thread per request
        // - thread pool (same pool used for two POAs)
        // - custom strategy
        Policy[] policies = new Policy[1];

        policies[0] = rootPOA.create_dispatch_strategy_policy(sameThread);
        var stPOA = rootPOA.create_POA("stPOA", manager, policies);

        policies[0] = rootPOA.create_dispatch_strategy_policy(threadPerRequest);
        var tprPOA = rootPOA.create_POA("tprPOA", manager, policies);

        policies[0] = rootPOA.create_dispatch_strategy_policy(threadPool);
        var tpPOA1 = rootPOA.create_POA("tpPOA1", manager, policies);
        var tpPOA2 = rootPOA.create_POA("tpPOA2", manager, policies);

        policies[0] = rootPOA.create_dispatch_strategy_policy(custom);
        var cusPOA = rootPOA.create_POA("cusPOA", manager, policies);

        // Create a servant in each POA (the same servant is used in both thread pool POAs)
        sameThreadServant = new TestSameThread_impl();
        threadPerRequestServant = new TestThreadPerReq_impl();
        threadPoolServant = new TestThreadPool_impl();
        customServant = new TestSameThread_impl();

        bus.put(SAME_THREAD, orb.object_to_string(stPOA.id_to_reference(stPOA.activate_object(sameThreadServant))));
        bus.put(THREAD_PER_REQUEST, orb.object_to_string(tprPOA.id_to_reference(tprPOA.activate_object(threadPerRequestServant))));
        bus.put(THREAD_POOL_1, orb.object_to_string(tpPOA1.id_to_reference(tpPOA1.activate_object(threadPoolServant))));
        bus.put(THREAD_POOL_2, orb.object_to_string(tpPOA2.id_to_reference(tpPOA2.activate_object(threadPoolServant))));
        bus.put(CUSTOM, orb.object_to_string(cusPOA.id_to_reference(cusPOA.activate_object(customServant))));
    }

    /**
     * Send several deferred (i.e. concurrent) requests to each of the named objects,
     * wait for all the replies, then check that the server saw no violation of the dispatch strategy.
     */
    private static void callConcurrently(ORB orb, Bus bus, Checker checker, String... objectKeys) throws Exception {
        int sent = 0;
        for (String key : objectKeys) {
            test.poa.Test obj = TestHelper.narrow(orb.string_to_object(bus.get(key)));
            for (int i = 0; i < REQUESTS_PER_OBJECT; i++, sent++) obj._request("aMethod").send_deferred();
        }
        for (int i = 0; i < sent; i++) orb.get_next_response();
        for (String key : objectKeys) assertFalse(checker.violated(key), "dispatch strategy violated for " + key);
    }

    @Test
    void sameThreadStrategy(ORB orb, Bus bus, Checker checker) throws Exception { callConcurrently(orb, bus, checker, SAME_THREAD); }

    @Test
    void threadPerRequestStrategy(ORB orb, Bus bus, Checker checker) throws Exception { callConcurrently(orb, bus, checker, THREAD_PER_REQUEST); }

    @Test
    void threadPoolStrategy(ORB orb, Bus bus, Checker checker) throws Exception { callConcurrently(orb, bus, checker, THREAD_POOL_1, THREAD_POOL_2); }

    @Test
    void customStrategy(ORB orb, Bus bus, Checker checker) throws Exception { callConcurrently(orb, bus, checker, CUSTOM); }
}
