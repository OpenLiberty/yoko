/*
 * Copyright 2010 IBM Corporation and others.
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
package javax.rmi.CORBA;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import org.omg.CORBA.ORB;

public interface StubDelegate {
    void connect(Stub self, ORB orb) throws RemoteException;
    boolean equals(Stub self, Object o);
    int hashCode(Stub self);
    void readObject(Stub self, ObjectInputStream ois) throws IOException, ClassNotFoundException;
    String toString(Stub self);
    void writeObject(Stub self, ObjectOutputStream oos) throws IOException;
}

