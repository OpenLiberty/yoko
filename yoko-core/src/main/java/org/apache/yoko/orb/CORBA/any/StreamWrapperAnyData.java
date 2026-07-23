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
package org.apache.yoko.orb.CORBA.any;

import org.apache.yoko.orb.CORBA.YokoInputStream;
import org.apache.yoko.orb.CORBA.YokoOutputStream;
import org.apache.yoko.orb.CORBA.TypeCodeImpl;

import org.apache.yoko.orb.OB.ORBInstance;
import org.omg.CORBA.TypeCode;
import org.omg.CORBA_2_3.portable.InputStream;
import org.omg.CORBA_2_3.portable.OutputStream;

/**
 * Immutable typed Any implementation for complex CORBA types (struct, union, sequence, array, except).
 * Unlike other lazy types, this simply wraps a YokoInputStream without extracting any value,
 * matching AnyImpl's behavior for these types.
 */
public final class StreamWrapperAnyData implements YokoAnyData<YokoInputStream> {
    private final TypeCode typeCode;
    private final YokoInputStream stream;

    private StreamWrapperAnyData(TypeCode typeCode, YokoInputStream stream) {
        this.typeCode = typeCode;
        // Defensive copy
        this.stream = new YokoInputStream(stream);
    }

    public static StreamWrapperAnyData from(InputStream is, TypeCode t) {
        if (!(is instanceof YokoInputStream)) {
            throw new IllegalArgumentException("StreamWrapperAnyData requires YokoInputStream");
        }
        return new StreamWrapperAnyData(t, (YokoInputStream) is);
    }

    @Override
    public TypeCode type() {
        return typeCode;
    }

    @Override
    public YokoInputStream extract() {
        return stream;
    }

    @Override
    public void write(OutputStream os) {
        if (!(os instanceof YokoOutputStream)) {
            throw new IllegalArgumentException("StreamWrapperAnyData requires YokoOutputStream");
        }
        YokoOutputStream yos = (YokoOutputStream) os;
        stream._OB_reset();
        yos.write_InputStream(stream, typeCode);
    }

    @Override
    public InputStream toInputStream(java.util.function.Supplier<OutputStream> outputStreamSupplier) {
        stream._OB_reset();
        return new YokoInputStream(stream);
    }
}
