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
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.apache.yoko;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import testify.annotation.Logging;
import testify.iiop.annotation.ConfigureServer.RemoteImpl;
import testify.iiop.annotation.ConfigureServer.RemoteStub;
import testify.iiop.annotation.InteropTest;

import java.rmi.Remote;
import java.rmi.RemoteException;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static testify.iiop.annotation.InteropTest.YokoVersion.V1_5_0;
import static testify.iiop.annotation.InteropTest.YokoVersion.V1_6_1;

/**
 * Enum interoperability tests against Yoko 1.5.0.
 * Server runs with Yoko 1.5.0, client with current version.
 *
 * Tests marshalling of:
 * - Simple enums
 * - Enums with fields
 * - Enums with anonymous classes
 * - Complex enums (fields + anonymous classes)
 *
 * This test may be skipped if Yoko 1.5.0 is not cached.
 */
abstract class EnumInteropTest {

    // 1. Simple Enum - basic enum with no additional fields or methods
    public enum Color { RED, GREEN, BLUE }

    // 2. Enum with Fields - enum with instance fields and constructor
    public enum Planet {
        MERCURY(3.303e+23, 2.4397e6),
        VENUS(4.869e+24, 6.0518e6),
        EARTH(5.976e+24, 6.37814e6);

        private final double mass;
        private final double radius;

        Planet(double mass, double radius) {
            this.mass = mass;
            this.radius = radius;
        }

        public double getMass() { return mass; }
        public double getRadius() { return radius; }
    }

    // 3. Enum with Anonymous Classes - enum where some members override methods
    public enum Operation {
        PLUS {
            public int apply(int x, int y) { return x + y; }
        },
        MINUS {
            public int apply(int x, int y) { return x - y; }
        },
        TIMES {
            public int apply(int x, int y) { return x * y; }
        };

        public abstract int apply(int x, int y);
    }

    // 4. Complex Enum - enum with both fields AND anonymous subclasses
    public enum Vehicle {
        CAR(4) {
            public String getDescription() {
                return "A car with " + getWheels() + " wheels";
            }
        },
        MOTORCYCLE(2) {
            public String getDescription() {
                return "A motorcycle with " + getWheels() + " wheels";
            }
        },
        BICYCLE(2) {
            public String getDescription() {
                return "A bicycle with " + getWheels() + " wheels";
            }
        };

        private final int wheels;

        Vehicle(int wheels) {
            this.wheels = wheels;
        }

        public int getWheels() { return wheels; }
        public abstract String getDescription();
    }

    // Remote Interface for testing enum marshalling
    public interface EnumTestService extends Remote {
        default <E extends Enum<E>> E echo(E e) throws RemoteException { return e; }
        default <E extends Enum<E>> E[] echo(E[] values) throws RemoteException {
            assertNotNull(values);
            assertTrue(0 < values.length);
            Class<E> enumClass = values[0].getDeclaringClass();
            assertEnumClassValues(enumClass, values);
            return values;
        }
    }

    @RemoteImpl
    public static final EnumInteropTest.EnumTestService service = new EnumTestService() {};

    @RemoteStub
    public static EnumInteropTest.EnumTestService remote;

    @ParameterizedTest
    @EnumSource(Color.class)
    void testSimpleEnum(Color color) throws Exception {
        Color result = remote.echo(color);
        assertSame(result, color, "Simple enum should be the same instance");
    }

    @ParameterizedTest
    @EnumSource(EnumInteropTest.Planet.class)
    void testEnumWithFields(EnumInteropTest.Planet planet) throws Exception {
        Planet result = remote.echo(planet);
        assertSame(planet, result, "Enum with fields should be the same instance");
    }

    @ParameterizedTest
    @EnumSource(Operation.class)
    void testEnumWithAnonymousClasses(Operation op) throws Exception {
        Operation result = remote.echo(op);
        assertSame(op, result, "Enum with anonymous classes should be the same instance");
    }

    @ParameterizedTest
    @EnumSource(EnumInteropTest.Vehicle.class)
    void testComplexEnum(Vehicle vehicle) throws Exception {
        Vehicle result = remote.echo(vehicle);
        assertSame(vehicle, result, "Complex enum should be the same instance");
    }

    @ParameterizedTest
    @ValueSource(classes = {Color.class, Planet.class, Operation.class, EnumInteropTest.Vehicle.class})
    <E extends Enum<E>> void testEnumValues(Class<E> enumClass) throws Exception {
        E[] values = remote.echo(enumClass.getEnumConstants());
        assertEnumClassValues(enumClass, values);
    }

    private static <E extends Enum<E>> void assertEnumClassValues(Class<E> enumClass, E[] result) {
        // verify the received array has same instances as local values
        E[] localValues = enumClass.getEnumConstants();
        assertEquals(asList(localValues), asList(result));
        for (int i = 0; i < localValues.length; i++) {
            assertSame(localValues[i], result[i], "Enum at index " + i + " should be the same instance for " + enumClass.getSimpleName());
        }
    }
}

@InteropTest(V1_5_0)
@Logging("yoko.verbose.giop")
class EnumInteropV150Test extends EnumInteropTest{}

@InteropTest(V1_6_1)
class EnumInteropV161Test extends EnumInteropTest{}
