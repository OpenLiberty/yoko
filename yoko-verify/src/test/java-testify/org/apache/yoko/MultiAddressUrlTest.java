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
package org.apache.yoko;

import org.junit.jupiter.api.Test;
import org.omg.CORBA.NO_RESPONSE;
import org.omg.CORBA.ORB;
import org.omg.CORBA.TRANSIENT;
import org.omg.CosNaming.NamingContext;
import testify.iiop.annotation.ConfigureOrb;
import testify.iiop.annotation.ConfigureOrb.NameService;
import testify.iiop.annotation.ConfigureServer;
import testify.iiop.annotation.ConfigureServer.Control;
import testify.iiop.annotation.ConfigureServer.NameServiceUrl;
import testify.iiop.annotation.ServerControl;

import java.lang.reflect.InvocationTargetException;
import java.net.ConnectException;

import static javax.rmi.PortableRemoteObject.narrow;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static testify.util.Assertions.assertThrowsExactly;

@ConfigureServer(
        clientOrb = @ConfigureOrb(props = "yoko.orb.policy.connect_timeout=10"),
        serverOrb = @ConfigureOrb(nameService = NameService.READ_ONLY)
)
public class MultiAddressUrlTest {
    // port 47 is reserved and should always refuse a connection
    static final String NO_LISTENER_NS_URL = "corbaname:iiop:127.0.0.1:47";
    // IP 192.0.2.1 is a reserved IP address and nothing should be reachable at that address.
    static final String UNREACHABLE_NS_URL = "corbaname:iiop:192.0.2.1:47";

    @NameServiceUrl
    public static String nsUrl;

    @Control
    public static ServerControl control;

    @Test
    public void testNameServiceUrl() {
        System.out.println(nsUrl);
        assertNotNull(nsUrl);
    }

    @Test
    public void testServerStopped(ORB orb) {
        narrow(orb.string_to_object(nsUrl), NamingContext.class);
        // stop the server
        control.stop();
        assertThrowsExactly(ClassCastException.class, () -> narrow(orb.string_to_object(nsUrl), NamingContext.class), InvocationTargetException.class, TRANSIENT.class, ConnectException.class);
        control.start();
    }

    @Test
    public void testConnectionRefused(ORB orb) throws Exception {
        // first show that port 47 is not listening
        assertThrowsExactly(ClassCastException.class, () -> narrow(orb.string_to_object(NO_LISTENER_NS_URL), NamingContext.class), InvocationTargetException.class, TRANSIENT.class, ConnectException.class);
        // check our nsURL starts with corbaname:
        assertThat(nsUrl, startsWith("corbaname:"));
        // construct a URL where the first server is stopped (connection refused)
        String url = NO_LISTENER_NS_URL + "," + nsUrl.substring("corbaname:".length());
        // try and get the name service
        narrow(orb.string_to_object(url), NamingContext.class);
    }

    @Test
    public void testHostUnreachable(ORB orb) throws Exception {
        // first show that port 47 is not listening
        assertThrowsExactly(ClassCastException.class, () -> narrow(orb.string_to_object(UNREACHABLE_NS_URL), NamingContext.class), InvocationTargetException.class, NO_RESPONSE.class);
        // check our nsURL starts with corbaname:
        assertThat(nsUrl, startsWith("corbaname:"));
        // construct a URL where the first server is stopped (connection refused)
        String url = UNREACHABLE_NS_URL + "," + nsUrl.substring("corbaname:".length());
        // try and get the name service
        narrow(orb.string_to_object(url), NamingContext.class);
    }
}
