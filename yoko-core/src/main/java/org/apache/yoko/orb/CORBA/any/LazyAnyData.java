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

import org.apache.yoko.util.concurrent.LazyReference;
import org.omg.CORBA.TypeCode;
import org.omg.CORBA_2_3.portable.InputStream;
import org.omg.CORBA_2_3.portable.OutputStream;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Abstract base class for YokoAnyData implementations that use lazy evaluation.
 * Provides common infrastructure for types that defer extraction from input streams
 * until the value is actually needed.
 *
 * @param <T> The Java type that this Any holds
 */
public abstract class LazyAnyData<T> implements YokoAnyData<T> {
    protected final TypeCode typeCode;
    protected final LazyReference<T> value;
    protected final LazyReference<YokoInputStream> inputStream;

    /**
     * Constructor for ignored creation (value provided, stream created lazily).
     *
     * @param typeCode the TypeCode for this Any
     * @param value the actual value
     * @param ignored marker to distinguish from lazy constructor (always true)
     * @param streamCreator function to create the input stream from the value
     */
    protected LazyAnyData(TypeCode typeCode, T value, boolean ignored, Function<T, YokoInputStream> streamCreator) {
        this.typeCode = typeCode;
        this.value = new LazyReference<>(() -> value);
        this.inputStream = new LazyReference<>(() -> streamCreator.apply(value));
    }

    /**
     * Constructor for lazy creation (stream provided, value extracted lazily).
     *
     * @param typeCode the TypeCode for this Any
     * @param stream the input stream containing the serialized value
     * @param valueExtractor function to extract the value from the stream
     */
    protected LazyAnyData(TypeCode typeCode, YokoInputStream stream, Function<YokoInputStream, T> valueExtractor) {
        this.typeCode = typeCode;
        // Defensive copy of the input stream
        YokoInputStream copiedStream = new YokoInputStream(stream);
        this.inputStream = new LazyReference<>(() -> copiedStream);
        this.value = new LazyReference<>(() -> {
            copiedStream._OB_reset();
            return valueExtractor.apply(copiedStream);
        });
    }

    @Override
    public TypeCode type() {
        return typeCode;
    }

    @Override
    public T extract() {
        return value.get();
    }

    @Override
    public void write(OutputStream os) {
        if (!(os instanceof YokoOutputStream)) {
            writeValueFallback(os);
            return;
        }
        YokoOutputStream yos = (YokoOutputStream) os;
        YokoInputStream is = inputStream.get();
        is._OB_reset();
        yos.write_InputStream(is, typeCode);
    }

    /**
     * Fallback write implementation for non-Yoko output streams.
     * Subclasses must implement this to write their extracted value.
     */
    protected abstract void writeValueFallback(OutputStream os);

    @Override
    public InputStream toInputStream(Supplier<OutputStream> outputStreamSupplier) {
        YokoInputStream is = inputStream.get();
        is._OB_reset();
        return new YokoInputStream(is);
    }
}
