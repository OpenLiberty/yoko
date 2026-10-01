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
package org.apache.yoko.util;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import static java.lang.Thread.currentThread;

public enum PrivilegedActions {
    ;
    private static final Logger LOGGER = Logger.getLogger(PrivilegedActions.class.getName());

    public static final PrivilegedAction<Properties> GET_SYSPROPS = System::getProperties;
    public static final PrivilegedAction<Map<Object, Object>> GET_SYSPROPS_OR_EMPTY_MAP = () -> {
        try {
            return System.getProperties();
        } catch (SecurityException swallowed) {
            return Collections.emptyMap();
        }
    };

    public static final PrivilegedAction<ClassLoader> GET_CONTEXT_CLASS_LOADER = () -> currentThread().getContextClassLoader();

    public static final PrivilegedAction<ClassLoader> GET_SYSTEM_CLASS_LOADER = ClassLoader::getSystemClassLoader;

    public static PrivilegedAction<String> getSysProp(final String key) { return () -> System.getProperty(key); }

    public static PrivilegedAction<String> getSysProp(final String key, final String defaultValue) { return () -> System.getProperty(key, defaultValue); }

    public static PrivilegedAction<ClassLoader> getClassLoader(final Class<?> clz) { return clz::getClassLoader; }

    public static <T> PrivilegedExceptionAction<Constructor<T>> getNoArgConstructor(Class<T> clz) { return clz::getDeclaredConstructor; }

    public static PrivilegedExceptionAction<Method> getMethod(Class<?> type, String name, Class<?>...parameterTypes) {
        return () -> type.getMethod(name, parameterTypes);
    }

    public static PrivilegedExceptionAction<Method> getDeclaredMethod(Class<?> type, String name, Class<?>...parameterTypes) {
        return () -> type.getDeclaredMethod(name, parameterTypes);
    }

    public static PrivilegedAction<Method[]> getDeclaredMethods(Class<?> type) {
        return type::getDeclaredMethods;
    }

    public static PrivilegedAction<Field[]> getDeclaredFields(Class<?> type) {
        return type::getDeclaredFields;
    }

    public static PrivilegedExceptionAction<Field> getDeclaredField(Class<?> type, String name) {
        return () -> type.getDeclaredField(name);
    }

    public static PrivilegedExceptionAction<Field> getField(Class<?> type, String name) {
        return () -> type.getField(name);
    }

    public static PrivilegedAction<Class<?>[]> getInterfaces(Class<?> type) {
        return type::getInterfaces;
    }

    public static <T extends AccessibleObject> PrivilegedAction<T> makeAccessible(T accessible) {
        return () -> {
            try {
                accessible.setAccessible(true);
            } catch (RuntimeException e) {
                if ("java.lang.reflect.InaccessibleObjectException".equals(e.getClass().getName())) { //Avoiding symbolic reference for java8 compatibility
                    logInaccessibleObject(accessible);
                }
                throw e;
            }
            return accessible;
        };
    }

    private static void logInaccessibleObject(AccessibleObject accessible) {
        Class<?> declaringClass = null;
        if (accessible instanceof Member) {
            declaringClass = ((Member) accessible).getDeclaringClass();
        }
        String className = declaringClass != null ? declaringClass.getName() : accessible.toString();

        String moduleName = "java.base";
        String packageName = "";
        if (declaringClass != null) {
            Package pkg = declaringClass.getPackage();
            if (pkg != null) {
                packageName = pkg.getName();
            }
            try {
                Method getModuleMethod = Class.class.getMethod("getModule");
                Object module = getModuleMethod.invoke(declaringClass);
                if (module != null) {
                    Method getNameMethod = module.getClass().getMethod("getName");
                    Object name = getNameMethod.invoke(module);
                    if (name != null) {
                        moduleName = name.toString();
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        String target = moduleName.isEmpty() ? packageName : moduleName + "/" + packageName;
        String message = String.format(
                "ERROR: Yoko cannot reflectively access %s.%n"
                + "       This is required for RMI-IIOP marshalling on Java 17+.%n"
                + "       Add the following JVM option to your application launch command:%n"
                + "           --add-opens=%s=ALL-UNNAMED",
                className, target);
        LOGGER.severe(message);
    }

    public static <T> PrivilegedAction<T> action(PrivilegedAction<T> action) { return action; }

    public static <T> PrivilegedExceptionAction<T> exAction(PrivilegedExceptionAction<T> action) { return action; }
}
