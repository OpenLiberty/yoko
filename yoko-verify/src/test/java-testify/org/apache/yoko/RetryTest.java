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

import org.apache.yoko.orb.OB.RETRY_ALWAYS;
import org.apache.yoko.orb.OB.RETRY_NEVER;
import org.apache.yoko.orb.OB.RETRY_POLICY_ID;
import org.apache.yoko.orb.OB.RETRY_STRICT;
import org.apache.yoko.orb.OB.RetryAttributes;
import org.apache.yoko.orb.OB.RetryAttributesHelper;
import org.junit.jupiter.api.Test;
import org.omg.CORBA.Any;
import org.omg.CORBA.LocalObject;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Policy;
import org.omg.CORBA.PolicyError;
import org.omg.CORBA.SetOverrideType;
import org.omg.CORBA.SystemException;
import org.omg.CORBA.TRANSIENT;
import org.omg.PortableServer.ForwardRequest;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import org.omg.PortableServer.Servant;
import org.omg.PortableServer.ServantLocator;
import org.omg.PortableServer.ServantLocatorPackage.CookieHolder;
import test.retry.Retry;
import test.retry.RetryHelper;
import test.retry.RetryServer;
import test.retry.RetryServerHelper;
import test.retry.RetryServer_impl;
import test.retry.Retry_impl;
import test.retry.TestHelper;
import testify.bus.Bus;
import testify.iiop.annotation.ConfigureServer;
import testify.iiop.annotation.ConfigureServer.BeforeServer;

import static org.apache.yoko.orb.exceptions.Transients.LOCATION_FORWARD_TOO_MANY_HOPS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.omg.PortableServer.ImplicitActivationPolicyValue.NO_IMPLICIT_ACTIVATION;
import static org.omg.PortableServer.RequestProcessingPolicyValue.USE_SERVANT_MANAGER;
import static org.omg.PortableServer.ServantRetentionPolicyValue.NON_RETAIN;

/** Test the ORB's retry policy and its detection of location forwarding loops. */
@ConfigureServer
public class RetryTest {
    private static final String SERVER = "server";

    /** A servant locator that always forwards requests to the same object. */
    static final class FwdLocator_impl extends LocalObject implements ServantLocator {
        private final org.omg.CORBA.Object target;

        FwdLocator_impl(org.omg.CORBA.Object target) { this.target = target; }

        public Servant preinvoke(byte[] oid, POA poa, String operation, CookieHolder cookie) throws ForwardRequest {
            throw new ForwardRequest(target);
        }

        public void postinvoke(byte[] oid, POA poa, String operation, java.lang.Object cookie, Servant servant) {}
    }

    @BeforeServer
    public static void setUp(ORB orb, Bus bus) throws Exception {
        // Use the real root POA (not the child POA testify injects) so that _this() can implicitly activate servants.
        POA rootPOA = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));

        Policy[] policies = {
                rootPOA.create_request_processing_policy(USE_SERVANT_MANAGER),
                rootPOA.create_servant_retention_policy(NON_RETAIN),
                rootPOA.create_implicit_activation_policy(NO_IMPLICIT_ACTIVATION),
        };

        // Create two POAs with servant locators, each with its own POAManager (and therefore its own endpoints).
        // The locators simply forward to an object in the other POA, thus creating an infinite loop.
        POA poa1 = rootPOA.create_POA("poa1", null, policies);
        POA poa2 = rootPOA.create_POA("poa2", null, policies);
        org.omg.CORBA.Object obj1 = poa1.create_reference("IDL:Test:1.0");
        org.omg.CORBA.Object obj2 = poa2.create_reference("IDL:Test:1.0");
        poa1.set_servant_manager(new FwdLocator_impl(obj2));
        poa2.set_servant_manager(new FwdLocator_impl(obj1));
        poa1.the_POAManager().activate();
        poa2.the_POAManager().activate();

        Retry retry = new Retry_impl(rootPOA)._this(orb);
        RetryServer server = new RetryServer_impl(rootPOA, TestHelper.narrow(obj1), retry)._this(orb);

        // Publish the reference only once everything is ready.
        bus.put(SERVER, orb.object_to_string(server));
    }

    private static RetryServer server(ORB orb, Bus bus) {
        RetryServer server = RetryServerHelper.narrow(orb.string_to_object(bus.get(SERVER)));
        assertNotNull(server);
        return server;
    }

    /**
     * Get a stub for the server's Retry object that has the given retry policy.
     * Note that the Retry object counts the calls made to it, so each test needs to call
     * {@link Retry#raise_exception(int, boolean)} to reset the count before using it.
     */
    private static Retry retryWithPolicy(ORB orb, Bus bus, short mode, int interval, int max, boolean remote) throws PolicyError {
        RetryAttributes attrib = new RetryAttributes();
        attrib.mode = mode;
        attrib.interval = interval;
        attrib.max = max;
        attrib.remote = remote;
        Any any = orb.create_any();
        RetryAttributesHelper.insert(any, attrib);
        Policy[] policies = { orb.create_policy(RETRY_POLICY_ID.value, any) };
        org.omg.CORBA.Object obj = server(orb, bus).get_retry_object()._set_policy_override(policies, SetOverrideType.SET_OVERRIDE);
        return RetryHelper.narrow(obj);
    }

    @Test
    void forwardingLoopIsDetected(ORB orb, Bus bus) {
        test.retry.Test test = server(orb, bus).get_location_forward_object();
        TRANSIENT ex = assertThrows(TRANSIENT.class, test::aMethod);
        assertTrue(LOCATION_FORWARD_TOO_MANY_HOPS.matches(ex), "Expected too many hops, but got: " + ex);
    }

    @Test
    void remoteExceptionsAreNotRetriedByDefault(ORB orb, Bus bus) throws Exception {
        Retry retry = retryWithPolicy(orb, bus, RETRY_STRICT.value, 0, 0, false);
        retry.raise_exception(1, false);
        try {
            retry.aMethod();
            // This functionality is broken: TRANSIENT is (usually) not a remote exception,
            // and nothing else is treated as a remote exception.
            // TODO: fix this functionality and assert that an exception is thrown.
        } catch (TRANSIENT expected) {
            assertEquals(1, retry.get_count());
        }
    }

    @Test
    void remoteExceptionsAreRetriedOnceWhenRequested(ORB orb, Bus bus) throws Exception {
        Retry retry = retryWithPolicy(orb, bus, RETRY_STRICT.value, 0, 0, true);
        retry.raise_exception(1, false);
        retry.aMethod();
        assertEquals(2, retry.get_count());
    }

    @Test
    void retryStrictDoesNotRetryIfCompletionIsMaybe(ORB orb, Bus bus) throws Exception {
        Retry retry = retryWithPolicy(orb, bus, RETRY_STRICT.value, 0, 0, true);
        retry.raise_exception(1, true);
        assertThrows(SystemException.class, retry::aMethod);
        assertEquals(1, retry.get_count());
    }

    @Test
    void retryNeverDoesNotRetry(ORB orb, Bus bus) throws Exception {
        Retry retry = retryWithPolicy(orb, bus, RETRY_NEVER.value, 0, 0, true);
        retry.raise_exception(1, false);
        assertThrows(TRANSIENT.class, retry::aMethod);
        assertEquals(1, retry.get_count());
    }

    @Test
    void retryAlwaysRetriesEvenIfCompletionIsMaybe(ORB orb, Bus bus) throws Exception {
        Retry retry = retryWithPolicy(orb, bus, RETRY_ALWAYS.value, 0, 0, true);
        retry.raise_exception(1, true);
        retry.aMethod();
        assertEquals(2, retry.get_count());
    }

    @Test
    void multipleRetries(ORB orb, Bus bus) throws Exception {
        Retry retry = retryWithPolicy(orb, bus, RETRY_STRICT.value, 0, 0, true);
        retry.raise_exception(5, false);
        retry.aMethod();
        assertEquals(6, retry.get_count());
    }

    @Test
    void retriesStopAtMaximum(ORB orb, Bus bus) throws Exception {
        Retry retry = retryWithPolicy(orb, bus, RETRY_STRICT.value, 0, 5, true);
        retry.raise_exception(6, false);
        assertThrows(TRANSIENT.class, retry::aMethod);
        assertEquals(6, retry.get_count());
    }

    @Test
    void retryIntervalIsHonoured(ORB orb, Bus bus) throws Exception {
        Retry retry = retryWithPolicy(orb, bus, RETRY_STRICT.value, 100, 0, true);
        retry.raise_exception(12, false);
        long start = System.currentTimeMillis();
        retry.aMethod();
        long elapsed = System.currentTimeMillis() - start;
        if (elapsed <= 1000) fail("Twelve retries at 100ms intervals should take over a second, but took " + elapsed + "ms");
    }
}
