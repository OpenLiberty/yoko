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

import org.omg.CORBA.Any;
import org.omg.CORBA.BAD_OPERATION;
import org.omg.CORBA.TypeCode;
import org.omg.CORBA_2_3.portable.InputStream;
import org.omg.CORBA_2_3.portable.OutputStream;
import org.omg.CORBA.portable.Streamable;

import java.io.Serializable;
import java.math.BigDecimal;

import static org.apache.yoko.util.MinorCodes.MinorTypeMismatch;
import static org.apache.yoko.util.MinorCodes.describeBadOperation;
import static org.omg.CORBA.CompletionStatus.COMPLETED_NO;

/**
 * Generic interface for typed Any implementations that are held within a wrapper.
 * The wrapper provides ORBInstance and stream creation, while typed implementations
 * focus on type-specific value storage and operations.
 * <p>
 * Provides default implementations that throw BAD_OPERATION for type mismatches.
 * Specialized Any types override the extract method for their type to delegate to extract().
 *
 * @param <T> The Java type that this Any holds
 */
public interface YokoAnyData<T> {

    /**
     * Returns the TypeCode for this typed Any.
     */
    TypeCode type();

    /**
     * Extracts the value from this typed Any.
     * @return the value held by this Any
     */
    T extract();


    /**
     * Writes the value to the output stream.
     */
    void write(OutputStream os);

    static BAD_OPERATION newMismatchBadOp() {
        return new BAD_OPERATION(describeBadOperation(MinorTypeMismatch), MinorTypeMismatch, COMPLETED_NO);
    }

    // Default implementations that throw BAD_OPERATION for type mismatch

    default short extract_short() throws BAD_OPERATION { throw newMismatchBadOp(); }
    default int extract_long() throws BAD_OPERATION { throw newMismatchBadOp(); }
    default long extract_longlong() throws BAD_OPERATION { throw newMismatchBadOp(); }
    default short extract_ushort() throws BAD_OPERATION { throw newMismatchBadOp(); }
    default int extract_ulong() throws BAD_OPERATION { throw newMismatchBadOp(); }
    default long extract_ulonglong() throws BAD_OPERATION { throw newMismatchBadOp(); }
    default float extract_float() throws BAD_OPERATION { throw newMismatchBadOp(); }
    default double extract_double() throws BAD_OPERATION { throw newMismatchBadOp(); }
    default boolean extract_boolean() throws BAD_OPERATION { throw newMismatchBadOp(); }
    default char extract_char() throws BAD_OPERATION { throw newMismatchBadOp(); }
    default char extract_wchar() throws BAD_OPERATION { throw newMismatchBadOp(); }
    default byte extract_octet() throws BAD_OPERATION { throw newMismatchBadOp(); }
    default Any extract_any() throws BAD_OPERATION { throw newMismatchBadOp(); }
    default org.omg.CORBA.Object extract_Object() throws BAD_OPERATION { throw newMismatchBadOp(); }
    default String extract_string() throws BAD_OPERATION { throw newMismatchBadOp(); }
    default String extract_wstring() throws BAD_OPERATION { throw newMismatchBadOp(); }
    default TypeCode extract_TypeCode() throws BAD_OPERATION { throw newMismatchBadOp(); }
    default Serializable extract_Value() throws BAD_OPERATION { throw newMismatchBadOp(); }
    default Streamable extract_Streamable() { throw newMismatchBadOp(); }
    default BigDecimal extract_fixed() { throw newMismatchBadOp(); }

    /**
     * Creates an InputStream for this typed Any's value.
     * Default implementation serializes via write (suitable for eager types).
     * Lazy types should override to return their cached stream with defensive copy.
     *
     * @param outputStreamSupplier supplier for creating output streams with proper ORB configuration
     * @return an InputStream containing this Any's serialized value
     */
    default InputStream toInputStream(java.util.function.Supplier<OutputStream> outputStreamSupplier) {
        YokoOutputStream out = (YokoOutputStream) outputStreamSupplier.get();
        write(out);
        return out.create_input_stream();
    }
}
