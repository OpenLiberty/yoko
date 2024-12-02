/*
 * Copyright 2024 IBM Corporation and others.
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

import static java.util.stream.Collectors.toUnmodifiableList;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectInputStream.GetField;
import java.io.ObjectOutputStream;
import java.io.ObjectOutputStream.PutField;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.apache.yoko.orb.CORBA.InputStream;
import org.apache.yoko.orb.CORBA.OutputStream;
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
        OutputStream out = (OutputStream)orb.create_output_stream();
        out.write_value(this);
        System.out.println(out.getBufferReader().dumpAllData());
        InputStream in = out.create_input_stream();
        Serializable result = in.read_value();
        assertNotNull(result);
    }

    public static class Primitives extends SerialPersistentFieldsTest {
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

    public static class MiscellaneousTypes extends SerialPersistentFieldsTest {
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

    public static class ValueTypes extends SerialPersistentFieldsTest {
        private static final ObjectStreamField[] serialPersistentFields = {
                new ObjectStreamField("abstractValue", AbstractInterface.class),
                new ObjectStreamField("valueInterface", AbstractValue.class),
                new ObjectStreamField("valueClass", StringValue.class)
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
}
