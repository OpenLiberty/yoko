/*
 * Copyright 2025 IBM Corporation and others.
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

import org.apache.yoko.orb.OBPortableServer.POAManager_impl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.omg.CORBA.BAD_INV_ORDER;
import org.omg.CORBA.ORB;
import org.omg.CORBA.TRANSIENT;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAManagerPackage.State;
import test.poa.TestDSI_impl;
import test.poa.TestHelper;
import test.poa.Test_impl;
import testify.bus.Bus;
import testify.iiop.annotation.ConfigureServer;
import testify.iiop.annotation.ConfigureServer.BeforeServer;
import testify.iiop.annotation.ConfigureServer.RemoteImpl;
import testify.io.SimpleCloseable;
import testify.util.function.RawRunnable;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.yoko.orb.PortableServer.PolicyValue.NO_IMPLICIT_ACTIVATION;
import static org.apache.yoko.orb.PortableServer.PolicyValue.PERSISTENT;
import static org.apache.yoko.orb.PortableServer.PolicyValue.RETAIN;
import static org.apache.yoko.orb.PortableServer.PolicyValue.UNIQUE_ID;
import static org.apache.yoko.orb.PortableServer.PolicyValue.USER_ID;
import static org.apache.yoko.orb.PortableServer.PolicyValue.create_POA;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static testify.iiop.annotation.ConfigureServer.Separation.COLLOCATED;
import static testify.iiop.annotation.ConfigureServer.Separation.INTER_ORB;
import static testify.util.Assertions.assertThrows;
import static testify.util.Assertions.assertThrowsExactly;

@ConfigureServer(separation = INTER_ORB)
public class PoaManagerServerTest {
    @ConfigureServer(separation = COLLOCATED)
    public static class CollocatedTest extends PoaManagerServerTest {}
    public interface Proxy extends Remote {
        void activate() throws RemoteException;
        void hold_requests(boolean wait_for_completion) throws RemoteException;
        void discard_requests(boolean wait_for_completion) throws RemoteException;
        void deactivate(boolean etherealize_objects, boolean wait_for_completion) throws RemoteException;
        int get_state() throws RemoteException;
    }

    @RemoteImpl
    public static final Proxy impl = new Proxy() {
        public void activate() { wrap(retainMgr::activate); }
        public void hold_requests(boolean w)  { wrap(() -> retainMgr.hold_requests(w)); }
        public void discard_requests(boolean w)  { wrap(() -> retainMgr.discard_requests(w)); }
        public void deactivate(boolean e, boolean w) { wrap(() -> retainMgr.deactivate(e, w)); }
        public int get_state() { return retainMgr.get_state().value(); }
        private void wrap(RawRunnable r) {
            try {
                r.run();
            } catch (RuntimeException e) {
                throw e;
            } catch(Exception e){
                throw new RuntimeException(e);
            }
        }
    };

    static POAManager_impl retainMgr;
    static test.poa.Test test;
    static test.poa.Test testDSI;

    @BeforeServer
    public static void setup(ORB orb, POA root, Bus bus) throws Exception {
        // Create POA w/ RETAIN. This POA should use a separate POAManager.
        POA retainPOA = create_POA("retain", root, null, PERSISTENT, USER_ID, RETAIN, NO_IMPLICIT_ACTIVATION, UNIQUE_ID);
        retainMgr = (POAManager_impl) retainPOA.the_POAManager();

        Test_impl testImpl = new Test_impl(orb, retainPOA);
        byte[] oid = "test".getBytes();
        retainPOA.activate_object_with_id(oid, testImpl);

        test.poa.Test test = testImpl._this();

        TestDSI_impl testDSIImpl = new TestDSI_impl(orb, retainPOA);
        byte[] oidDSI = "testDSI".getBytes();
        retainPOA.activate_object_with_id(oidDSI, testDSIImpl);
        org.omg.CORBA.Object objDSI = retainPOA.create_reference_with_id(oidDSI, "IDL:Test:1.0");
        test.poa.Test testDSI = TestHelper.narrow(objDSI);

        // Don't write references until we're ready to run
        bus.put("test", orb.object_to_string(test));
        bus.put("testDSI", orb.object_to_string(testDSI));
    }

    private static Future<?> assertRequestHeld(Runnable task) {
        ExecutorService xs = newSingleThreadExecutor();
        try (SimpleCloseable ignored = xs::shutdown) {
            Future<?> future = xs.submit(task); //A future represents an asynchronous task that is waiting to be run
            assertFalse(future.isDone());
            assertThrows(TimeoutException.class, () -> future.get(2, MILLISECONDS));
            return future;
        }
    }

    @BeforeAll
    public static void setupClient(ORB orb, Bus bus) {
        test = TestHelper.narrow(orb.string_to_object(bus.get("test")));
        testDSI = TestHelper.narrow(orb.string_to_object(bus.get("testDSI")));
    }

    @Test
    void testPoaMgrBlockedRequests(Proxy proxy) throws Exception {
        proxy.hold_requests(false);

        Future<?> f1 = assertRequestHeld(test::aMethod);  //We are checking that any requests made are being blocked by the POA manager
        Future<?> f2 = assertRequestHeld(testDSI::aMethod);

        proxy.activate(); //This should cause the blocked call to release.
        f1.get(); //checks that the call to aMethod succeeded. Will throw if not
        f2.get();
    }

    @Test
    void testDiscardRequest(Proxy proxy) throws Exception {
        proxy.activate();
        test.aMethod();
        testDSI.aMethod();

        // Try discard_request with wait completion == true, shouldn't work since the discard_request call is done through the POAManagerProxy.
        assertThrowsExactly(RemoteException.class, () -> proxy.discard_requests(true), BAD_INV_ORDER.class);

        assertSame(proxy.get_state(), State._ACTIVE);

        // Test discard_requests when active.
        proxy.discard_requests(false);
        assertSame(proxy.get_state(), State._DISCARDING);

        assertThrowsExactly(TRANSIENT.class, test::aMethod);
        assertThrowsExactly(TRANSIENT.class, testDSI::aMethod);
    }

    @Test
    void testHoldRequest(Proxy proxy) throws Exception {
        proxy.activate();
        test.aMethod();
        testDSI.aMethod();

        //Try hold_request with wait completion == true, shouldn't work since the hold_request call is done through the POAManagerProxy.
        assertThrowsExactly(RemoteException.class, () -> proxy.hold_requests(true), BAD_INV_ORDER.class);
        // Expected, ensure the state didn't change
        assertSame(proxy.get_state(), State._ACTIVE);

        // Test hold_requests when active.
        proxy.hold_requests(false);
        assertSame(proxy.get_state(), State._HOLDING);

        Future<?> f1 = assertRequestHeld(test::aMethod);  //We are checking that any requests made are being blocked by the POA manager
        Future<?> f2 = assertRequestHeld(testDSI::aMethod);
        proxy.activate();
        f1.get();
        f2.get();
    }

    @Test
    void testDiscardRequestWithHoldingRequest(Proxy proxy) throws Exception {
        // Test discard_requests when holding.
        proxy.hold_requests(false);
        assertSame(proxy.get_state(), State._HOLDING);

        Future<?> f1 = assertRequestHeld(test::aMethod);
        Future<?> f2 = assertRequestHeld(testDSI::aMethod);

        proxy.discard_requests(false);
        assertSame(proxy.get_state(), State._DISCARDING);

        assertThrowsExactly(ExecutionException.class, f1::get, TRANSIENT.class);
        assertThrowsExactly(ExecutionException.class, f2::get, TRANSIENT.class);

        // Test hold_requests when discarding.
        proxy.hold_requests(false);
        assertSame(proxy.get_state(), State._HOLDING);

        f1 = assertRequestHeld(test::aMethod);
        f2 = assertRequestHeld(testDSI::aMethod);

        proxy.activate();

        f1.get();
        f2.get();
    }

    @Test
    void testDeactivate(Proxy proxy) throws Exception {
        // Try to deactivate with wait completion == true, shouldn't work since the hold_request call is done through the POAManagerProxy.
        proxy.activate();
        assertThrowsExactly(RemoteException.class, () -> proxy.deactivate(true, true), BAD_INV_ORDER.class);

        // Expected, ensure the state didn't change
        assertSame(proxy.get_state(), State._ACTIVE);
    }
}
