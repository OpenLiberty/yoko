/*
 * Copyright 2024 IBM Corporation and others.
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
package org.apache.yoko.orb.IMR;

import static org.omg.CORBA.CompletionStatus.COMPLETED_NO;

import java.io.ObjectStreamException;

import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.portable.IDLEntity;

//
// IDL:orb.yoko.apache.org/IMR/ServerActivationMode:1.0
//
/**
 *
 * This enumeration contains the various activation modes of a server.
 *
 **/

public class ServerActivationMode implements IDLEntity
{
    private static ServerActivationMode [] values_ = new ServerActivationMode[2];
    private int value_;

    public final static int _SHARED = 0;
    public final static ServerActivationMode SHARED = new ServerActivationMode(_SHARED);
    public final static int _PERSISTENT = 1;
    public final static ServerActivationMode PERSISTENT = new ServerActivationMode(_PERSISTENT);

    protected
    ServerActivationMode(int value)
    {
        values_[value] = this;
        value_ = value;
    }

    public int
    value()
    {
        return value_;
    }

    public static ServerActivationMode
    from_int(int value)
    {
        if(value < values_.length)
            return values_[value];
        else
            throw new BAD_PARAM("Value (" + value  + ") out of range", 25, COMPLETED_NO);
    }

    private Object
    readResolve()
        throws ObjectStreamException
    {
        return from_int(value());
    }
}
