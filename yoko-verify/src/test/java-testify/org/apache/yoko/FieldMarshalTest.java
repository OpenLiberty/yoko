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
package org.apache.yoko;

import acme.RemoteFunction;
import acme.RemoteSupplier;
import org.junit.jupiter.api.Test;
import org.omg.CORBA.portable.IDLEntity;
import org.omg.TimeBase.IntervalT;
import testify.annotation.Logging;
import testify.iiop.annotation.ConfigureServer;
import testify.iiop.annotation.ConfigureServer.RemoteImpl;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static testify.iiop.annotation.ConfigureServer.Separation.INTER_PROCESS;

/**
 * Unmarshal a string in a field that is declared as a Comparable
 */
@ConfigureServer(separation = INTER_PROCESS)
public class FieldMarshalTest {
    public interface GetString extends Remote { Comparable<?> get() throws RemoteException; }
    public interface GetValue extends Remote { Value get() throws RemoteException; }
    public static class Value implements Serializable {
        static final long serialVersionUID = 1L;
        String f1 = "first field";
        Comparable<?> f2 = "second field";
        // object graph indirection for String field
        String f1_prime = f1;
        // object graph indirection for Comparable field
        Comparable<?> f2_prime = f2;
    }

    @RemoteImpl
    public static final GetString GET_STRING_IMPL = () -> "return value";

    @RemoteImpl
    public static final GetValue GET_VALUE_IMPL = Value::new;

    @Test
    @Logging("yoko.verbose.giop.in")
    public void unmarshalStringAsComparableReturnValue(GetString stub) throws RemoteException {
        Comparable<?> actual = stub.get();
        assertEquals("return value", actual);
    }

    @Test
    @Logging("yoko.verbose.giop.in")
    public void unmarshalStringAsComparableFieldValue(GetValue stub) throws RemoteException {
        Value result = stub.get();
        assertEquals("first field", result.f1);
        assertEquals("second field", result.f2);
        assertSame(result.f1, result.f1_prime);
        assertSame(result.f2, result.f2_prime);
    }
}
