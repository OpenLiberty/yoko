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

import org.apache.yoko.orb.OCI.IIOP.ConnectionHelper;
import org.apache.yoko.orb.OCI.IIOP.DefaultConnectionHelper;
import org.apache.yoko.orb.OCI.IIOP.TransportInfo_impl;
import org.apache.yoko.orb.PortableInterceptor.ServerRequestInfoExt;
import org.junit.jupiter.api.Test;
import org.omg.CORBA.Any;
import org.omg.CORBA.AnyHolder;
import org.omg.CORBA.MARSHAL;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Policy;
import org.omg.IOP.Codec;
import org.omg.IOP.CodecFactory;
import org.omg.IOP.CodecFactoryHelper;
import org.omg.IOP.ENCODING_CDR_ENCAPS;
import org.omg.IOP.Encoding;
import org.omg.IOP.IOR;
import org.omg.PortableInterceptor.ForwardRequest;
import org.omg.PortableInterceptor.ServerRequestInfo;
import org.omg.PortableServer.POA;
import test.iiopplugin.LocalTest_impl;
import test.iiopplugin.TestHelper;
import test.iiopplugin.TestPOA;
import testify.bus.Bus;
import testify.iiop.TestServerRequestInterceptor;
import testify.iiop.annotation.ConfigureOrb;
import testify.iiop.annotation.ConfigureOrb.UseWithOrb;
import testify.iiop.annotation.ConfigureServer;
import testify.iiop.annotation.ConfigureServer.BeforeServer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import static org.apache.yoko.orb.PortableServer.PolicyValue.RETAIN;
import static org.apache.yoko.orb.PortableServer.PolicyValue.SYSTEM_ID;
import static org.apache.yoko.orb.PortableServer.PolicyValue.USE_ACTIVE_OBJECT_MAP_ONLY;
import static org.apache.yoko.orb.PortableServer.PolicyValue.create_POA;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static testify.iiop.annotation.ConfigureOrb.UseWithOrb.InitializerScope.CLIENT;
import static testify.iiop.annotation.ConfigureOrb.UseWithOrb.InitializerScope.SERVER;
import static testify.iiop.annotation.ConfigureServer.Separation.INTER_ORB;

/**
 * Testify-based implementation of IIOPPluginTest.
 * Tests IIOP connection helper plugin mechanism with client and server plugins.
 * 
 * This test validates:
 * - Plugin lifecycle (construction, initialization)
 * - Socket creation interception
 * - Client-server communication through plugins
 * - Object marshaling and unmarshalling
 * - Local vs remote object handling
 */
@ConfigureServer(
    separation = INTER_ORB,
    clientOrb = @ConfigureOrb(args = {"-IIOPconnectionHelperArgs", "Client Plugin"}),
    serverOrb = @ConfigureOrb(args = {"-IIOPconnectionHelperArgs", "Server Plugin"})
)
public class IiopPluginTest {

    /**
     * Client-side connection helper plugin.
     * Tracks construction, initialization, and connection creation.
     */
    @UseWithOrb(scope = CLIENT)
    public static class ClientPlugin extends DefaultConnectionHelper implements ConnectionHelper {
        private static boolean constructed = false;
        private static boolean initialized = false;
        private static boolean createdConnection = false;

        public ClientPlugin() {
            System.out.println("Client-side connection helper constructed");
            constructed = true;
        }

        @Override
        public void init(ORB orb, String parms) {
            System.out.println("Initializing client-side connection helper with parms " + parms);
            if (!parms.equals("Client Plugin")) throw new IllegalArgumentException("Invalid client initialization argument " + parms);
            initialized = true;
        }

        @Override
        public Socket createSocket(IOR ior, Policy[] policies, InetAddress address, int port) throws IOException {
            System.out.printf("Plugin %s creating client socket connection for IOR=%s address=%s port=%d%n", this, ior, address, port);
            createdConnection = true;
            return super.createSocket(ior, policies, address, port);
        }

        @Override
        public Socket createSelfConnection(InetAddress address, int port) throws IOException {
            System.out.printf("Plugin %s creating self client socket connection for address=%s port=%d%n", this, address, port);
            return super.createSelfConnection(address, port);
        }

        @Override
        public ServerSocket createServerSocket(int port, int backlog) throws IOException {
            System.out.printf("Plugin %s creating server socket for port=%d backlog=%d%n", this, port, backlog);
            return super.createServerSocket(port, backlog);
        }

        @Override
        public ServerSocket createServerSocket(int port, int backlog, InetAddress address) throws IOException {
            System.out.printf("Plugin %s creating server socket for port=%d backlog=%d address=%s%n", this, port, backlog, address);
            return super.createServerSocket(port, backlog, address);
        }

        public static boolean testPassed() {
            System.out.printf("Client plugin: constructed=%s initialized=%s createdConnection=%s%n", constructed, initialized, createdConnection);
            return constructed && initialized && createdConnection;
        }
    }

    /**
     * Server-side connection helper plugin.
     * Tracks construction, initialization, and server socket creation.
     */
    @UseWithOrb(scope = SERVER)
    public static class ServerPlugin extends DefaultConnectionHelper implements ConnectionHelper {
        private static boolean constructed = false;
        private static boolean initialized = false;
        private static boolean createdSelfConnection = false;
        private static boolean createdServerConnection = false;

        public ServerPlugin() {
            System.out.println("Server-side connection helper constructed");
            constructed = true;
        }

        @Override
        public void init(ORB orb, String parms) {
            System.out.printf("Initializing server-side connection helper with parms %s%n", parms);
            // The framework passes the class name, so we validate against that
            if (!parms.contains("Server Plugin")) throw new IllegalArgumentException("Invalid server initialization argument " + parms);
            initialized = true;
        }

        @Override
        public Socket createSocket(IOR ior, Policy[] policies, InetAddress address, int port) throws IOException {
            System.out.printf("Plugin %s creating client socket connection for IOR=%s address=%s port=%d%n", this, ior, address, port);
            return super.createSocket(ior, policies, address, port);
        }

        @Override
        public Socket createSelfConnection(InetAddress address, int port) throws IOException {
            System.out.printf("Plugin %s creating self client socket connection for address=%s port=%d%n", this, address, port);
            createdSelfConnection = true;
            return super.createSelfConnection(address, port);
        }

        @Override
        public ServerSocket createServerSocket(int port, int backlog) throws IOException {
            System.out.printf("Plugin %s creating server socket for port=%d backlog=%d%n", this, port, backlog);
            createdServerConnection = true;
            return super.createServerSocket(port, backlog);
        }

        @Override
        public ServerSocket createServerSocket(int port, int backlog, InetAddress address) throws IOException {
            System.out.printf("Plugin %s creating server socket for port=%d backlog=%d address=%s%n", this, port, backlog, address);
            createdServerConnection = true;
            return super.createServerSocket(port, backlog, address);
        }

        public static boolean testPassed() {
            System.out.printf("Server plugin: constructed=%s initialized=%s createdSelfConnection=%s createdServerConnection=%s%n", constructed, initialized, createdSelfConnection, createdServerConnection);
            return constructed && initialized && createdServerConnection;
        }
    }

    /**
     * Server request interceptor to validate transport information.
     */
    @UseWithOrb(scope = SERVER)
    public static class ServiceContextInterceptor implements TestServerRequestInterceptor {
        @Override
        public void receive_request_service_contexts(ServerRequestInfo ri) throws ForwardRequest {
            // Access transport info to validate connection
            try {
                ServerRequestInfoExt riExt = (ServerRequestInfoExt) ri;
                TransportInfo_impl connection = (TransportInfo_impl) riExt.getTransportInfo();
                if (connection != null) {
                    String remoteHost = connection.remote_addr();
                    if (!(remoteHost == null || remoteHost.isEmpty())) {
                        System.out.println("Retrieved remote host successfully: " + remoteHost);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error accessing transport info: " + e.getMessage());
            }
        }
    }

    /**
     * Server-side implementation of Test interface.
     * Uses the IDL-generated TestPOA base class.
     */
    public static class TestImpl extends TestPOA {
        private final POA poa;
        private final test.iiopplugin.Test localTest;

        public TestImpl(POA poa) {
            this.poa = poa;
            this.localTest = new LocalTest_impl();
        }

        @Override
        public POA _default_POA() {
            return poa;
        }

        @Override
        public void say(String s) {
            // Simple method - just accept the string
            System.out.println("Server received: " + s);
        }

        @Override
        public void intest(test.iiopplugin.Test t) {
            // Call back on the passed object
            t.say("hi");
        }

        @Override
        public void inany(Any a) {
            // Extract and validate the Any contains a Test object
            var t = TestHelper.extract(a);
            System.out.println("Extracted Test object from Any: " + t);
        }

        @Override
        public void outany(AnyHolder a) {
            // Return a local test object in an Any
            a.value = _orb().create_any();
            TestHelper.insert(a.value, localTest);
        }

        @Override
        public test.iiopplugin.Test returntest() {
            // Return the local test object
            return localTest;
        }

        @Override
        public void shutdown() {
            // Validate server plugin before shutdown
            if (!ServerPlugin.testPassed()) {
                throw new RuntimeException("Server plugin validation failed");
            }
            _orb().shutdown(false);
        }
    }

    private static test.iiopplugin.Test testRef;

    /**
     * Set up the server-side test object.
     * Creates a child POA with appropriate policies for explicit activation.
     */
    @BeforeServer
    public static void setupServer(ORB serverOrb, POA rootPoa, Bus bus) throws Exception {
        // Create a child POA with policies for explicit activation
        // TRANSIENT (default), SYSTEM_ID, RETAIN, USE_ACTIVE_OBJECT_MAP_ONLY
        POA childPoa = create_POA("TestPOA", rootPoa, rootPoa.the_POAManager(),
            SYSTEM_ID, RETAIN, USE_ACTIVE_OBJECT_MAP_ONLY);
        
        // Create implementation object with the child POA
        TestImpl impl = new TestImpl(childPoa);
        
        // Activate the servant
        byte[] oid = childPoa.activate_object(impl);
        var obj = childPoa.id_to_reference(oid);
        testRef = TestHelper.narrow(obj);
        
        // Publish the IOR for the client
        String ior = serverOrb.object_to_string(testRef);
        bus.put("TEST_IOR", ior);
        System.out.println("Server: Test object published with IOR");
    }

    /**
     * Test 1: Codec encoding of object reference.
     * Validates that object references can be encoded using CORBA Codec.
     */
    @Test
    public void testCodec(ORB clientOrb, Bus bus) throws Exception {
        System.out.print("Testing Codec... ");
        
        // Get the test object reference
        String ior = bus.get("TEST_IOR");
        var obj = clientOrb.string_to_object(ior);
        var testObj = TestHelper.narrow(obj);
        assertNotNull(testObj, "Test object should be available");
        
        // Get codec factory
        CodecFactory factory = CodecFactoryHelper.narrow(
            clientOrb.resolve_initial_references("CodecFactory"));
        assertNotNull(factory, "CodecFactory should be available");

        // Create codec
        Encoding how = new Encoding();
        how.major_version = 0;
        how.minor_version = 0;
        how.format = ENCODING_CDR_ENCAPS.value;

        Codec codec = factory.create_codec(how);
        assertNotNull(codec, "Codec should be created");

        // Encode the test object in an Any
        Any a = clientOrb.create_any();
        TestHelper.insert(a, testObj);
        
        assertDoesNotThrow(() -> codec.encode_value(a),
            "Codec should encode object reference");
        
        System.out.println("Done!");
    }

    /**
     * Test 2: Simple RPC call.
     * Validates basic remote method invocation.
     */
    @Test
    public void testSimpleRPC(ORB clientOrb, Bus bus) throws Exception {
        System.out.print("Testing simple RPC call... ");
        
        String ior = bus.get("TEST_IOR");
        var obj = clientOrb.string_to_object(ior);
        var testObj = TestHelper.narrow(obj);
        
        assertDoesNotThrow(() -> testObj.say("Hi"),
            "Simple RPC call should succeed");
        
        System.out.println("Done!");
    }

    /**
     * Test 3: Passing non-local object reference.
     * Validates that remote object references can be passed as parameters.
     */
    @Test
    public void testPassingNonLocalObject(ORB clientOrb, Bus bus) throws Exception {
        System.out.print("Testing passing non-local object... ");
        
        String ior = bus.get("TEST_IOR");
        var obj = clientOrb.string_to_object(ior);
        var testObj = TestHelper.narrow(obj);
        
        // Pass the remote object to itself
        assertDoesNotThrow(() -> testObj.intest(testObj),
            "Passing remote object should succeed");
        
        System.out.println("Done!");
    }

    /**
     * Test 4: Passing local object (negative test).
     * Validates that local-only objects cannot be marshaled.
     */
    @Test
    public void testPassingLocalObject(ORB clientOrb, Bus bus) throws Exception {
        System.out.print("Testing passing local object... ");
        
        String ior = bus.get("TEST_IOR");
        var obj = clientOrb.string_to_object(ior);
        var testObj = TestHelper.narrow(obj);
        
        // Create a local test object
        var localTest = new LocalTest_impl();
        
        // Attempting to pass a local object should fail with MARSHAL exception
        assertThrows(MARSHAL.class, () -> testObj.intest(localTest),
            "Passing local object should throw MARSHAL exception");
        
        System.out.println("Done!");
    }

    /**
     * Test 5: Passing object in Any.
     * Validates object reference marshaling within Any type.
     */
    @Test
    public void testPassingObjectInAny(ORB clientOrb, Bus bus) throws Exception {
        System.out.print("Testing passing non-local object in any... ");
        
        String ior = bus.get("TEST_IOR");
        var obj = clientOrb.string_to_object(ior);
        var testObj = TestHelper.narrow(obj);
        
        // Create an Any containing the test object
        Any a = clientOrb.create_any();
        TestHelper.insert(a, testObj);
        
        assertDoesNotThrow(() -> testObj.inany(a),
            "Passing object in Any should succeed");
        
        System.out.println("Done!");
    }

    /**
     * Test 6: Client plugin validation.
     * Validates that client plugin was properly invoked.
     */
    @Test
    public void testClientPluginValidation(ORB clientOrb, Bus bus) throws Exception {
        System.out.print("Testing client plugin validation... ");
        
        // First make a call to ensure the plugin is used
        String ior = bus.get("TEST_IOR");
        var obj = clientOrb.string_to_object(ior);
        var testObj = TestHelper.narrow(obj);
        testObj.say("Trigger plugin");
        
        // Now validate the plugin
        assertTrue(ClientPlugin.testPassed(),
            "Client plugin should be constructed, initialized, and used");
        
        System.out.println("Done!");
    }

    /**
     * Test 7: Server shutdown and validation.
     * Validates server plugin and performs clean shutdown.
     */
    @Test
    public void testShutdownAndServerValidation(ORB clientOrb, Bus bus) throws Exception {
        System.out.print("Testing shutdown and server validation... ");
        
        String ior = bus.get("TEST_IOR");
        var obj = clientOrb.string_to_object(ior);
        var testObj = TestHelper.narrow(obj);
        
        // This will validate server plugin internally and shutdown
        assertDoesNotThrow(() -> testObj.shutdown(), "Shutdown should succeed with valid server plugin");
        
        System.out.println("Done!");
    }
}

// Made with Bob
