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
import static test.tnaming.NameServiceChecks.checkWritesAreRejected;
import static test.tnaming.NameServiceChecks.createBindingsOverWhichToIterate;
import static test.tnaming.NameServiceChecks.narrow;
import static test.tnaming.NameServiceChecks.publishTestObjects;
import static testify.iiop.annotation.ConfigureOrb.NameService.READ_ONLY;

/**
 * Test a name service that is integral to the server ORB and that refuses remote modification.
 * The server populates the name service through its local reference, which is always writable.
 */
@ConfigureServer(serverOrb = @ConfigureOrb(nameService = READ_ONLY))
public class ReadOnlyNameServiceTest {
    @NameServiceStub
    public static NamingContext nameService;

    @BeforeServer
    public static void setUp(ORB serverOrb, Bus bus) throws Exception {
        NamingContextExt root = narrow(serverOrb.resolve_initial_references("NameService"));
        publishTestObjects(serverOrb, root, bus);
        createBindingsOverWhichToIterate(serverOrb, root);
        bindObjectFactories(serverOrb, root);
    }

    @Test
    void testWritesAreRejected() {
        checkWritesAreRejected(narrow(nameService));
    }

    @Test
    void testBoundReferences(ORB clientOrb, Bus bus) throws Exception {
        checkBoundReferences(clientOrb, narrow(nameService), bus);
    }

    @Test
    void testIterators() throws Exception {
        checkIterators(narrow(nameService));
    }

    @Test
    void testObjectFactories() throws Exception {
        checkObjectFactories(narrow(nameService));
    }
}
