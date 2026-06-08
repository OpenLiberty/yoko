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

import org.omg.CORBA.BAD_OPERATION;
import org.omg.CORBA.TypeCode;
import org.omg.CORBA_2_3.portable.InputStream;
import org.omg.CORBA_2_3.portable.OutputStream;
import org.omg.CORBA.portable.Streamable;

import java.util.function.Supplier;

/**
 * Immutable typed Any implementation for CORBA Streamable values with lazy extraction.
 * The value is extracted from the input stream only when needed.
 */
public final class StreamableAnyData extends LazyAnyData<Streamable> {

    // Constructor for of() - value provided, stream created lazily
    private StreamableAnyData(TypeCode typeCode, Streamable streamableValue, Supplier<OutputStream> outputStreamSupplier) {
        super(typeCode, streamableValue, true, val -> {
            YokoOutputStream os = (YokoOutputStream) outputStreamSupplier.get();
            val._write(os);
            return os.create_input_stream();
        });
    }

    // Constructor for from() - stream provided, value extracted lazily
    private StreamableAnyData(TypeCode typeCode, YokoInputStream stream, Streamable streamable) {
        super(typeCode, stream, is -> {
            streamable._read(is);
            return streamable;
        });
    }

    public static StreamableAnyData of(Streamable value, TypeCode typeCode, Supplier<OutputStream> outputStreamSupplier) {
        return new StreamableAnyData(typeCode, value, outputStreamSupplier);
    }

    public static StreamableAnyData from(InputStream is, TypeCode t, Streamable streamable) {
        if (!(is instanceof YokoInputStream)) {
            throw new BAD_OPERATION("StreamableAnyData requires YokoInputStream");
        }
        return new StreamableAnyData(t, (YokoInputStream) is, streamable);
    }

    @Override
    protected void writeValueFallback(OutputStream os) {
        extract()._write(os);
    }

    @Override
    public Streamable extract_Streamable() {
        return extract();
    }
}
