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
package org.apache.yoko.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.util.function.Supplier;

import static java.security.AccessController.doPrivileged;
import static org.apache.yoko.util.PrivilegedActions.getNoArgConstructor;

/**
 * Utility methods for creating instances efficiently using cached MethodHandles.
 */
public enum InstanceFactory {
    ;

    /**
     * Unchecked wrapper for instantiation failures.
     * Thrown when a class cannot be instantiated (abstract class, interface, no accessible constructor).
     */
    public static class CannotInstantiateException extends RuntimeException {
        public CannotInstantiateException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * ClassValue that caches factory methods for creating instances via public no-arg constructors.
     * Uses MethodHandles for better performance than reflection.
     * Does NOT use setAccessible - requires public constructors.
     */
    private static final ClassValue<Supplier<?>> INSTANCE_SUPPLIERS = new ClassValue<Supplier<?>>() {
        @Override
        protected Supplier<?> computeValue(Class<?> type) {
            try {
                Constructor<?> constructor = doPrivileged(getNoArgConstructor(type));

                // Convert to MethodHandle WITHOUT setAccessible - requires public constructor
                MethodHandle mh = MethodHandles.lookup().unreflectConstructor(constructor);

                return () -> newInstance(type, mh);
            } catch (Exception ex) {
                // Return a supplier that throws the exception when invoked
                return () -> {
                    throw new CannotInstantiateException("Cannot create instance of " + type.getName() +
                        " (requires public no-arg constructor)", ex);
                };
            }
        }
    };

    private static Object newInstance(Class<?> type, MethodHandle mh) {
        try {
            return mh.invoke();
        } catch (Throwable ex) {
            throw new CannotInstantiateException("Cannot instantiate " + type.getName(), ex);
        }
    }

    /**
     * Creates a new instance of the given type using its public no-arg constructor.
     *
     * @param <T> the type to instantiate
     * @param type the class to instantiate, or null
     * @return a new instance of the given type, or null if type is null
     * @throws CannotInstantiateException if the type cannot be instantiated
     */
    @SuppressWarnings("unchecked")
    public static <T> T createNoArgsInstance(Class<T> type) {
        return (null == type) ? null : (T) INSTANCE_SUPPLIERS.get(type).get();
    }
}
