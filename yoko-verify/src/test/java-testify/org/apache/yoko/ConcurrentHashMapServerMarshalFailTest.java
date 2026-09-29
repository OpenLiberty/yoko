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
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import testify.iiop.annotation.ConfigureServer;
import testify.iiop.annotation.ConfigureServer.RemoteImpl;

import java.lang.reflect.InaccessibleObjectException;
import java.rmi.MarshalException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static testify.iiop.annotation.ConfigureServer.Separation.INTER_PROCESS;

/**
 * Verifies that when the server JVM does NOT have --add-opens for java.util.concurrent,
 * attempting to marshal a ConcurrentHashMap as a return value fails on the server side
 * and the client receives a MarshalException.
 *
 * This is the counterpart to {@link ConcurrentHashMapMarshalTest#testServerWithAddOpensCanMarshal},
 * which uses a server configured WITH --add-opens and expects success.
 */
@ConfigureServer(
        separation = INTER_PROCESS
        // Intentionally no jvmArgs: the server JVM does NOT get --add-opens java.base/java.util.concurrent,
        // so attempting to serialise a ConcurrentHashMap there must fail.
)
public class ConcurrentHashMapServerMarshalFailTest {

    public interface MapService extends Remote {
        ConcurrentHashMap<String, String> createServerMap() throws RemoteException;
    }

    @RemoteImpl
    public static final MapService SERVER_IMPL = new MapService() {
        @Override
        public ConcurrentHashMap<String, String> createServerMap() {
            ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();
            map.put("key1", "val1");
            map.put("key2", "val2");
            return map;
        }
    };

    /**
     * The server JVM has no --add-opens for java.util.concurrent, so serialising the
     * ConcurrentHashMap return value raises an InaccessibleObjectException on the server.
     * That propagates across the wire as a CORBA MARSHAL system exception, which the
     * RMI-IIOP layer maps to a java.rmi.MarshalException on the client.
     * We verify the root cause is an InaccessibleObjectException.
     */
    @Test
    @EnabledForJreRange(min = JRE.JAVA_17)
    public void testServerWithoutAddOpensFails(MapService stub) {
        MarshalException ex = assertThrows(MarshalException.class, stub::createServerMap);

        boolean foundInaccessibleException = false;
        Throwable cause = ex;
        while (cause != null) {
            if (cause instanceof InaccessibleObjectException) {
                foundInaccessibleException = true;
                break;
            }
            cause = cause.getCause();
        }
        assertTrue(foundInaccessibleException,
                "Expected InaccessibleObjectException in cause chain, but got: " + ex);
    }
}
