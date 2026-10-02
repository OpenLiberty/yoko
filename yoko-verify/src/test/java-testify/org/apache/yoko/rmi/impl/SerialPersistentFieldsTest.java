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

package org.apache.yoko.rmi.impl;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toUnmodifiableList;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectInputStream.GetField;
import java.io.ObjectOutputStream;
import java.io.ObjectOutputStream.PutField;
import java.io.ObjectStreamClass;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.apache.yoko.orb.CORBA.YokoInputStream;
import org.apache.yoko.orb.CORBA.YokoOutputStream;
import org.junit.jupiter.api.Test;
import org.omg.CORBA.ORB;

import acme.AbstractInterface;
import acme.AbstractValue;
import acme.StringValue;
import testify.iiop.annotation.ConfigureOrb;

@SuppressWarnings({"serial"})
@ConfigureOrb
public abstract class SerialPersistentFieldsTest implements Serializable {
    @Test
    public void marshalAndUnmarshal(ORB orb) {
        YokoOutputStream out = (YokoOutputStream)orb.create_output_stream();
        out.write_value(this);
        System.out.println(out.getBufferReader().dumpAllData());
        YokoInputStream in = out.create_input_stream();
        Serializable result = in.read_value();
        assertNotNull(result);
    }
}

class Primitives extends SerialPersistentFieldsTest {
        private static final ObjectStreamField[] serialPersistentFields = {
                new ObjectStreamField("z", boolean.class),
                new ObjectStreamField("b", byte.class),
                new ObjectStreamField("c", char.class),
                new ObjectStreamField("s", short.class),
                new ObjectStreamField("i", int.class),
                new ObjectStreamField("f", float.class),
                new ObjectStreamField("j", long.class),
                new ObjectStreamField("d", double.class),
        };
        static final boolean Z = true;
        static final byte B = -127;
        static final char C = 'C';
        static final short S = 0x0F00;
        static final int I = 0xCAFEBABE;
        static final float F = 3.14F;
        static final long J = 0xFEED_FACE_DEAD_BEEFL;
        static final double D = 6.28D;

        private void writeObject(ObjectOutputStream out) throws IOException {
            System.out.println("### writeObject() called");
            PutField fields = out.putFields();
            fields.put("z", Z);
            fields.put("b", B);
            fields.put("c", C);
            fields.put("s", S);
            fields.put("i", I);
            fields.put("f", F);
            fields.put("j", J);
            fields.put("d", D);
            out.writeFields();
        }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            GetField fields = in.readFields();
            assertEquals(Z, fields.get("z", false));
            assertEquals(B, fields.get("b", (byte)0));
            assertEquals(C, fields.get("c", (char)0));
            assertEquals(S, fields.get("s", (short)0));
            assertEquals(I, fields.get("i", 0));
            assertEquals(F, fields.get("f", 0F));
            assertEquals(J, fields.get("j", 0L));
            assertEquals(D, fields.get("d", 0D));
        }
}

class MiscellaneousTypes extends SerialPersistentFieldsTest {
        private static final ObjectStreamField[] serialPersistentFields = {
                new ObjectStreamField("s", String.class),
                new ObjectStreamField("c", Class.class),
                new ObjectStreamField("d", Date.class),
                new ObjectStreamField("e", Enum.class),
                new ObjectStreamField("t", TimeUnit.class),
        };
        static final String S = "a string";
        static final Class<?> C = String.class;
        static final Date D = new Date();
        static final Enum<?> E = TimeUnit.DAYS;
        static final TimeUnit T = TimeUnit.MICROSECONDS;

        private void writeObject(ObjectOutputStream out) throws IOException {
            System.out.println("### writeObject() called");
            PutField fields = out.putFields();
            fields.put("s", S);
            fields.put("c", C);
            fields.put("d", D);
            fields.put("e", E);
            fields.put("t", T);
            out.writeFields();
        }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            GetField fields = in.readFields();
            assertEquals(S, fields.get("s", ""));
            assertEquals(C, fields.get("c", null));
            assertEquals(D, fields.get("d", null));
            assertEquals(E, fields.get("e", null));
            assertEquals(T, fields.get("t", null));
        }
}

class ValueTypes extends SerialPersistentFieldsTest {
    private static final ObjectStreamField[] serialPersistentFields = {
            new ObjectStreamField("abstractValue", AbstractInterface.class),
            new ObjectStreamField("valueInterface", AbstractValue.class),
            new ObjectStreamField("valueClass", StringValue.class),
            new ObjectStreamField("anyValue", Serializable.class)
    };
    private static final List<String> FIELD_NAMES = Stream.of(serialPersistentFields).map(ObjectStreamField::getName).collect(toUnmodifiableList());

    private void writeObject(ObjectOutputStream out) throws IOException {
        System.out.println("### writeObject() called");
        PutField fields = out.putFields();
        FIELD_NAMES.forEach(name -> fields.put(name, new StringValue(name)));
        out.writeFields();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        GetField fields = in.readFields();
        for (String name: FIELD_NAMES) assertEquals(name, ((StringValue) fields.get(name, null)).toString());
    }
}

class DefaultSerialPersistentFields extends SerialPersistentFieldsTest {
    private static final ObjectStreamField[] serialPersistentFields = {
            new ObjectStreamField("name", String.class)
    };

    private final String name = "example";
    private final int ordinal = 42;

    @Override
    @Test
    public void marshalAndUnmarshal(ORB orb) {
        YokoOutputStream out = (YokoOutputStream) orb.create_output_stream();
        out.write_value(this);
        System.out.println(out.getBufferReader().dumpAllData());
        YokoInputStream in = out.create_input_stream();
        DefaultSerialPersistentFields result = (DefaultSerialPersistentFields) in.read_value();
        assertNotNull(result);
        assertEquals(name, result.name);
        assertEquals(42, result.ordinal);
    }

    @Test
    public void javaSerializationUsesSerialPersistentFields() throws Exception {
        final byte[] data;
        try (
                ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
                ObjectOutputStream out = new ObjectOutputStream(byteOut)
        ) {
            out.writeObject(this);
            data = byteOut.toByteArray();
        }

        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(data))) {
            DefaultSerialPersistentFields result = (DefaultSerialPersistentFields) in.readObject();
            assertNotNull(result);
            assertEquals(name, result.name);
            assertEquals(42, result.ordinal);
        }
    }
}

// ---------------------------------------------------------------------------
// ThreeStrings — data class under test
// ---------------------------------------------------------------------------

/**
 * Demonstrates {@code serialPersistentFields} with phantom fields (x, y) that
 * have no corresponding Java fields, and three string values transferred as
 * custom data written after {@code writeFields()}.
 *
 * <p>The non-persistent field {@code z} is set to {@code -1} in the constructor
 * but has no assignment during deserialization, so it must retain the Java
 * default value of {@code 0} on a deserialized instance.
 */
@SuppressWarnings("serial")
class ThreeStrings implements Serializable {

    /**
     * Persistent-field descriptor: x and y are the only persistent fields.
     * {@code x} maps to the real Java field below; {@code y} has no corresponding
     * Java field in this version of the class.
     */
    private static final ObjectStreamField[] serialPersistentFields = { //Java serialization's behaviour is modified by the existence of a field with this signature
            new ObjectStreamField("x", int.class),
            new ObjectStreamField("y", int.class),
    };

    /** Persistent field — populated by {@code defaultReadObject()} during deserialization. */
    int x;

    String first;
    String second;
    String third;

    /** Not persistent. Set to -1 by the constructor; not restored during deserialization. */
    int z;

    ThreeStrings(String first, String second, String third) {
        this.first  = first;
        this.second = second;
        this.third  = third;
        this.z      = -1;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        PutField fields = out.putFields();
        fields.put("x", 6);
        fields.put("y", 7);
        out.writeFields();
        // Three strings written as custom data after the persistent fields.
        out.writeUTF(first);
        out.writeUTF(second);
        out.writeUTF(third);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        // Restores the persistent field x into the Java field of the same name.
        // y has no Java field in this version; its serialized value is silently dropped.
        in.defaultReadObject();
        // Read the three strings that were written as custom data.
        first  = in.readUTF();
        second = in.readUTF();
        third  = in.readUTF();
        // z is deliberately not assigned; it will be 0 (Java default for int).
    }
}

// ---------------------------------------------------------------------------
// Test class
// ---------------------------------------------------------------------------

/** Verifies {@link ThreeStrings} serialization behaviour per the test plan. */
class ThreeStringsSerializationTest {

    // Test case 1: initial state
    @Test
    public void checkInitialState() {
        ThreeStrings ts = new ThreeStrings("red", "green", "blue");
        assertEquals(-1, ts.z, "z must be -1 before serialization");
    }

    // Test case 2: persistent-field declaration
    @Test
    public void checkPersistentFieldDeclaration() {
        ObjectStreamField[] fields = ObjectStreamClass.lookup(ThreeStrings.class).getFields();
        Set<String> names = stream(fields)
                .map(ObjectStreamField::getName)
                .collect(java.util.stream.Collectors.toSet());
        assertEquals(Set.of("x", "y"), names,
                "serialPersistentFields must contain exactly x and y");
        assertFalse(names.contains("first"),  "first must not be a persistent field");
        assertFalse(names.contains("second"), "second must not be a persistent field");
        assertFalse(names.contains("third"),  "third must not be a persistent field");
        assertFalse(names.contains("z"),      "z must not be a persistent field");
    }

    // Test case 3: round-trip
    @Test
    public void roundTripThroughJavaSerialization() throws Exception {
        ThreeStrings original = new ThreeStrings("red", "green", "blue");

        final byte[] data;
        try (
                ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
                ObjectOutputStream out = new ObjectOutputStream(byteOut)
        ) {
            out.writeObject(original);
            data = byteOut.toByteArray();
        }

        final ThreeStrings result;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(data))) {
            result = (ThreeStrings) in.readObject();
        }

        assertEquals("red",   result.first,  "first string must survive the round-trip");
        assertEquals("green", result.second, "second string must survive the round-trip");
        assertEquals("blue",  result.third,  "third string must survive the round-trip");
        assertEquals(6, result.x,
                "persistent field x must equal 6 after deserialization");
        assertEquals(0, result.z,
                "z must be 0: its initialiser does not run during deserialization");
    }
}
