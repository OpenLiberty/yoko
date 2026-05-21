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
package testify.iiop.annotation;

import org.junit.jupiter.api.Test;
import testify.iiop.annotation.ConfigureServer.RemoteImpl;

import java.rmi.Remote;
import java.rmi.RemoteException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static testify.iiop.annotation.InteropTest.YokoVersion.V1_5_3;

/**
 * Basic test to verify interop testing infrastructure works.
 * Server runs with Yoko 1.5.3, client with current version.
 *
 * This test may be skipped if Yoko 1.5.3 is not cached.
 */
@InteropTest(V1_5_3)
public class InteropVersionTest {

    public interface TestService extends Remote {
        void setValue(int value) throws RemoteException;
        int getValue() throws RemoteException;
    }

    static class TestServiceImpl implements TestService {
        private int value;
        public void setValue(int value) { this.value = value; }
        public int getValue() { return value; }
    }

    @RemoteImpl
    public static final TestService service = new TestServiceImpl();

    @Test
    void testBasicInterop(TestService remote) throws Exception {
        // Test basic RMI-IIOP call across versions
        remote.setValue(42);
        assertEquals(42, remote.getValue());
    }
}