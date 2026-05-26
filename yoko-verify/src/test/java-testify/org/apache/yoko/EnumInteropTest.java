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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import testify.iiop.annotation.ConfigureServer.RemoteImpl;
import testify.iiop.annotation.ConfigureServer.RemoteStub;
import testify.iiop.annotation.InteropTest;

import java.rmi.Remote;
import java.rmi.RemoteException;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        default <T extends Enum<T>> T echo(T t) { return Enum.valueOf(t.getDeclaringClass(), t.name()); }
    }

    @RemoteImpl
    public static final EnumTestService service = new EnumTestService() {};

    @RemoteStub
    public static EnumTestService remote;

    @ParameterizedTest
    @EnumSource(Color.class)
    void testSimpleEnum(Color color) throws Exception {
        Color result = remote.echo(color);
        assertEquals(color, result, "Simple enum value should match");
    }

    @ParameterizedTest
    @EnumSource(Planet.class)
    void testEnumWithFields(Planet planet) throws Exception {
        Planet result = remote.echo(planet);
        assertEquals(planet, result, "Enum with fields should match");
        assertEquals(planet.getMass(), result.getMass(), "Planet mass should match");
        assertEquals(planet.getRadius(), result.getRadius(), "Planet radius should match");
    }

    @ParameterizedTest
    @EnumSource(Operation.class)
    void testEnumWithAnonymousClasses(Operation op) throws Exception {
        Operation result = remote.echo(op);
        assertEquals(op, result, "Enum with anonymous classes should match");
        assertEquals(op.apply(5, 3), result.apply(5, 3), "Operation behavior should match for " + op);
    }

    @ParameterizedTest
    @EnumSource(Vehicle.class)
    void testComplexEnum(Vehicle vehicle) throws Exception {
            Vehicle result = remote.echo(vehicle);
            assertEquals(vehicle, result, "Complex enum should match");
            assertEquals(vehicle.getWheels(), result.getWheels(), "Vehicle wheels should match for " + vehicle);
            assertEquals(vehicle.getDescription(), result.getDescription(), "Vehicle description should match for " + vehicle);
    }
}

@InteropTest(V1_5_0)
class EnumInteropV150Test extends EnumInteropTest{}

@InteropTest(V1_6_1)
class EnumInteropV161Test extends EnumInteropTest{}
