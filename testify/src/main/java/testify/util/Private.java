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
package testify.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility for invoking package-private methods via reflection in tests.
 * Provides type-safe access to methods that are not publicly accessible.
 */
public enum Private {
    ;

    private static final Map<Class<?>, Class<?>> PRIMITIVE_TO_WRAPPER = new HashMap<>();
    private static final Map<Class<?>, Class<?>> WRAPPER_TO_PRIMITIVE = new HashMap<>();

    static {
        PRIMITIVE_TO_WRAPPER.put(boolean.class, Boolean.class);
        PRIMITIVE_TO_WRAPPER.put(byte.class, Byte.class);
        PRIMITIVE_TO_WRAPPER.put(char.class, Character.class);
        PRIMITIVE_TO_WRAPPER.put(short.class, Short.class);
        PRIMITIVE_TO_WRAPPER.put(int.class, Integer.class);
        PRIMITIVE_TO_WRAPPER.put(long.class, Long.class);
        PRIMITIVE_TO_WRAPPER.put(float.class, Float.class);
        PRIMITIVE_TO_WRAPPER.put(double.class, Double.class);
        PRIMITIVE_TO_WRAPPER.put(void.class, Void.class);

        PRIMITIVE_TO_WRAPPER.forEach((k, v) -> WRAPPER_TO_PRIMITIVE.put(v, k));
    }

    /**
     * Invokes a static method (including package-private) on the specified class.
     *
     * @param <T> the expected return type
     * @param targetClass the class containing the method
     * @param methodName the name of the method to invoke
     * @param args the arguments to pass to the method (varargs)
     * @return the result of the method invocation, cast to type T
     * @throws RuntimeException if the method cannot be found or invoked
     */
    @SuppressWarnings("unchecked")
    public static <T> T invoke(Class<?> targetClass, String methodName, Object... args) {
        Method method = findMethod(targetClass, methodName, args);
        try {
            method.setAccessible(true);
            return (T) method.invoke(null, args);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to access method: " + methodName + " on " + targetClass.getName(), e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new RuntimeException("Method invocation failed: " + methodName + " on " + targetClass.getName(), cause);
        }
    }

    private static Method findMethod(Class<?> targetClass, String methodName, Object... args) {
        Method[] methods = targetClass.getDeclaredMethods();
        Method[] candidates = Arrays.stream(methods)
                .filter(m -> m.getName().equals(methodName))
                .filter(m -> parametersMatch(m.getParameterTypes(), args))
                .toArray(Method[]::new);

        if (candidates.length == 0) {
            String availableMethods = Arrays.stream(methods)
                    .map(m -> m.getName() + "(" + Arrays.stream(m.getParameterTypes())
                            .map(Class::getSimpleName)
                            .collect(Collectors.joining(", ")) + ")")
                    .collect(Collectors.joining(", "));
            throw new RuntimeException(
                    "No method found: " + methodName + " with " + args.length + " parameters on " + targetClass.getName() +
                    ". Available methods: " + availableMethods);
        }

        if (candidates.length > 1) {
            String ambiguousMethods = Arrays.stream(candidates)
                    .map(m -> m.getName() + "(" + Arrays.stream(m.getParameterTypes())
                            .map(Class::getSimpleName)
                            .collect(Collectors.joining(", ")) + ")")
                    .collect(Collectors.joining(", "));
            throw new RuntimeException(
                    "Ambiguous method call: multiple methods match " + methodName + " on " + targetClass.getName() +
                    ". Candidates: " + ambiguousMethods);
        }

        return candidates[0];
    }

    private static boolean parametersMatch(Class<?>[] paramTypes, Object[] args) {
        if (paramTypes.length != args.length) {
            return false;
        }

        for (int i = 0; i < paramTypes.length; i++) {
            if (args[i] == null) {
                // null can be assigned to any non-primitive type
                if (paramTypes[i].isPrimitive()) {
                    return false;
                }
                continue;
            }

            Class<?> argType = args[i].getClass();
            Class<?> paramType = paramTypes[i];

            // Exact match
            if (paramType.isAssignableFrom(argType)) {
                continue;
            }

            // Handle primitive/wrapper conversions
            if (paramType.isPrimitive()) {
                Class<?> wrapperType = PRIMITIVE_TO_WRAPPER.get(paramType);
                if (wrapperType != null && wrapperType.isAssignableFrom(argType)) {
                    continue;
                }
            } else {
                Class<?> primitiveType = WRAPPER_TO_PRIMITIVE.get(paramType);
                if (primitiveType != null) {
                    Class<?> wrapperType = PRIMITIVE_TO_WRAPPER.get(primitiveType);
                    if (wrapperType != null && wrapperType.isAssignableFrom(argType)) {
                        continue;
                    }
                }
            }

            return false;
        }

        return true;
    }
}
