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
import testify.iiop.annotation.ConfigureServer;
import testify.iiop.annotation.ConfigureServer.RemoteImpl;

import java.rmi.Remote;
import java.rmi.RemoteException;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ConfigureServer
public class RMIExceptionHandlingTest {
    public static class AppExc extends Exception {}
    public static class RuntimeExc extends RuntimeException {}
    public interface AppExcThrower extends Remote { void throwAppExc() throws RemoteException, AppExc; }
    public interface RuntimeExcThrower extends Remote { void throwRuntimeExc() throws RemoteException; }
    @RemoteImpl
    public static final AppExcThrower impl1 = () -> {throw new AppExc();};
    @RemoteImpl
    public static final RuntimeExcThrower impl2 = () -> {throw new RuntimeExc();};

    @Test
    public void testRuntimeException(RuntimeExcThrower stub) {
        // Test that runtime exception is properly propagated
        assertThrows(RuntimeExc.class, stub::throwRuntimeExc);
    }

    @Test
    public void testAppException(AppExcThrower stub) {
        // Test that application exception is properly propagated
        assertThrows(AppExc.class, stub::throwAppExc);
    }
}

// Made with Bob
