package org.omg.PortableInterceptor;

import acme.Echo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.omg.CORBA.BAD_INV_ORDER;
import org.omg.CORBA.NO_PERMISSION;
import testify.iiop.TestORBInitializer;
import testify.iiop.annotation.ConfigureOrb;
import testify.iiop.annotation.ConfigureServer;
import testify.iiop.annotation.ConfigureServer.RemoteImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.omg.CORBA.CompletionStatus.COMPLETED_YES;
import static testify.iiop.annotation.ConfigureOrb.UseWithOrb.InitializerScope.CLIENT;
import static testify.iiop.annotation.ConfigureOrb.UseWithOrb.InitializerScope.SERVER;

@ConfigureServer
public class ClientServerMethodsSupportTest {

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
    public static final Echo impl = ClientServerMethodsSupportTest::convertString;

    private static String convertString(String s) { return '#' + s + '#'; }

    @AfterEach
    void resetMocks() {
        // Reset all mocks after each test to ensure clean state
        Mockito.reset(CI1, CI2, CI3, SI);
    }

    @ConfigureOrb.UseWithOrb(scope = CLIENT)
    public static class ClientOrbInitializer implements TestORBInitializer {
        @Override
        public void pre_init(ORBInitInfo info) {
            assertDoesNotThrow(() -> info.add_client_request_interceptor(CI1));
            assertDoesNotThrow(() -> info.add_client_request_interceptor(CI2));
            assertDoesNotThrow(() -> info.add_client_request_interceptor(CI3));
        }
    }

    @ConfigureOrb.UseWithOrb(scope = SERVER)
    public static class ServerOrbInitializer implements TestORBInitializer {
        @Override
        public void pre_init(ORBInitInfo info) {
            assertDoesNotThrow(() -> info.add_server_request_interceptor(SI));
        }
    }

    /**
     * Comprehensive test of ClientRequestInfo method availability at different interception points.
     *
     * Tests what methods are supported at each interception point:
     * - send_request: Before the request is sent to the server
     * - receive_reply: After receiving a successful reply
     * - receive_exception: After receiving an exception
     *
     * Expected behaviors:
     * - Fully supported: Method succeeds and returns valid data
     * - BAD_INV_ORDER: Method is defined but not available at this point (error path covered)
     * - NO_RESOURCES or other: Method not supported at all
     */
    @Test
    void testClientRequestInfoMethodAvailabilityAtInterceptionPoints(Echo stub) throws Exception {
        final StringBuilder report = new StringBuilder();

        // Configure CI1 to test method availability at send_request
        Mockito.doAnswer(invocation -> {
            ClientRequestInfo ri = invocation.getArgument(0);
            report.append("\n=== send_request() interception point ===\n");
            testMethodAvailability(ri, report);
            return null;
        }).when(CI1).send_request(Mockito.any(ClientRequestInfo.class));

        // Configure CI1 to test method availability at receive_reply
        Mockito.doAnswer(invocation -> {
            ClientRequestInfo ri = invocation.getArgument(0);
            report.append("\n=== receive_reply() interception point ===\n");
            testMethodAvailability(ri, report);
            return null;
        }).when(CI1).receive_reply(Mockito.any(ClientRequestInfo.class));

        // Configure server to throw exception to test receive_exception
        Mockito.doAnswer(invocation -> {
            throw new NO_PERMISSION("Test exception for receive_exception", 0, COMPLETED_YES);
        }).when(SI).receive_request(Mockito.any(ServerRequestInfo.class));

        // Configure CI1 to test method availability at receive_exception
        Mockito.doAnswer(invocation -> {
            ClientRequestInfo ri = invocation.getArgument(0);
            report.append("\n=== receive_exception() interception point ===\n");
            testMethodAvailability(ri, report);
            return null;
        }).when(CI1).receive_exception(Mockito.any(ClientRequestInfo.class));

        // Execute the call
        try {
            stub.echo("test");
        } catch (Exception e) {
            // Expected - server throws exception
        }

        // Print the comprehensive report
        System.out.println("\n" + "=".repeat(80));
        System.out.println("CLIENT REQUEST INFO METHOD AVAILABILITY REPORT");
        System.out.println("=".repeat(80));
        System.out.println(report);
        System.out.println("=".repeat(80));

        // Verify that the report contains all interception points
        String reportStr = report.toString();
        assertTrue(reportStr.contains("send_request() interception point"), "Report should contain send_request section");
        assertTrue(reportStr.contains("receive_exception() interception point"), "Report should contain receive_exception section");
    }

    /**
     * Test ClientRequestInfo method availability at receive_reply interception point.
     *
     * This test complements testClientRequestInfoMethodAvailabilityAtInterceptionPoints
     * by testing the receive_reply interception point when the call succeeds.
     * Also tests send_poll and receive_other interception points defined in
     * ClientRequestInterceptorOperations.java.
     */
    @Test
    void testClientRequestInfoMethodAvailabilityAtReceiveReply(Echo stub) throws Exception {
        final StringBuilder report = new StringBuilder();

        // Configure CI1 to test method availability at send_request
        Mockito.doAnswer(invocation -> {
            ClientRequestInfo ri = invocation.getArgument(0);
            report.append("\n=== send_request() interception point ===\n");
            testMethodAvailability(ri, report);
            return null;
        }).when(CI1).send_request(Mockito.any(ClientRequestInfo.class));

        // Configure CI1 to test method availability at send_poll
        Mockito.doAnswer(invocation -> {
            ClientRequestInfo ri = invocation.getArgument(0);
            report.append("\n=== send_poll() interception point ===\n");
            testMethodAvailability(ri, report);
            return null;
        }).when(CI1).send_poll(Mockito.any(ClientRequestInfo.class));

        // Configure CI1 to test method availability at receive_reply
        Mockito.doAnswer(invocation -> {
            ClientRequestInfo ri = invocation.getArgument(0);
            report.append("\n=== receive_reply() interception point ===\n");
            testMethodAvailability(ri, report);
            return null;
        }).when(CI1).receive_reply(Mockito.any(ClientRequestInfo.class));

        // Configure CI1 to test method availability at receive_other
        Mockito.doAnswer(invocation -> {
            ClientRequestInfo ri = invocation.getArgument(0);
            report.append("\n=== receive_other() interception point ===\n");
            testMethodAvailability(ri, report);
            return null;
        }).when(CI1).receive_other(Mockito.any(ClientRequestInfo.class));

        // Execute a successful call - this will trigger send_request and receive_reply
        String result = stub.echo("test");
        assertEquals("#test#", result, "Echo should succeed");

        // Print the comprehensive report
        System.out.println("\n" + "=".repeat(80));
        System.out.println("CLIENT REQUEST INFO METHOD AVAILABILITY REPORT (SUCCESSFUL REPLY)");
        System.out.println("=".repeat(80));
        System.out.println(report.toString());
        System.out.println("=".repeat(80));

        // Verify that the report contains all interception points that were triggered
        String reportStr = report.toString();
        assertEquals(true, reportStr.contains("send_request() interception point"),
                "Report should contain send_request section");
        assertEquals(true, reportStr.contains("receive_reply() interception point"),
                "Report should contain receive_reply section");
        
        // Note: send_poll and receive_other may not be triggered in a normal synchronous call
        // but we've configured them to be tested if they are invoked
    }

    /**
     * Comprehensive test of ServerRequestInfo method availability at different interception points.
     *
     * Tests what methods are supported at each server-side interception point:
     * - receive_request_service_contexts: After receiving request but before unmarshaling
     * - receive_request: After unmarshaling arguments
     * - send_reply: Before sending successful reply
     * - send_exception: Before sending exception reply
     *
     * Expected behaviors:
     * - Fully supported: Method succeeds and returns valid data
     * - BAD_INV_ORDER: Method is defined but not available at this point (error path covered)
     * - NO_RESOURCES or other: Method not supported at all
     */
    @Test
    void testServerRequestInfoMethodAvailabilityAtInterceptionPoints(Echo stub) throws Exception {
        final StringBuilder report = new StringBuilder();

        // Configure SI to test method availability at receive_request_service_contexts
        Mockito.doAnswer(invocation -> {
            ServerRequestInfo ri = invocation.getArgument(0);
            report.append("\n=== receive_request_service_contexts() interception point ===\n");
            testServerMethodAvailability(ri, "receive_request_service_contexts", report);
            return null;
        }).when(SI).receive_request_service_contexts(Mockito.any(ServerRequestInfo.class));

        // Configure SI to test method availability at receive_request
        Mockito.doAnswer(invocation -> {
            ServerRequestInfo ri = invocation.getArgument(0);
            report.append("\n=== receive_request() interception point ===\n");
            testServerMethodAvailability(ri, "receive_request", report);
            return null;
        }).when(SI).receive_request(Mockito.any(ServerRequestInfo.class));

        // Configure SI to test method availability at send_reply
        Mockito.doAnswer(invocation -> {
            ServerRequestInfo ri = invocation.getArgument(0);
            report.append("\n=== send_reply() interception point ===\n");
            testServerMethodAvailability(ri, "send_reply", report);
            return null;
        }).when(SI).send_reply(Mockito.any(ServerRequestInfo.class));

        // Execute a successful call
        String result = stub.echo("test");
        assertEquals("#test#", result, "Echo should succeed");

        // Print the comprehensive report
        System.out.println("\n" + "=".repeat(80));
        System.out.println("SERVER REQUEST INFO METHOD AVAILABILITY REPORT (SUCCESSFUL CALL)");
        System.out.println("=".repeat(80));
        System.out.println(report.toString());
        System.out.println("=".repeat(80));

        // Verify that the report contains all interception points
        String reportStr = report.toString();
        assertEquals(true, reportStr.contains("receive_request_service_contexts() interception point"),
                "Report should contain receive_request_service_contexts section");
        assertEquals(true, reportStr.contains("receive_request() interception point"),
                "Report should contain receive_request section");
        assertEquals(true, reportStr.contains("send_reply() interception point"),
                "Report should contain send_reply section");
    }

    /**
     * Test ServerRequestInfo method availability at send_exception interception point.
     */
    @Test
    void testServerRequestInfoMethodAvailabilityAtSendException(Echo stub) throws Exception {
        final StringBuilder report = new StringBuilder();

        // Configure SI to test method availability at receive_request_service_contexts
        Mockito.doAnswer(invocation -> {
            ServerRequestInfo ri = invocation.getArgument(0);
            report.append("\n=== receive_request_service_contexts() interception point ===\n");
            testServerMethodAvailability(ri, "receive_request_service_contexts", report);
            return null;
        }).when(SI).receive_request_service_contexts(Mockito.any(ServerRequestInfo.class));

        // Configure SI to throw exception at receive_request
        Mockito.doAnswer(invocation -> {
            ServerRequestInfo ri = invocation.getArgument(0);
            report.append("\n=== receive_request() interception point ===\n");
            testServerMethodAvailability(ri, "receive_request", report);
            throw new NO_PERMISSION("Test exception from server", 0, COMPLETED_YES);
        }).when(SI).receive_request(Mockito.any(ServerRequestInfo.class));

        // Configure SI to test method availability at send_exception
        Mockito.doAnswer(invocation -> {
            ServerRequestInfo ri = invocation.getArgument(0);
            report.append("\n=== send_exception() interception point ===\n");
            testServerMethodAvailability(ri, "send_exception", report);
            return null;
        }).when(SI).send_exception(Mockito.any(ServerRequestInfo.class));

        // Execute the call - expect exception
        try {
            stub.echo("test");
        } catch (Exception e) {
            // Expected
        }

        // Print the comprehensive report
        System.out.println("\n" + "=".repeat(80));
        System.out.println("SERVER REQUEST INFO METHOD AVAILABILITY REPORT (EXCEPTION CASE)");
        System.out.println("=".repeat(80));
        System.out.println(report.toString());
        System.out.println("=".repeat(80));

        // Verify that the report contains all interception points
        String reportStr = report.toString();
        assertEquals(true, reportStr.contains("receive_request_service_contexts() interception point"),
                "Report should contain receive_request_service_contexts section");
        assertEquals(true, reportStr.contains("receive_request() interception point"),
                "Report should contain receive_request section");
        assertEquals(true, reportStr.contains("send_exception() interception point"),
                "Report should contain send_exception section");
    }

    /**
     * Helper method to test availability of ServerRequestInfo methods at a specific interception point.
     */
    private static void testServerMethodAvailability(ServerRequestInfo ri, String interceptionPoint, StringBuilder report) {
        // Test RequestInfo methods (inherited)
        testMethod(report, "request_id()", () -> {
            int id = ri.request_id();
            return "SUCCESS: " + id;
        });

        testMethod(report, "operation()", () -> {
            String op = ri.operation();
            return "SUCCESS: " + op;
        });

        testMethod(report, "arguments()", () -> {
            org.omg.Dynamic.Parameter[] args = ri.arguments();
            return "SUCCESS: " + args.length + " arguments";
        });

        testMethod(report, "exceptions()", () -> {
            org.omg.CORBA.TypeCode[] exs = ri.exceptions();
            return "SUCCESS: " + exs.length + " exceptions";
        });

        testMethod(report, "contexts()", () -> {
            String[] ctxs = ri.contexts();
            return "SUCCESS: " + ctxs.length + " contexts";
        });

        testMethod(report, "operation_context()", () -> {
            String[] opCtx = ri.operation_context();
            return "SUCCESS: " + opCtx.length + " operation contexts";
        });

        testMethod(report, "result()", () -> {
            org.omg.CORBA.Any res = ri.result();
            return "SUCCESS: result available";
        });

        testMethod(report, "response_expected()", () -> {
            boolean expected = ri.response_expected();
            return "SUCCESS: " + expected;
        });

        testMethod(report, "sync_scope()", () -> {
            short scope = ri.sync_scope();
            return "SUCCESS: " + scope;
        });

        testMethod(report, "reply_status()", () -> {
            short status = ri.reply_status();
            return "SUCCESS: " + status;
        });

        testMethod(report, "forward_reference()", () -> {
            org.omg.CORBA.Object ref = ri.forward_reference();
            return "SUCCESS: forward reference available";
        });

        testMethod(report, "get_slot(0)", () -> {
            org.omg.CORBA.Any slot = ri.get_slot(0);
            return "SUCCESS: slot available";
        });

        testMethod(report, "get_request_service_context(0)", () -> {
            org.omg.IOP.ServiceContext ctx = ri.get_request_service_context(0);
            return "SUCCESS: service context available";
        });

        testMethod(report, "get_reply_service_context(0)", () -> {
            org.omg.IOP.ServiceContext ctx = ri.get_reply_service_context(0);
            return "SUCCESS: reply service context available";
        });

        // Test ServerRequestInfo-specific methods
        testMethod(report, "sending_exception()", () -> {
            org.omg.CORBA.Any ex = ri.sending_exception();
            return "SUCCESS: sending exception available";
        });

        testMethod(report, "object_id()", () -> {
            byte[] id = ri.object_id();
            return "SUCCESS: object_id length=" + id.length;
        });

        testMethod(report, "adapter_id()", () -> {
            byte[] id = ri.adapter_id();
            return "SUCCESS: adapter_id length=" + id.length;
        });

        testMethod(report, "target_most_derived_interface()", () -> {
            String iface = ri.target_most_derived_interface();
            return "SUCCESS: " + iface;
        });

        testMethod(report, "server_id()", () -> {
            String id = ri.server_id();
            return "SUCCESS: " + id;
        });

        testMethod(report, "orb_id()", () -> {
            String id = ri.orb_id();
            return "SUCCESS: " + id;
        });

        testMethod(report, "adapter_name()", () -> {
            String[] name = ri.adapter_name();
            return "SUCCESS: " + name.length + " adapter name components";
        });

        testMethod(report, "get_server_policy(0)", () -> {
            org.omg.CORBA.Policy policy = ri.get_server_policy(0);
            return "SUCCESS: server policy available";
        });

        testMethod(report, "set_slot(0, any)", () -> {
            // Note: We can't easily create an Any here without access to the proper ORB
            // So we'll just test with a null Any to see if the method is callable
            try {
                ri.set_slot(0, null);
                return "SUCCESS: slot set (with null)";
            } catch (NullPointerException e) {
                // Expected - but at least we know the method is callable
                return "SUCCESS: method callable (NPE on null Any is expected)";
            }
        });

        testMethod(report, "target_is_a(\"IDL:acme/Echo:1.0\")", () -> {
            boolean result = ri.target_is_a("IDL:acme/Echo:1.0");
            return "SUCCESS: " + result;
        });

        testMethod(report, "add_reply_service_context()", () -> {
            org.omg.IOP.ServiceContext ctx = new org.omg.IOP.ServiceContext(999, new byte[]{1, 2, 3});
            ri.add_reply_service_context(ctx, false);
            return "SUCCESS: reply service context added";
        });
    }

    /**
     * Helper method to test availability of ClientRequestInfo methods at a specific interception point.
     */
    private static void testMethodAvailability(ClientRequestInfo ri, StringBuilder report) {
        // Test RequestInfo methods (inherited)
        testMethod(report, "request_id()", () -> {
            int id = ri.request_id();
            return "SUCCESS: " + id;
        });

        testMethod(report, "operation()", () -> {
            String op = ri.operation();
            return "SUCCESS: " + op;
        });

        testMethod(report, "arguments()", () -> {
            org.omg.Dynamic.Parameter[] args = ri.arguments();
            return "SUCCESS: " + args.length + " arguments";
        });

        testMethod(report, "exceptions()", () -> {
            org.omg.CORBA.TypeCode[] exs = ri.exceptions();
            return "SUCCESS: " + exs.length + " exceptions";
        });

        testMethod(report, "contexts()", () -> {
            String[] ctxs = ri.contexts();
            return "SUCCESS: " + ctxs.length + " contexts";
        });

        testMethod(report, "operation_context()", () -> {
            String[] opCtx = ri.operation_context();
            return "SUCCESS: " + opCtx.length + " operation contexts";
        });

        testMethod(report, "result()", () -> {
            org.omg.CORBA.Any res = ri.result();
            return "SUCCESS: result available";
        });

        testMethod(report, "response_expected()", () -> {
            boolean expected = ri.response_expected();
            return "SUCCESS: " + expected;
        });

        testMethod(report, "sync_scope()", () -> {
            short scope = ri.sync_scope();
            return "SUCCESS: " + scope;
        });

        testMethod(report, "reply_status()", () -> {
            short status = ri.reply_status();
            return "SUCCESS: " + status;
        });

        testMethod(report, "forward_reference()", () -> {
            org.omg.CORBA.Object ref = ri.forward_reference();
            return "SUCCESS: forward reference available";
        });

        testMethod(report, "get_slot(0)", () -> {
            org.omg.CORBA.Any slot = ri.get_slot(0);
            return "SUCCESS: slot available";
        });

        testMethod(report, "get_request_service_context(0)", () -> {
            org.omg.IOP.ServiceContext ctx = ri.get_request_service_context(0);
            return "SUCCESS: service context available";
        });

        testMethod(report, "get_reply_service_context(0)", () -> {
            org.omg.IOP.ServiceContext ctx = ri.get_reply_service_context(0);
            return "SUCCESS: reply service context available";
        });

        // Test ClientRequestInfo-specific methods
        testMethod(report, "target()", () -> {
            org.omg.CORBA.Object target = ri.target();
            return "SUCCESS: target available";
        });

        testMethod(report, "effective_target()", () -> {
            org.omg.CORBA.Object target = ri.effective_target();
            return "SUCCESS: effective target available";
        });

        testMethod(report, "effective_profile()", () -> {
            org.omg.IOP.TaggedProfile profile = ri.effective_profile();
            return "SUCCESS: effective profile available";
        });

        testMethod(report, "received_exception()", () -> {
            org.omg.CORBA.Any ex = ri.received_exception();
            return "SUCCESS: received exception available";
        });

        testMethod(report, "received_exception_id()", () -> {
            String id = ri.received_exception_id();
            return "SUCCESS: " + id;
        });

        testMethod(report, "get_effective_component(0)", () -> {
            org.omg.IOP.TaggedComponent comp = ri.get_effective_component(0);
            return "SUCCESS: effective component available";
        });

        testMethod(report, "get_effective_components(0)", () -> {
            org.omg.IOP.TaggedComponent[] comps = ri.get_effective_components(0);
            return "SUCCESS: " + comps.length + " effective components";
        });

        testMethod(report, "get_request_policy(0)", () -> {
            org.omg.CORBA.Policy policy = ri.get_request_policy(0);
            return "SUCCESS: request policy available";
        });

        testMethod(report, "add_request_service_context()", () -> {
            // Create a dummy service context
            org.omg.IOP.ServiceContext ctx = new org.omg.IOP.ServiceContext(999, new byte[]{1, 2, 3});
            ri.add_request_service_context(ctx, false);
            return "SUCCESS: service context added";
        });
    }

    /**
     * Helper to test a single method and categorize the result.
     */
    private static void testMethod(StringBuilder report, String methodName, ThrowingSupplier<String> test) {
        try {
            String result = test.get();
            report.append(String.format("  ✓ %-40s %s\n", methodName, result));
        } catch (BAD_INV_ORDER e) {
            report.append(String.format("  ⚠ %-40s BAD_INV_ORDER (supported with BAD_INV_ORDER at this point)\n", methodName));
        } catch (org.omg.CORBA.NO_RESOURCES e) {
            if (e.getMessage() != null && e.getMessage().contains("arguments unavailable")) {
                report.append(String.format("  ✗ %-40s NO_RESOURCES: %s\n", methodName, "NOT SUPPORTED - arguments unavailable"));
            } else {
                report.append(String.format("  ✗ %-40s NO_RESOURCES: %s\n", methodName, e.getMessage()));
            }
        } catch (Exception e) {
            report.append(String.format("  ✗ %-40s %s: %s\n", methodName, e.getClass().getSimpleName(), e.getMessage()));
        }
    }

    @FunctionalInterface
    interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}
