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
package org.apache.yoko.orb.CosNaming;

import org.junit.jupiter.api.Test;
import org.omg.CORBA.ORB;
import org.omg.CosNaming.NamingContext;
import org.omg.CosNaming.NamingContextExt;
import testify.bus.Bus;
import testify.iiop.annotation.ConfigureOrb;
import testify.iiop.annotation.ConfigureServer;
import testify.iiop.annotation.ConfigureServer.BeforeServer;
import testify.iiop.annotation.ConfigureServer.NameServiceStub;

import static test.tnaming.NameServiceChecks.bindObjectFactories;
import static test.tnaming.NameServiceChecks.checkBoundReferences;
import static test.tnaming.NameServiceChecks.checkIterators;
import static test.tnaming.NameServiceChecks.checkObjectFactories;
import static test.tnaming.NameServiceChecks.createBindingsOverWhichToIterate;
import static test.tnaming.NameServiceChecks.narrow;
import static test.tnaming.NameServiceChecks.publishTestObjects;
import static test.tnaming.NameServiceChecks.removeBindingsOverWhichToIterate;
import static testify.iiop.annotation.ConfigureOrb.NameService.READ_WRITE;

/**
 * Test a name service that is integral to the server ORB and that allows remote modification.
 * The client creates the bindings that the iterator test lists, so they are hosted by the client ORB.
 */
@ConfigureServer(serverOrb = @ConfigureOrb(nameService = READ_WRITE))
public class ReadWriteNameServiceTest {
    @NameServiceStub
    public static NamingContext nameService;

    @BeforeServer
    public static void setUp(ORB serverOrb, Bus bus) throws Exception {
        NamingContextExt root = narrow(serverOrb.resolve_initial_references("NameService"));
        publishTestObjects(serverOrb, root, bus);
        bindObjectFactories(serverOrb, root);
    }

    @Test
    void testBoundReferences(ORB clientOrb, Bus bus) throws Exception {
        checkBoundReferences(clientOrb, narrow(nameService), bus);
    }

    @Test
    void testIterators(ORB clientOrb) throws Exception {
        NamingContextExt root = narrow(nameService);
        createBindingsOverWhichToIterate(clientOrb, root);
        try {
            checkIterators(root);
        } finally {
            removeBindingsOverWhichToIterate(root);
        }
    }

    @Test
    void testObjectFactories() throws Exception {
        checkObjectFactories(narrow(nameService));
    }
}
