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
package org.apache.yoko.rmi.impl;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.security.PrivilegedActionException;
import java.util.function.Function;

import static java.lang.invoke.MethodHandles.publicLookup;
import static java.lang.invoke.MethodType.methodType;
import static java.security.AccessController.doPrivileged;
import static org.apache.yoko.util.PrivilegedActions.exAction;

/**
 * Helper class for obtaining MethodHandles to methods, including private ones.
 * This class provides secure access to private methods using MethodHandles,
 * with support for both Java 8 and Java 9+ APIs.
 *
 * <p>Security: This class uses privileged blocks when necessary and does not
 * expose Lookup objects to prevent security vulnerabilities.</p>
 */
enum MethodHandleHelper {
    ; // Empty enum - prevents instantiation

    /**
     * Function that creates a Lookup for a given class.
     * Determined once at class initialization based on Java version.
     */
    private static final Function<Class<?>, Lookup> LOOKUP_FACTORY;

    static {
        Function<Class<?>,Lookup> lookupFactory;
        try {
            lookupFactory = createJava9LookupFactory();
        } catch (NoSuchMethodException | IllegalAccessException e) {
            lookupFactory = createJava8LookupFactory();
        }
        LOOKUP_FACTORY = lookupFactory;
    }

    /**
     * Creates a lookup factory using Java 9+ privateLookupIn API.
     */
    private static Function<Class<?>, Lookup> createJava9LookupFactory()
            throws NoSuchMethodException, IllegalAccessException {
        MethodHandle privateLookupIn = publicLookup().findStatic(
            MethodHandles.class,
            "privateLookupIn",
            methodType(
                Lookup.class,
                Class.class,
                Lookup.class
            )
        );

        // Capture the lookup once at build time
        Lookup callerLookup = MethodHandles.lookup();

        return targetClass -> {
            try {
                return (Lookup) privateLookupIn.invoke(targetClass, callerLookup);
            } catch (Throwable t) {
                throw new RuntimeException("Failed to create private lookup for " + targetClass, t);
            }
        };
    }

    /**
     * Creates a lookup factory using Java 8 reflection-based approach.
     */
    private static Function<Class<?>, Lookup> createJava8LookupFactory() {
        Constructor<Lookup> constructor;
        try {
            constructor = doPrivileged(exAction(() -> {
                Constructor<Lookup> ctor = Lookup.class.getDeclaredConstructor(Class.class, int.class);
                ctor.setAccessible(true);
                return ctor;
            }));
        } catch (PrivilegedActionException pae) {
            throw new ExceptionInInitializerError(pae.getException());
        }

        return targetClass -> {
            try {
                return doPrivileged(exAction(() ->
                    constructor.newInstance(targetClass, -1) // -1 = all access modes
                ));
            } catch (PrivilegedActionException pae) {
                throw new RuntimeException("Failed to create private lookup for " + targetClass, pae.getException());
            }
        };
    }

    /**
     * Cache of Lookup objects per class, providing private access to each class.
     */
    private static final class LookupCache extends ClassValue<Lookup> {
        @Override
        protected Lookup computeValue(Class<?> targetClass) {
            return LOOKUP_FACTORY.apply(targetClass);
        }
    }

    private static final LookupCache PRIVATE_LOOKUPS = new LookupCache();

    /**
     * Gets a Lookup with private access to the target class.
     *
     * @param targetClass the class to get private access to
     * @return a Lookup with full private access to the target class
     */
    private static Lookup getPrivateLookup(Class<?> targetClass) {
        return PRIVATE_LOOKUPS.get(targetClass);
    }

    /**
     * Looks up a private method with the given name and signature.
     *
     * @param targetClass the class to look up the method in
     * @param methodName the name of the method
     * @param methodType the method signature
     * @return a MethodHandle for the method, or null if not found
     * @throws Exception if an error occurs during lookup (other than method not found)
     */
    static MethodHandle getMethodHandle(Class<?> targetClass, String methodName, MethodType methodType) throws Exception {
        Lookup lookup = getPrivateLookup(targetClass);
        try {
            return lookup.findVirtual(targetClass, methodName, methodType);
        } catch (NoSuchMethodException e) {
            return null; // Method doesn't exist
        }
    }
}
