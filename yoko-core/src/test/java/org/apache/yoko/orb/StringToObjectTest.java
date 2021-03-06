/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.yoko.orb;

import org.apache.yoko.orb.OBPortableServer.POAManager_impl;
import org.apache.yoko.orb.OCI.Acceptor;
import org.apache.yoko.orb.OCI.IIOP.AcceptorInfo;
import org.apache.yoko.orb.spi.naming.NameServiceInitializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omg.CORBA.LocalObject;
import org.omg.CORBA.ORB;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContext;
import org.omg.PortableInterceptor.ClientRequestInfo;
import org.omg.PortableInterceptor.ORBInitInfo;
import org.omg.PortableInterceptor.ORBInitializer;
import org.omg.PortableServer.POA;
import testify.bus.Bus;
import testify.bus.StringRef;
import testify.bus.TypeRef;
import testify.bus.VoidRef;
import testify.iiop.TestClientRequestInterceptor;
import testify.iiop.TestServerRequestInterceptor;
import testify.jupiter.annotation.ConfigurePartRunner;
import testify.jupiter.annotation.iiop.ConfigureOrb;
import testify.jupiter.annotation.iiop.ConfigureOrb.UseWithOrb;
import testify.parts.PartRunner;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ConfigurePartRunner
@ConfigureOrb
public class StringToObjectTest {
    final static String[] NO_ARGS = {};
    public static final String MY_CONTEXT = "MyContext";
    public static final NameComponent[] MY_CONTEXT_NC = {new NameComponent(MY_CONTEXT, "")};
    String url;

    @UseWithOrb
    public static class ClientInterceptor extends LocalObject implements TestClientRequestInterceptor {
        public void send_request(ClientRequestInfo ri) { System.out.println("### client interceptor send_request op=" + ri.operation()); }
        public void send_poll(ClientRequestInfo ri) { System.out.println("### client interceptor send_poll op=" + ri.operation()); }
        public void receive_reply(ClientRequestInfo ri) { System.out.println("### client interceptor receive_reply op=" + ri.operation()); }
        public void receive_exception(ClientRequestInfo ri) { System.out.println("### client interceptor receive_exception op=" + ri.operation() + " ex=" + ri.received_exception_id()); }
        public void receive_other(ClientRequestInfo ri) { System.out.println("### client interceptor receive_other op=" + ri.operation()); }
    }

    @BeforeEach
    public void setup(PartRunner runner) throws Exception {
        runner.useNewJVMWhenForking();
        runner.fork("server", StringToObjectTest::runServer);
        runner.bus("server").get(ServerEvent.STARTED);
        Integer port = runner.bus("server").get(Port.NUMBER);
        String host = runner.bus("server").get(Host.NAME);
        assertNotNull(port);
        url = "corbaname::" + host + ":" + port + "/NameService#" + MY_CONTEXT;
    }

    @AfterEach
    public void stopServer(PartRunner runner) throws Exception {
        runner.bus("server")
                .put(ServerEvent.STOP)
                .get(ServerEvent.STOPPED);
    }

    enum Port implements TypeRef<Integer> {NUMBER}
    enum Host implements StringRef {NAME}
    enum ServerEvent implements VoidRef {STARTED, RESTART_ORB, ORB_RESTARTED, STOP, STOPPED}

    private static void runServer(Bus bus) throws Exception {
        Properties props = new Properties();
        props.setProperty(ORBInitializer.class.getName() + "Class." + NameServiceInitializer.class.getName(), "");
        ORB orb = ORB.init(NO_ARGS, props);
        NamingContext ctx = (NamingContext) orb.resolve_initial_references("NameService");
        ctx.bind_new_context(MY_CONTEXT_NC);
        POA poa = (POA) orb.resolve_initial_references("RootPOA");
        POAManager_impl pm = (POAManager_impl)poa.the_POAManager();
        pm.activate();
        final Acceptor[] acceptors = pm.get_acceptors();
        final Acceptor acceptor = acceptors[0];
        final AcceptorInfo info = (AcceptorInfo) acceptor.get_info();
        // retrieve, save, and publish the host and port number
        Integer port = info.port() & 0xFFFF;
        bus.put(Port.NUMBER, port);
        props.setProperty("yoko.iiop.port", "" + port);
        String host = info.hosts()[0];
        bus.put(Host.NAME, host);
        props.setProperty("yoko.iiop.host", host);
        System.out.printf("Server listening on host %s and port %d%n", host, port);
        // let the client know the server is ready
        bus.put(ServerEvent.STARTED);
        // wait for next event
        bus.get(ServerEvent.RESTART_ORB);
        destroy(orb);
        orb = ORB.init(NO_ARGS, props);
        ctx = (NamingContext) orb.resolve_initial_references("NameService");
        ctx.bind_new_context(MY_CONTEXT_NC);
        poa = (POA) orb.resolve_initial_references("RootPOA");
        pm = (POAManager_impl)poa.the_POAManager();
        pm.activate();
        // let the client know the new ORB is ready
        bus.put(ServerEvent.ORB_RESTARTED);
        // wait for the final shutdown request
        Thread.sleep(2000);
        bus.get(ServerEvent.STOP);
        destroy(orb);
        bus.put(ServerEvent.STOPPED);
    }

    private static void destroy(ORB orb) {
        orb.shutdown(true);
        orb.destroy();
    }

    @Test
    public void testRestartServer(PartRunner runner, ORB orb) throws Exception {
        orb.string_to_object(url);
        runner.bus("server").put(ServerEvent.RESTART_ORB);
        runner.bus("server").get(ServerEvent.ORB_RESTARTED);
        orb.string_to_object(url);
    }
}
