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
import testify.iiop.annotation.ConfigureServer.RemoteImpl;
import testify.iiop.annotation.InteropTest;

import java.rmi.Remote;
import java.rmi.RemoteException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static testify.iiop.annotation.InteropTest.YokoVersion.V1_6_1;

/**
 * Enum interoperability tests against Yoko 1.6.1.
 * Server runs with Yoko 1.6.1, client with current version.
 *
 * Tests marshalling of:
 * - Simple enums
 * - Enums with fields
 * - Enums with anonymous classes
 * - Complex enums (fields + anonymous classes)
 *
 * This test may be skipped if Yoko 1.6.1 is not cached.
 */
@InteropTest(V1_6_1)
public class EnumInteropV1_6_1Test {

    // 1. Simple Enum - basic enum with no additional fields or methods
    enum SimpleColor {
        RED, GREEN, BLUE
    }

    // 2. Enum with Fields - enum with instance fields and constructor
    enum Planet {
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
    enum Operation {
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
    enum Vehicle {
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
        SimpleColor echoSimpleColor(SimpleColor color) throws RemoteException;
        Planet echoPlanet(Planet planet) throws RemoteException;
        Operation echoOperation(Operation op) throws RemoteException;
        Vehicle echoVehicle(Vehicle vehicle) throws RemoteException;
    }

    // Service Implementation
    static class EnumTestServiceImpl implements EnumTestService {
        public SimpleColor echoSimpleColor(SimpleColor color) { return color; }
        public Planet echoPlanet(Planet planet) { return planet; }
        public Operation echoOperation(Operation op) { return op; }
        public Vehicle echoVehicle(Vehicle vehicle) { return vehicle; }
    }

    @RemoteImpl
    public static final EnumTestService service = new EnumTestServiceImpl();

    @Test
    void testSimpleEnum(EnumTestService remote) throws Exception {
        // Test all simple enum values
        for (SimpleColor color : SimpleColor.values()) {
            SimpleColor result = remote.echoSimpleColor(color);
            assertEquals(color, result, "Simple enum value should match");
        }
    }

    @Test
    void testEnumWithFields(EnumTestService remote) throws Exception {
        // Test enum with fields - verify both identity and field values
        for (Planet planet : Planet.values()) {
            Planet result = remote.echoPlanet(planet);
            assertEquals(planet, result, "Enum with fields should match");
            assertEquals(planet.getMass(), result.getMass(), "Planet mass should match");
            assertEquals(planet.getRadius(), result.getRadius(), "Planet radius should match");
        }
    }

    @Test
    void testEnumWithAnonymousClasses(EnumTestService remote) throws Exception {
        // Test enum with anonymous classes - verify both identity and method behavior
        for (Operation op : Operation.values()) {
            Operation result = remote.echoOperation(op);
            assertEquals(op, result, "Enum with anonymous classes should match");
            assertEquals(op.apply(5, 3), result.apply(5, 3), 
                "Operation behavior should match for " + op);
        }
    }

    @Test
    void testComplexEnum(EnumTestService remote) throws Exception {
        // Test complex enum with both fields and anonymous classes
        for (Vehicle vehicle : Vehicle.values()) {
            Vehicle result = remote.echoVehicle(vehicle);
            assertEquals(vehicle, result, "Complex enum should match");
            assertEquals(vehicle.getWheels(), result.getWheels(), 
                "Vehicle wheels should match for " + vehicle);
            assertEquals(vehicle.getDescription(), result.getDescription(), 
                "Vehicle description should match for " + vehicle);
        }
    }
}