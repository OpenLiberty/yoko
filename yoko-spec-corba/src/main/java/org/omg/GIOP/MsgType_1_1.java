/*
 * Copyright 2019 IBM Corporation and others.
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
package org.omg.GIOP;

// IDL:omg.org/GIOP/MsgType_1_1:1.0

import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.portable.IDLEntity;

import static org.omg.CORBA.CompletionStatus.COMPLETED_NO;

public class MsgType_1_1 implements IDLEntity {
    private static final MsgType_1_1 [] values_ = new MsgType_1_1[8];
    private final int value_;

    public final static int _Request = 0;
    public final static MsgType_1_1 Request = new MsgType_1_1(_Request);
    public final static int _Reply = 1;
    public final static MsgType_1_1 Reply = new MsgType_1_1(_Reply);
    public final static int _CancelRequest = 2;
    public final static MsgType_1_1 CancelRequest = new MsgType_1_1(_CancelRequest);
    public final static int _LocateRequest = 3;
    public final static MsgType_1_1 LocateRequest = new MsgType_1_1(_LocateRequest);
    public final static int _LocateReply = 4;
    public final static MsgType_1_1 LocateReply = new MsgType_1_1(_LocateReply);
    public final static int _CloseConnection = 5;
    public final static MsgType_1_1 CloseConnection = new MsgType_1_1(_CloseConnection);
    public final static int _MessageError = 6;
    public final static MsgType_1_1 MessageError = new MsgType_1_1(_MessageError);
    public final static int _Fragment = 7;
    public final static MsgType_1_1 Fragment = new MsgType_1_1(_Fragment);

    private MsgType_1_1(int value) {
        values_[value] = this;
        value_ = value;
    }

    public int value()
    {
        return value_;
    }

    public static MsgType_1_1 from_int(int value) {
        if (value >= values_.length) throw new BAD_PARAM("Value (" + value + ") out of range", 25, COMPLETED_NO);
        return values_[value];
    }

    private Object
    readResolve() {
        return from_int(value());
    }
}
