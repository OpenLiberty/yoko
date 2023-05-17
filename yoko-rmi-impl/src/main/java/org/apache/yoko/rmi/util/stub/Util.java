/*==============================================================================
 * Copyright 2021 IBM Corporation and others.
 * 
 * Licensed under the Apache License, Version 2.0 (the License);
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
 *=============================================================================*/
package org.apache.yoko.rmi.util.stub;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.security.CodeSource;
import java.security.Permissions;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;

import static java.lang.Thread.currentThread;
import static java.security.AccessController.doPrivileged;
import static java.util.Objects.requireNonNull;

class Util {
    static String getPackageName(Class<?> clazz) {
        String class_name = clazz.getName();
        int idx = class_name.lastIndexOf('.');
        return idx == -1 ? null : class_name.substring(0, idx);
    }

    private enum DefineClass {
        ;
        private static final URL STUB_SOURCE_URL;
        static {
            try {
                STUB_SOURCE_URL = new URL(null, "yoko:stub", new URLStreamHandler() {protected URLConnection openConnection(URL u) { return null; }});
            } catch (MalformedURLException unexpected) {
                throw new Error(unexpected);
            }
        }
        private static final Certificate[] NO_CERTS = new Certificate[0];

        private static final Method defineClass;
        static {
            try {
                // get the method object
                Class<?> clc = ClassLoader.class;
                defineClass = clc.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class, ProtectionDomain.class );
                doPrivileged((PrivilegedAction<Void>) () -> { defineClass.setAccessible(true); return null; } );
            } catch (RuntimeException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new Error("unexpected exception: " + ex.getMessage(), ex);
            }
        }

        private static ProtectionDomain getProtectionDomain(ClassLoader loader) {
            return new ProtectionDomain(new CodeSource(STUB_SOURCE_URL, NO_CERTS), new Permissions(), loader, null);
        }

        /** Requires the caller to have class definition privileges */
        private static <S> Class<S> invoke(ClassLoader loader, String className, byte[] data) {
            try {
                //noinspection unchecked
                return (Class<S>)defineClass.invoke(loader, className, data, 0, data.length, getProtectionDomain(loader));
            } catch (IllegalAccessException|IllegalArgumentException ex) {
                throw new Error("internal error", ex);
            } catch (InvocationTargetException ex) {
                try {
                    throw ex.getTargetException();
                } catch (Error|RuntimeException e) {
                    throw e;
                } catch (Throwable e) {
                    throw new Error("unexpected exception: " + ex.getMessage(), ex);
                }
            }
        }
    }

    /** Requires the caller to have class definition privileges */
    static <S> Class<S> defineClass(final ClassLoader loader, String className, byte[] data) {
        return loader == null ?
                defineClass(requireNonNull(currentThread().getContextClassLoader()), className, data) :
                DefineClass.invoke(loader, className, data);
    }

}
