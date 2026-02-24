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
package org.omg.PortableServer;

// IDL:omg.org/PortableServer/ThreadPolicyValue:1.0

import org.omg.CORBA.BAD_PARAM;

import java.io.ObjectStreamException;

import static org.omg.CORBA.CompletionStatus.COMPLETED_NO;

public class ThreadPolicyValue implements org.omg.CORBA.portable.IDLEntity {
    public final static int _ORB_CTRL_MODEL = 0;
    public final static ThreadPolicyValue ORB_CTRL_MODEL = new ThreadPolicyValue(_ORB_CTRL_MODEL);
    public final static int _SINGLE_THREAD_MODEL = 1;
    public final static ThreadPolicyValue SINGLE_THREAD_MODEL = new ThreadPolicyValue(_SINGLE_THREAD_MODEL);

    private static final ThreadPolicyValue [] values = {ORB_CTRL_MODEL, SINGLE_THREAD_MODEL};
    private final int value;

    protected ThreadPolicyValue(int value) { this.value = value; }

    public int value() { return value; }

    public static ThreadPolicyValue from_int(int value) {
        try {
            return values[value];
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new BAD_PARAM("Value (" + value  + ") out of range", 25, COMPLETED_NO);
        }
    }

    private Object readResolve() throws ObjectStreamException { return from_int(value()); }
}
