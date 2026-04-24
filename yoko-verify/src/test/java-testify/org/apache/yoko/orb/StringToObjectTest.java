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
package org.apache.yoko.orb;

import acme.Echo;
import org.junit.jupiter.api.Test;
import org.omg.CORBA.ORB;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContext;
import org.omg.CosNaming.NamingContextHelper;
import org.omg.PortableInterceptor.ClientRequestInfo;
import testify.annotation.Logging;
import testify.annotation.RetriedTest;
import testify.iiop.TestClientRequestInterceptor;
import testify.iiop.annotation.ConfigureOrb;
import testify.iiop.annotation.ConfigureOrb.UseWithOrb;
import testify.iiop.annotation.ConfigureServer;
import testify.iiop.annotation.ConfigureServer.BeforeServer;
import testify.iiop.annotation.ServerControl;

import javax.rmi.PortableRemoteObject;
import java.net.URLEncoder;

import static testify.iiop.annotation.ConfigureOrb.NameService.READ_WRITE;
import static testify.iiop.annotation.ConfigureOrb.UseWithOrb.InitializerScope.CLIENT;

@ConfigureServer(serverOrb = @ConfigureOrb(nameService = READ_WRITE))
public class StringToObjectTest {
    private static final String MY_CONTEXT = "my_context";
    private static final String MY_ENTRY = "na\u00efvet\u00e9"; // "naivete" with i diaeresis and e acute
    private static final NameComponent[] MY_CONTEXT_NC = {new NameComponent(MY_CONTEXT, "")};
    private static final NameComponent[] MY_ENTRY_NC = {new NameComponent(MY_ENTRY, "")};

    @ConfigureServer.NameServiceUrl
    public static String nameServiceUrl;

    @ConfigureServer.Control
    public static ServerControl serverControl;

    @ConfigureServer.RemoteImpl
    public static final Echo impl = s -> s;

    @ConfigureServer.RemoteStub
    public static Echo stub;

    /** Traces request flows from the client ORB perspective */
    @UseWithOrb(scope = CLIENT)
    public static class ClientInterceptor implements TestClientRequestInterceptor {
        public void send_request(ClientRequestInfo ri) { System.out.println("### client interceptor send_request op=" + ri.operation()); }
        public void send_poll(ClientRequestInfo ri) { System.out.println("### client interceptor send_poll op=" + ri.operation()); }
        public void receive_reply(ClientRequestInfo ri) { System.out.println("### client interceptor receive_reply op=" + ri.operation()); }
        public void receive_exception(ClientRequestInfo ri) { System.out.println("### client interceptor receive_exception op=" + ri.operation() + " ex=" + ri.received_exception_id()); }
        public void receive_other(ClientRequestInfo ri) { System.out.println("### client interceptor receive_other op=" + ri.operation()); }
    }

    @BeforeServer
    public static void bindContext(ORB orb) throws Exception {
        System.out.println("### binding context");
        NamingContext ctx = (NamingContext) orb.resolve_initial_references("NameService");
        ctx.bind_new_context(MY_CONTEXT_NC);
    }

    private static void destroy(ORB orb) {
        orb.shutdown(true);
        orb.destroy();
    }

    @RetriedTest(maxRuns = 10)
    public void testRestartServer(ORB orb) throws Exception {
        String url = nameServiceUrl + "#" + MY_CONTEXT;
        System.out.println("### url = " + url);
        orb.string_to_object(url);
        serverControl.restart();
        orb.string_to_object(url);
    }

    @Test
    @Logging
    public void testLatinChars(ORB orb) throws Exception {
        String url = nameServiceUrl + "#" + MY_CONTEXT;
        NamingContext ctx = NamingContextHelper.narrow(orb.string_to_object(url));
        ctx.bind(MY_ENTRY_NC, (org.omg.CORBA.Object)PortableRemoteObject.toStub(stub));
        String corbaname = nameServiceUrl + "#" + MY_CONTEXT + "/" + MY_ENTRY;
        System.out.println("### corbaname = " + URLEncoder.encode(corbaname));
        Object obj = orb.string_to_object(corbaname);
        Echo stub2 = (Echo) PortableRemoteObject.narrow(obj, Echo.class);
        String s = stub2.echo(corbaname);
        System.out.println("### echoed corbaname = " + s);
    }
}
