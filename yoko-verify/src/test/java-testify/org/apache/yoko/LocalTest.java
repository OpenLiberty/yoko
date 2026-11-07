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

import org.junit.jupiter.api.Test;
import org.omg.CORBA.Any;
import org.omg.CORBA.AnyHolder;
import org.omg.CORBA.MARSHAL;
import org.omg.CORBA.ORB;
import org.omg.CORBA.UserException;
import org.omg.IOP.Codec;
import org.omg.IOP.CodecFactory;
import org.omg.IOP.CodecFactoryHelper;
import org.omg.IOP.ENCODING_CDR_ENCAPS;
import org.omg.IOP.Encoding;
import test.local.LocalTest_impl;
import test.local.TestHelper;
import test.local.Test_impl;
import testify.bus.Bus;
import testify.iiop.annotation.ConfigureServer;
import testify.iiop.annotation.ConfigureServer.BeforeServer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verify that local objects (those that can only be used in the process that created them)
 * are rejected when an attempt is made to marshal them, and that remote objects are not.
 */
@ConfigureServer
public class LocalTest {
    private static final String REMOTE_OBJECT = "test";

    @BeforeServer
    public static void setUp(ORB orb, Bus bus) {
        // Passing a null POA makes the servant use the ORB's root POA, which allows implicit activation.
        // (The POA that testify injects is a child POA with NO_IMPLICIT_ACTIVATION.)
        bus.put(REMOTE_OBJECT, orb.object_to_string(new Test_impl(null)._this(orb)));
    }

    private static test.local.Test remote(ORB orb, Bus bus) {
        test.local.Test remote = TestHelper.narrow(orb.string_to_object(bus.get(REMOTE_OBJECT)));
        assertNotNull(remote);
        return remote;
    }

    private static Codec codec(ORB orb) throws UserException {
        CodecFactory factory = CodecFactoryHelper.narrow(orb.resolve_initial_references("CodecFactory"));
        assertNotNull(factory);
        Codec codec = factory.create_codec(new Encoding(ENCODING_CDR_ENCAPS.value, (byte) 0, (byte) 0));
        assertNotNull(codec);
        return codec;
    }

    @Test
    void codecEncodesRemoteObject(ORB orb, Bus bus) throws Exception {
        Codec codec = codec(orb);
        Any any = orb.create_any();
        TestHelper.insert(any, remote(orb, bus));
        assertDoesNotThrow(() -> codec.encode_value(any));
    }

    @Test
    void codecRejectsLocalObject(ORB orb) throws Exception {
        Codec codec = codec(orb);
        Any any = orb.create_any();
        TestHelper.insert(any, new LocalTest_impl());
        assertThrows(MARSHAL.class, () -> codec.encode_value(any));
    }

    @Test
    void simpleRpcCall(ORB orb, Bus bus) {
        assertDoesNotThrow(() -> remote(orb, bus).say("Hi"));
    }

    @Test
    void passRemoteObject(ORB orb, Bus bus) {
        test.local.Test remote = remote(orb, bus);
        assertDoesNotThrow(() -> remote.intest(remote));
    }

    @Test
    void passLocalObjectFails(ORB orb, Bus bus) {
        test.local.Test remote = remote(orb, bus);
        assertThrows(MARSHAL.class, () -> remote.intest(new LocalTest_impl()));
    }

    @Test
    void passRemoteObjectInAny(ORB orb, Bus bus) {
        test.local.Test remote = remote(orb, bus);
        Any any = orb.create_any();
        TestHelper.insert(any, remote);
        assertDoesNotThrow(() -> remote.inany(any));
    }

    @Test
    void insertLocalObjectInAny(ORB orb) {
        test.local.Test local = new LocalTest_impl();
        Any any = orb.create_any();
        TestHelper.insert(any, local);
        assertSame(local, TestHelper.extract(any));
    }

    @Test
    void passLocalObjectInAnyFails(ORB orb, Bus bus) {
        test.local.Test remote = remote(orb, bus);
        Any any = orb.create_any();
        TestHelper.insert(any, new LocalTest_impl());
        assertThrows(MARSHAL.class, () -> remote.inany(any));
    }

    @Test
    void returnLocalObjectFails(ORB orb, Bus bus) {
        test.local.Test remote = remote(orb, bus);
        assertThrows(MARSHAL.class, remote::returntest);
    }

    @Test
    void returnLocalObjectInAnyFails(ORB orb, Bus bus) {
        test.local.Test remote = remote(orb, bus);
        assertThrows(MARSHAL.class, () -> remote.outany(new AnyHolder()));
    }
}
