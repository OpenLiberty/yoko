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
import testify.iiop.annotation.ExpectServerLog;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static testify.iiop.annotation.ConfigureServer.Separation.INTER_PROCESS;
import static testify.iiop.annotation.ExpectServerLog.LogLevel.SEVERE;

/**
 * Verifies that when the server JVM does NOT have --add-opens for java.util.concurrent,
 * attempting to marshal a ConcurrentHashMap as a return value fails on the server side
 * and the client receives a MarshalException.
 *
 * This is the counterpart to {@link ConcurrentHashMapMarshalTest#testServerWithAddOpensCanMarshal},
 * which uses a server configured WITH --add-opens and expects success.
 */
@ConfigureServer(
        separation = INTER_PROCESS,
        jvmArgs = {
                "--add-opens=java.base/java.lang=ALL-UNNAMED",
                "--add-opens=java.base/java.io=ALL-UNNAMED",
                "--add-opens=java.base/java.util=ALL-UNNAMED",
                "--add-opens=java.rmi/java.rmi=ALL-UNNAMED"
                // add enough permissions to start the server, but leave out the next ones
                // so creating a ConcurrentHashMap produces the exception we're testing
                //"--add-opens=java.base/java.util.concurrent=ALL-UNNAMED",
                //"--add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED",
                //"--add-opens=java.base/java.util.concurrent.locks=ALL-UNNAMED",
        }
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
     * ConcurrentHashMap return value fails on the server.
     * We verify the exception message contains add-opens advice, and that the same
     * message was also logged at SEVERE level in the server JVM.
     */
    @Test
    @EnabledForJreRange(min = JRE.JAVA_17)
    @ExpectServerLog(value = "ERROR: Yoko cannot reflectively access ",
                     level = SEVERE, logger = "org.apache.yoko.util.PrivilegedActions")
    @ExpectServerLog(value = "--add-opens=java.base/java.util.concurrent",
                     level = SEVERE, logger = "org.apache.yoko.util.PrivilegedActions")
    public void testServerWithoutAddOpensFails(MapService stub) {
        String target1 = "ERROR: Yoko cannot reflectively access ";
        String target2 = " --add-opens=java.base/";

        Throwable originalException = assertThrows(RuntimeException.class, stub::createServerMap);
        Throwable ex = originalException;
        boolean foundRightException = false;
        String matchedMessage = null;
        do {
            if (ex.getMessage() != null && ex.getMessage().contains(target1) && ex.getMessage().contains(target2)) {
                foundRightException = true;
                matchedMessage = ex.getMessage();
            }
            ex = ex.getCause();
        } while (!foundRightException && ex != null);

        assertTrue(foundRightException, "Expected exception message to contain [" + target1 + "] and [" + target2 + "] but was: " + originalException);

        // Verify the --add-opens suggestion names the same package as the inaccessible class
        String fqcn = extractAfter(matchedMessage, target1);
        String pkg = fqcn.contains(".") ? fqcn.substring(0, fqcn.lastIndexOf('.')) : fqcn;
        String addOpensLine = lineContaining(matchedMessage, target2);
        assertTrue(addOpensLine.contains(pkg),
                "Expected the --add-opens line [" + addOpensLine + "] to contain the package [" + pkg + "] from the inaccessible class [" + fqcn + "]");
    }

    /** Returns the substring of {@code text} that follows {@code prefix}, up to the first line-break. */
    private static String extractAfter(String text, String prefix) {
        if (text == null) return "";
        int start = text.indexOf(prefix);
        if (start < 0) return "";
        start += prefix.length();
        int end = text.length();
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n' || c == '\r') { end = i; break; }
        }
        String result = text.substring(start, end).trim();
        if (result.endsWith(".")) result = result.substring(0, result.length() - 1);
        return result;
    }

    /** Returns the line within {@code text} that contains {@code needle}, or empty string if not found. */
    private static String lineContaining(String text, String needle) {
        if (text == null) return "";
        for (String line : text.split("\n")) {
            if (line.contains(needle)) return line;
        }
        return "";
    }
}
