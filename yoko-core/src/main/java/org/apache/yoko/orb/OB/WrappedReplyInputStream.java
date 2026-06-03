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
package org.apache.yoko.orb.OB;

import org.apache.yoko.orb.CORBA.YokoInputStream;
import org.omg.CORBA.Any;
import org.omg.CORBA.Context;
import org.omg.CORBA.INTERNAL;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Principal;
import org.omg.CORBA.SystemException;
import org.omg.CORBA.TypeCode;
import org.omg.CORBA.portable.BoxedValueHelper;
import org.omg.CORBA_2_3.portable.InputStream;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.function.Supplier;

import static org.apache.yoko.util.Exceptions.as;

/**
 * Wrapper for InputStream that intercepts the first read operation to trigger
 * portable interceptor receive_reply/receive_exception interception points.
 * <p>
 * This ensures interceptors are called AFTER the stub has unmarshalled at least
 * the first value, making the result available to interceptors and ensuring
 * unmarshalling exceptions are properly reported via receive_exception.
 */
final class WrappedReplyInputStream extends InputStream {
    private final YokoInputStream yokoStream;
    private final Downcall downcall;
    private boolean readYet;

    WrappedReplyInputStream(YokoInputStream yokoStream, Downcall downcall) {
        this.yokoStream = yokoStream;
        this.downcall = downcall;
    }

    /**
     * Wraps a read operation with first-read interception logic.
     * Handles both value-returning and void operations uniformly.
     */
    private <T> T wrapRead(Supplier<T> readOp) {
        try {
            T result = readOp.get();
            triggerInterceptorsOnFirstRead();
            return result;
        } catch (SystemException ex) {
            return handleFirstReadException(ex);
        }
    }

    /**
     * Wraps a void read operation (array reads) with first-read interception logic.
     */
    private void wrapVoidRead(Runnable readOp) {
        try {
            readOp.run();
            triggerInterceptorsOnFirstRead();
        } catch (SystemException ex) {
            handleFirstReadException(ex);
        }
    }

    /**
     * Called before the first read operation returns to trigger interceptors.
     * Subsequent reads bypass this check.
     */
    private void triggerInterceptorsOnFirstRead() throws SystemException {
        if (readYet) return;
        readYet = true;

        try {
            downcall.postUnmarshal();
        } catch (LocationForward ex) {
            throw as(INTERNAL::new, ex, "Location forward during postUnmarshal");
        } catch (FailureException ex) {
            throw as(INTERNAL::new, ex, "Failure during postUnmarshal");
        }
    }

    /**
     * Handles SystemException during first read by reporting to interceptors
     * via receive_exception before propagating the exception.
     */
    private <T> T handleFirstReadException(SystemException ex) throws SystemException {
        if (readYet) throw ex;
        readYet = true;
        try {
            downcall.unmarshalEx(ex);
            downcall.postUnmarshal();
            throw ex;
        } catch (LocationForward e) {
            throw as(INTERNAL::new, e, "Location forward during exception handling");
        } catch (FailureException ignored) {
            // FailureException is expected when interceptors detect issues during exception handling
            // Let it propagate naturally - it will be converted to appropriate CORBA exception
            throw ex;
        }
    }

    // Primitive read methods
    @Override public boolean read_boolean() { return wrapRead(yokoStream::read_boolean); }
    @Override public char read_char() { return wrapRead(yokoStream::read_char); }
    @Override public char read_wchar() { return wrapRead(yokoStream::read_wchar); }
    @Override public byte read_octet() { return wrapRead(yokoStream::read_octet); }
    @Override public short read_short() { return wrapRead(yokoStream::read_short); }
    @Override public short read_ushort() { return wrapRead(yokoStream::read_ushort); }
    @Override public int read_long() { return wrapRead(yokoStream::read_long); }
    @Override public int read_ulong() { return wrapRead(yokoStream::read_ulong); }
    @Override public long read_longlong() { return wrapRead(yokoStream::read_longlong); }
    @Override public long read_ulonglong() { return wrapRead(yokoStream::read_ulonglong); }
    @Override public float read_float() { return wrapRead(yokoStream::read_float); }
    @Override public double read_double() { return wrapRead(yokoStream::read_double); }
    @Override public String read_string() { return wrapRead(yokoStream::read_string); }
    @Override public String read_wstring() { return wrapRead(yokoStream::read_wstring); }

    // Array read methods
    @Override public void read_boolean_array(boolean[] v, int o, int l) { wrapVoidRead(() -> yokoStream.read_boolean_array(v, o, l)); }
    @Override public void read_char_array(char[] v, int o, int l) { wrapVoidRead(() -> yokoStream.read_char_array(v, o, l)); }
    @Override public void read_wchar_array(char[] v, int o, int l) { wrapVoidRead(() -> yokoStream.read_wchar_array(v, o, l)); }
    @Override public void read_octet_array(byte[] v, int o, int l) { wrapVoidRead(() -> yokoStream.read_octet_array(v, o, l)); }
    @Override public void read_short_array(short[] v, int o, int l) { wrapVoidRead(() -> yokoStream.read_short_array(v, o, l)); }
    @Override public void read_ushort_array(short[] v, int o, int l) { wrapVoidRead(() -> yokoStream.read_ushort_array(v, o, l)); }
    @Override public void read_long_array(int[] v, int o, int l) { wrapVoidRead(() -> yokoStream.read_long_array(v, o, l)); }
    @Override public void read_ulong_array(int[] v, int o, int l) { wrapVoidRead(() -> yokoStream.read_ulong_array(v, o, l)); }
    @Override public void read_longlong_array(long[] v, int o, int l) { wrapVoidRead(() -> yokoStream.read_longlong_array(v, o, l)); }
    @Override public void read_ulonglong_array(long[] v, int o, int l) { wrapVoidRead(() -> yokoStream.read_ulonglong_array(v, o, l)); }
    @Override public void read_float_array(float[] v, int o, int l) { wrapVoidRead(() -> yokoStream.read_float_array(v, o, l)); }
    @Override public void read_double_array(double[] v, int o, int l) { wrapVoidRead(() -> yokoStream.read_double_array(v, o, l)); }

    // Complex type read methods
    @Override public org.omg.CORBA.Object read_Object() { return wrapRead(yokoStream::read_Object); }
    @Override public TypeCode read_TypeCode() { return wrapRead(yokoStream::read_TypeCode); }
    @Override public Any read_any() { return wrapRead(yokoStream::read_any); }
    @Override @Deprecated public Principal read_Principal() { return wrapRead(yokoStream::read_Principal); }
    @Override public BigDecimal read_fixed() { return wrapRead(yokoStream::read_fixed); }
    @SuppressWarnings("rawtypes")
    @Override public org.omg.CORBA.Object read_Object(Class clz) { return wrapRead(() -> yokoStream.read_Object(clz)); }
    @Override public Context read_Context() { return wrapRead(yokoStream::read_Context); }

    // CORBA 2.3 methods
    @Override public Serializable read_value() { return wrapRead(yokoStream::read_value); }
    @SuppressWarnings("rawtypes")
    @Override public Serializable read_value(Class clz) { return wrapRead(() -> yokoStream.read_value(clz)); }
    @Override public Serializable read_value(BoxedValueHelper factory) { return wrapRead(() -> yokoStream.read_value(factory)); }
    @Override public Serializable read_value(String rep_id) { return wrapRead(() -> yokoStream.read_value(rep_id)); }
    @Override public Serializable read_value(Serializable value) { return wrapRead(() -> yokoStream.read_value(value)); }
    @Override public Object read_abstract_interface() { return wrapRead(yokoStream::read_abstract_interface); }
    @SuppressWarnings("rawtypes")
    @Override public Object read_abstract_interface(Class clz) { return wrapRead(() -> yokoStream.read_abstract_interface(clz)); }

    // java.io.InputStream method - wrapped for transparency
    @Override public int read() { return wrapRead(yokoStream::read); }

    // Passthrough method (no interception needed - doesn't read data)
    @Override public ORB orb() { return yokoStream.orb(); }
}
