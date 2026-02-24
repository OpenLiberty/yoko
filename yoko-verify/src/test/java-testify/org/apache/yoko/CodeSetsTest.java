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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.omg.CORBA.ORB;
import test.codesets.TestCodeSets;
import test.codesets.TestCodeSetsHelper;
import test.codesets.TestCodeSets_impl;
import testify.bus.Bus;
import testify.bus.key.TypeKey;
import testify.iiop.annotation.ConfigureOrb;
import testify.iiop.annotation.ConfigureServer;
import testify.iiop.annotation.ConfigureServer.BeforeServer;

import static org.apache.yoko.CodeSetsTest.PublishStub.REF;

@ConfigureServer
public class CodeSetsTest {
    // same latin codeset on server and client
    @ConfigureServer (clientOrb = @ConfigureOrb(args = {"-ORBnative_cs", "8859-1"}), serverOrb = @ConfigureOrb(args = {"-ORBnative_cs", "8859-1"})) public static class CodeSet1Test extends CodeSetsTest {}
    @ConfigureServer (clientOrb = @ConfigureOrb(args = {"-ORBnative_cs", "8859-2"}), serverOrb = @ConfigureOrb(args = {"-ORBnative_cs", "8859-2"})) public static class CodeSet2Test extends CodeSetsTest {}
    @ConfigureServer (clientOrb = @ConfigureOrb(args = {"-ORBnative_cs", "8859-3"}), serverOrb = @ConfigureOrb(args = {"-ORBnative_cs", "8859-3"})) public static class CodeSet3Test extends CodeSetsTest {}
    @ConfigureServer (clientOrb = @ConfigureOrb(args = {"-ORBnative_cs", "8859-4"}), serverOrb = @ConfigureOrb(args = {"-ORBnative_cs", "8859-4"})) public static class CodeSet4Test extends CodeSetsTest {}
    @ConfigureServer (clientOrb = @ConfigureOrb(args = {"-ORBnative_cs", "8859-5"}), serverOrb = @ConfigureOrb(args = {"-ORBnative_cs", "8859-5"})) public static class CodeSet5Test extends CodeSetsTest {}
    @ConfigureServer (clientOrb = @ConfigureOrb(args = {"-ORBnative_cs", "8859-6"}), serverOrb = @ConfigureOrb(args = {"-ORBnative_cs", "8859-6"})) public static class CodeSet6Test extends CodeSetsTest {}
    @ConfigureServer (clientOrb = @ConfigureOrb(args = {"-ORBnative_cs", "8859-7"}), serverOrb = @ConfigureOrb(args = {"-ORBnative_cs", "8859-7"})) public static class CodeSet7Test extends CodeSetsTest {}
    @ConfigureServer (clientOrb = @ConfigureOrb(args = {"-ORBnative_cs", "8859-8"}), serverOrb = @ConfigureOrb(args = {"-ORBnative_cs", "8859-8"})) public static class CodeSet8Test extends CodeSetsTest {}
    @ConfigureServer (clientOrb = @ConfigureOrb(args = {"-ORBnative_cs", "8859-9"}), serverOrb = @ConfigureOrb(args = {"-ORBnative_cs", "8859-9"})) public static class CodeSet9Test extends CodeSetsTest {}
    // different latin codesets on server and client
    @ConfigureServer (clientOrb = @ConfigureOrb(args = {"-ORBnative_cs", "8859-1"}), serverOrb = @ConfigureOrb(args = {"-ORBnative_cs", "8859-2"})) public static class CodeSets12Test extends CodeSetsTest {}
    @ConfigureServer (clientOrb = @ConfigureOrb(args = {"-ORBnative_cs", "8859-2"}), serverOrb = @ConfigureOrb(args = {"-ORBnative_cs", "8859-3"})) public static class CodeSets23Test extends CodeSetsTest {}
    @ConfigureServer (clientOrb = @ConfigureOrb(args = {"-ORBnative_cs", "8859-3"}), serverOrb = @ConfigureOrb(args = {"-ORBnative_cs", "8859-4"})) public static class CodeSets34Test extends CodeSetsTest {}
    @ConfigureServer (clientOrb = @ConfigureOrb(args = {"-ORBnative_cs", "8859-4"}), serverOrb = @ConfigureOrb(args = {"-ORBnative_cs", "8859-5"})) public static class CodeSets45Test extends CodeSetsTest {}
    @ConfigureServer (clientOrb = @ConfigureOrb(args = {"-ORBnative_cs", "8859-5"}), serverOrb = @ConfigureOrb(args = {"-ORBnative_cs", "8859-6"})) public static class CodeSets56Test extends CodeSetsTest {}
    @ConfigureServer (clientOrb = @ConfigureOrb(args = {"-ORBnative_cs", "8859-6"}), serverOrb = @ConfigureOrb(args = {"-ORBnative_cs", "8859-7"})) public static class CodeSets67Test extends CodeSetsTest {}
    @ConfigureServer (clientOrb = @ConfigureOrb(args = {"-ORBnative_cs", "8859-7"}), serverOrb = @ConfigureOrb(args = {"-ORBnative_cs", "8859-8"})) public static class CodeSets78Test extends CodeSetsTest {}
    @ConfigureServer (clientOrb = @ConfigureOrb(args = {"-ORBnative_cs", "8859-8"}), serverOrb = @ConfigureOrb(args = {"-ORBnative_cs", "8859-9"})) public static class CodeSets89Test extends CodeSetsTest {}
    @ConfigureServer (clientOrb = @ConfigureOrb(args = {"-ORBnative_cs", "8859-9"}), serverOrb = @ConfigureOrb(args = {"-ORBnative_cs", "8859-1"})) public static class CodeSets91Test extends CodeSetsTest {}
    // server uses default (UTF-8)
    @ConfigureServer (clientOrb = @ConfigureOrb(args = {"-ORBnative_cs", "8859-1"})) public static class CodeSets1DTest extends CodeSetsTest {}
    @ConfigureServer (clientOrb = @ConfigureOrb(args = {"-ORBnative_cs", "8859-2"})) public static class CodeSets2DTest extends CodeSetsTest {}
    @ConfigureServer (clientOrb = @ConfigureOrb(args = {"-ORBnative_cs", "8859-3"})) public static class CodeSets3DTest extends CodeSetsTest {}
    @ConfigureServer (clientOrb = @ConfigureOrb(args = {"-ORBnative_cs", "8859-4"})) public static class CodeSets4DTest extends CodeSetsTest {}
    @ConfigureServer (clientOrb = @ConfigureOrb(args = {"-ORBnative_cs", "8859-5"})) public static class CodeSets5DTest extends CodeSetsTest {}
    @ConfigureServer (clientOrb = @ConfigureOrb(args = {"-ORBnative_cs", "8859-6"})) public static class CodeSets6DTest extends CodeSetsTest {}
    @ConfigureServer (clientOrb = @ConfigureOrb(args = {"-ORBnative_cs", "8859-7"})) public static class CodeSets7DTest extends CodeSetsTest {}
    @ConfigureServer (clientOrb = @ConfigureOrb(args = {"-ORBnative_cs", "8859-8"})) public static class CodeSets8DTest extends CodeSetsTest {}
    @ConfigureServer (clientOrb = @ConfigureOrb(args = {"-ORBnative_cs", "8859-9"})) public static class CodeSets9DTest extends CodeSetsTest {}
    // client uses default (UTF-8)
    @ConfigureServer (serverOrb = @ConfigureOrb(args = {"-ORBnative_cs", "8859-1"})) public static class CodeSetsD1Test extends CodeSetsTest {}
    @ConfigureServer (serverOrb = @ConfigureOrb(args = {"-ORBnative_cs", "8859-2"})) public static class CodeSetsD2Test extends CodeSetsTest {}
    @ConfigureServer (serverOrb = @ConfigureOrb(args = {"-ORBnative_cs", "8859-3"})) public static class CodeSetsD3Test extends CodeSetsTest {}
    @ConfigureServer (serverOrb = @ConfigureOrb(args = {"-ORBnative_cs", "8859-4"})) public static class CodeSetsD4Test extends CodeSetsTest {}
    @ConfigureServer (serverOrb = @ConfigureOrb(args = {"-ORBnative_cs", "8859-5"})) public static class CodeSetsD5Test extends CodeSetsTest {}
    @ConfigureServer (serverOrb = @ConfigureOrb(args = {"-ORBnative_cs", "8859-6"})) public static class CodeSetsD6Test extends CodeSetsTest {}
    @ConfigureServer (serverOrb = @ConfigureOrb(args = {"-ORBnative_cs", "8859-7"})) public static class CodeSetsD7Test extends CodeSetsTest {}
    @ConfigureServer (serverOrb = @ConfigureOrb(args = {"-ORBnative_cs", "8859-8"})) public static class CodeSetsD8Test extends CodeSetsTest {}
    @ConfigureServer (serverOrb = @ConfigureOrb(args = {"-ORBnative_cs", "8859-9"})) public static class CodeSetsD9Test extends CodeSetsTest {}

    enum PublishStub implements TypeKey<String> {REF}
    private static TestCodeSets stub;

    @BeforeServer
    public static void exportObject(ORB orb, Bus bus) {
        TestCodeSets_impl impl = new TestCodeSets_impl(null);
        TestCodeSets tcs = impl._this(orb);
        String ior = orb.object_to_string(tcs);
        bus.put(REF, ior);
    }

    @BeforeAll
    public static void retrieveStub(ORB orb, Bus bus) {
        stub = TestCodeSetsHelper.narrow(orb.string_to_object(bus.get(REF)));
    }

    @Test void testChar() { Assertions.assertEquals('a', stub.testChar('a')); }
    @Test void testString() { Assertions.assertEquals("Hello, world", stub.testString("Hello, world")); }
    @Test void testWchar() { Assertions.assertEquals((char) 0x1234, stub.testWChar((char) 0x1234)); }
    @Test void testWstring() { Assertions.assertEquals("Hello, world", stub.testWString("Hello, world")); }
}
