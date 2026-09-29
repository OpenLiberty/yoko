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

import org.apache.yoko.orb.CORBA.YokoOutputStream;
import org.apache.yoko.orb.OCI.GiopVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import testify.iiop.annotation.ConfigureServer;
import testify.iiop.annotation.ConfigureServer.RemoteImpl;

import java.lang.reflect.InaccessibleObjectException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static testify.iiop.annotation.ConfigureServer.Separation.INTER_PROCESS;

@ConfigureServer(
        separation = INTER_PROCESS,
        jvmArgs = {
                "--add-opens=java.base/java.lang=ALL-UNNAMED",
                "--add-opens=java.base/java.io=ALL-UNNAMED",
                "--add-opens=java.base/java.util=ALL-UNNAMED",
                "--add-opens=java.base/java.util.concurrent=ALL-UNNAMED",
                "--add-opens=java.rmi/java.rmi=ALL-UNNAMED"
        }
)
public class ConcurrentHashMapMarshalTest {

    public interface MapService extends Remote {
        ConcurrentHashMap<String, String> echoMap(ConcurrentHashMap<String, String> map) throws RemoteException;
        ConcurrentHashMap<String, String> createServerMap() throws RemoteException;
    }

    @RemoteImpl
    public static final MapService SERVER_IMPL = new MapService() {
        @Override
        public ConcurrentHashMap<String, String> echoMap(ConcurrentHashMap<String, String> map) {
            return map;
        }

        @Override
        public ConcurrentHashMap<String, String> createServerMap() {
            ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();
            map.put("key1", "val1");
            map.put("key2", "val2");
            return map;
        }
    };

    /**
     * 1. On the client (which does NOT have --add-opens for java.util.concurrent),
     * writing/marshalling a populated ConcurrentHashMap fails due to module encapsulation.
     */
    @Test
    @EnabledForJreRange(min = JRE.JAVA_17)
    public void testClientMarshalFailsWithoutAddOpens() {
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();
        map.put("hello", "world");

        YokoOutputStream out = new YokoOutputStream(null, GiopVersion.GIOP1_2);

        Throwable thrown = assertThrows(Throwable.class, () -> {
            out.write_value(map, ConcurrentHashMap.class);
        });

        boolean foundInaccessibleException = false;
        Throwable cause = thrown;
        while (cause != null) {
            if (cause instanceof InaccessibleObjectException) {
                foundInaccessibleException = true;
                break;
            }
            cause = cause.getCause();
        }
        assertTrue(foundInaccessibleException,
                "Expected InaccessibleObjectException due to missing --add-opens for java.util.concurrent");
    }

    /**
     * 2. Over the network with the remote server process configured with --add-opens:
     * The server ORB running in the forked JVM successfully marshals the map.
     */
    @Test
    @EnabledForJreRange(min = JRE.JAVA_17)
    public void testServerWithAddOpensCanMarshal(MapService stub) throws Exception {
        ConcurrentHashMap<String, String> remoteMap = stub.createServerMap();

        assertEquals(2, remoteMap.size());
        assertEquals("val1", remoteMap.get("key1"));
        assertEquals("val2", remoteMap.get("key2"));
    }
}
