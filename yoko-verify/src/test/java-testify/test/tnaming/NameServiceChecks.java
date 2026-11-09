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
package test.tnaming;

import org.apache.yoko.orb.spi.naming.Resolvable;
import org.apache.yoko.orb.spi.naming.Resolver;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.ORB;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.CosNaming.Binding;
import org.omg.CosNaming.BindingHolder;
import org.omg.CosNaming.BindingIteratorHolder;
import org.omg.CosNaming.BindingListHolder;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContext;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;
import org.omg.CosNaming.NamingContextHelper;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import testify.bus.Bus;

import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The server-side set-up and client-side checks shared by the name service tests.
 * <p>
 * The server creates three test objects and binds them into a small tree of naming contexts.
 * It then publishes the stringified object references and the stringified names they are bound under
 * on the {@link Bus}, so the client can check that each name resolves to the right object.
 * <p>
 * This class is in the same package as {@link Util} because the assertions it relies on are package-private.
 */
public final class NameServiceChecks {
    private static final NameComponent LEVEL1 = new NameComponent("level1", "test");
    private static final NameComponent LEVEL2 = new NameComponent("level2", "");

    private static final NameComponent TEST1 = new NameComponent("Test1", "");
    private static final NameComponent TEST2 = new NameComponent("Test2", "");
    private static final NameComponent TEST3 = new NameComponent("Test3", "");

    private static final NameComponent RESOLVABLE_TEST = new NameComponent("ResolvableTest", "");
    private static final NameComponent RESOLVER_TEST = new NameComponent("ResolverTest", "");

    private NameServiceChecks() {}

    public static NamingContextExt narrow(org.omg.CORBA.Object nameService) {
        return NamingContextExtHelper.narrow(nameService);
    }

    private static String iorKey(int n) { return "test" + n + ".ior"; }
    private static String nameKey(int n) { return "test" + n + ".name"; }

    private static POA activatedRootPoa(ORB orb) throws Exception {
        POA rootPoa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
        rootPoa.the_POAManager().activate();
        return rootPoa;
    }

    // ------------------------------------------------------------------
    // Server side
    // ------------------------------------------------------------------

    /**
     * Create three test objects in the given (server) ORB, bind them into the name service,
     * and publish their references and names on the bus.
     *
     * @param orb the server ORB
     * @param root the root naming context to bind the objects into
     */
    public static void publishTestObjects(ORB orb, NamingContextExt root, Bus bus) throws Exception {
        final POA rootPoa = activatedRootPoa(orb);
        final Test test1 = TestHelper.narrow(new Test_impl(rootPoa, "Test1")._this_object(orb));
        final Test test2 = TestHelper.narrow(new Test_impl(rootPoa, "Test2")._this_object(orb));
        final Test test3 = TestHelper.narrow(new Test_impl(rootPoa, "Test3")._this_object(orb));

        final NamingContext nc1 = root.new_context();
        root.bind_context(new NameComponent[]{LEVEL1}, nc1);
        final NamingContext nc2 = root.bind_new_context(new NameComponent[]{LEVEL1, LEVEL2});

        Util.assertNameNotBound(root, TEST1);
        root.bind(new NameComponent[]{TEST1}, test1);
        Util.assertTestIsBound("Test1", root, TEST1);

        nc1.bind(new NameComponent[]{TEST2}, test2);
        Util.assertTestIsBound("Test2", root, LEVEL1, TEST2);

        root.bind(new NameComponent[]{LEVEL1, LEVEL2, TEST3}, test3);
        Util.assertTestIsBound("Test3", root, LEVEL1, LEVEL2, TEST3);

        final Test test3a = TestHelper.narrow(new Test_impl(rootPoa, "Test3a")._this_object(orb));
        nc2.rebind(new NameComponent[]{TEST3}, test3a);
        Util.assertTestIsBound("Test3a", root, LEVEL1, LEVEL2, TEST3);

        root.unbind(new NameComponent[]{LEVEL1, LEVEL2, TEST3});
        Util.assertNameNotBound(nc2, TEST3);

        nc2.bind(new NameComponent[]{TEST3}, test3);
        Util.assertTestIsBound("Test3", root, LEVEL1, LEVEL2, TEST3);

        nc1.unbind(new NameComponent[]{LEVEL2});
        Util.assertNameNotBound(root, LEVEL1, LEVEL2, TEST3);

        nc1.rebind_context(new NameComponent[]{LEVEL2}, nc2);

        bus.put(iorKey(1), orb.object_to_string(test1));
        bus.put(nameKey(1), root.to_string(new NameComponent[]{TEST1}));
        bus.put(iorKey(2), orb.object_to_string(test2));
        bus.put(nameKey(2), root.to_string(new NameComponent[]{LEVEL1, TEST2}));
        bus.put(iorKey(3), orb.object_to_string(test3));
        bus.put(nameKey(3), root.to_string(new NameComponent[]{LEVEL1, LEVEL2, TEST3}));
    }

    /**
     * Bind two object factories into the name service: one that is a {@link Resolvable}
     * and one that is a {@link Resolver}. Both should be treated identically.
     */
    public static void bindObjectFactories(ORB orb, NamingContextExt root) throws Exception {
        final POA rootPoa = activatedRootPoa(orb);
        final Resolvable resolvable = new TestFactory_impl(rootPoa, orb, RESOLVABLE_TEST.id);
        final Resolver resolver = new Resolver() {
            @Override
            public org.omg.CORBA.Object resolve() {
                return resolvable.resolve();
            }
        };
        root.bind(new NameComponent[]{RESOLVABLE_TEST}, resolvable);
        Util.assertFactoryIsBound(root, RESOLVABLE_TEST);
        root.bind(new NameComponent[]{RESOLVER_TEST}, resolver);
        Util.assertFactoryIsBound(root, RESOLVER_TEST);
    }

    // ------------------------------------------------------------------
    // Iterator bindings (created on whichever side suits the test)
    // ------------------------------------------------------------------

    /**
     * Create the bindings that {@link #checkIterators} lists.
     * The bound objects are hosted by the given ORB, which need not be the one hosting the name service.
     */
    public static void createBindingsOverWhichToIterate(ORB orb, NamingContext root) throws Exception {
        activatedRootPoa(orb);
        Util.createBindingsOverWhichToIterate(orb, root);
    }

    /** Remove the bindings created by {@link #createBindingsOverWhichToIterate}, leaving everything else alone. */
    public static void removeBindingsOverWhichToIterate(NamingContext root) throws Exception {
        final NamingContext nc = NamingContextHelper.narrow(root.resolve(Util.ITERATOR_TEST_CONTEXT_PATH));
        Util.unbindEverything(nc);
        root.unbind(Util.ITERATOR_TEST_CONTEXT_PATH);
        nc.destroy();
    }

    // ------------------------------------------------------------------
    // Client side
    // ------------------------------------------------------------------

    /**
     * Check that each published name resolves to the right object, both via the name service
     * and via a corbaname URL that finds the name service from the client ORB's initial references.
     * The client ORB must have a "NameService" initial reference for the corbaname check.
     */
    public static void checkBoundReferences(ORB orb, NamingContextExt root, Bus bus) throws Exception {
        makeInitialReference(orb, root);
        for (int i = 1; i <= 3; i++) {
            final Test expected = getTestObject(orb, bus.get(iorKey(i)));
            Util.assertTestIsBound(expected, root, bus.get(nameKey(i)));
        }
        final Test test1 = getTestObject(orb, bus.get(iorKey(1)));
        Util.assertCorbanameIsBound(test1, orb, "corbaname:rir:/NameService#" + bus.get(nameKey(1)));
    }

    /** Make the name service available to the client ORB as the "NameService" initial reference, if it is not already. */
    private static void makeInitialReference(ORB orb, NamingContextExt root) throws Exception {
        try {
            orb.resolve_initial_references("NameService");
        } catch (InvalidName e) {
            ((org.apache.yoko.orb.CORBA.ORB) orb).register_initial_reference("NameService", root);
        }
    }

    private static Test getTestObject(ORB orb, String ref) {
        assertNotNull(ref, "Reference should not have been null");
        final org.omg.CORBA.Object obj = orb.string_to_object(ref);
        assertNotNull(obj, "Orb should have created a non-null stub");
        final Test result = TestHelper.narrow(obj);
        assertNotNull(result, "Should have been able to narrow stub to a Test");
        return result;
    }

    /** Check the binding iterators behave correctly for every possible size of initial list. */
    public static void checkIterators(NamingContextExt root) throws Exception {
        final NamingContextExt nc = NamingContextExtHelper.narrow(root.resolve(Util.ITERATOR_TEST_CONTEXT_PATH));
        for (int listSize = 0; listSize <= Util.EXPECTED_NAMES.size() + 1; listSize++) {
            final BindingListHolder blh = new BindingListHolder();
            final BindingIteratorHolder bih = new BindingIteratorHolder();
            final BindingHolder bh = new BindingHolder();
            final Set<String> actualNames = new TreeSet<>();

            nc.list(listSize, blh, bih); // <-- this is what we are testing

            final int expectedListSize = Math.min(listSize, Util.EXPECTED_NAMES.size());
            assertEquals(expectedListSize, blh.value.length,
                    "Should have as many elements in the list as requested or available");
            for (Binding b : blh.value) {
                final String name = b.binding_name[0].id;
                assertTrue(Util.EXPECTED_NAMES.contains(name), "Name '" + name + "' should be an expected one");
                assertFalse(actualNames.contains(name), "Name '" + name + "' should not be a dupe");
                actualNames.add(name);
            }
            for (int i = expectedListSize; i < Util.EXPECTED_NAMES.size(); i++) {
                assertTrue(bih.value.next_one(bh));
                final String name = bh.value.binding_name[0].id;
                assertTrue(Util.EXPECTED_NAMES.contains(name), "Name '" + name + "' should be an expected one");
                assertFalse(actualNames.contains(name), "Name '" + name + "' should not be a dupe");
                actualNames.add(name);
            }
            assertFalse(bih.value.next_one(bh));
            assertEquals(0, bh.value.binding_name.length);
            assertEquals(Util.EXPECTED_NAMES, actualNames);
        }
    }

    /** Check the factories are invoked on each resolution and so yield a different object each time. */
    public static void checkObjectFactories(NamingContextExt root) throws Exception {
        Util.assertFactoryIsBound(root, RESOLVABLE_TEST);
        Util.assertFactoryIsBound(root, RESOLVER_TEST);
    }

    private enum WriteMethod {
        bind, bind_context, bind_new_context, destroy, new_context, rebind_context, rebind, unbind
    }

    /** Check that every operation that would modify a read-only name service is rejected. */
    public static void checkWritesAreRejected(NamingContextExt root) {
        final NameComponent[] foo = Util.makeName("foo");
        for (WriteMethod method : WriteMethod.values()) {
            try {
                switch (method) {
                    case bind: root.bind(foo, root); break;
                    case bind_context: root.bind_context(foo, root); break;
                    case bind_new_context: root.bind_new_context(foo); break;
                    case destroy: root.destroy(); break;
                    case new_context: root.new_context(); break;
                    case rebind: root.rebind(foo, root); break;
                    case rebind_context: root.rebind_context(foo, root); break;
                    case unbind: root.unbind(foo); break;
                }
                fail(method + " should have thrown a NO_PERMISSION exception");
            } catch (NO_PERMISSION expected) {
                // expected
            } catch (org.omg.CORBA.UserException e) {
                fail(method + " should have thrown a NO_PERMISSION exception but threw " + e);
            }
        }
    }
}
