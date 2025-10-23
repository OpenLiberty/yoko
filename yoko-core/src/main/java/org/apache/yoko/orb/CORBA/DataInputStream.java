/*
 * Copyright 2025 IBM Corporation and others.
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
package org.apache.yoko.orb.CORBA;

import java.io.Serializable;

import org.omg.CORBA.Any;
import org.omg.CORBA.AnySeqHolder;
import org.omg.CORBA.BooleanSeqHolder;
import org.omg.CORBA.CharSeqHolder;
import org.omg.CORBA.DoubleSeqHolder;
import org.omg.CORBA.FloatSeqHolder;
import org.omg.CORBA.LongLongSeqHolder;
import org.omg.CORBA.LongSeqHolder;
import org.omg.CORBA.NO_IMPLEMENT;
import org.omg.CORBA.OctetSeqHolder;
import org.omg.CORBA.ShortSeqHolder;
import org.omg.CORBA.TypeCode;
import org.omg.CORBA.ULongLongSeqHolder;
import org.omg.CORBA.ULongSeqHolder;
import org.omg.CORBA.UShortSeqHolder;
import org.omg.CORBA.WCharSeqHolder;

final public class DataInputStream implements org.omg.CORBA.DataInputStream {
    private YokoInputStream in_;

    // ------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------

    public DataInputStream(YokoInputStream in) {
        in_ = in;
    }

    // ------------------------------------------------------------------
    // Standard IDL to Java Mapping
    // ------------------------------------------------------------------

    public String[] _truncatable_ids() {
        throw new NO_IMPLEMENT();
    }

    public Any read_any() {
        return in_.read_any();
    }

    public boolean read_boolean() {
        return in_.read_boolean();
    }

    public char read_char() {
        return in_.read_char();
    }

    public char read_wchar() {
        return in_.read_wchar();
    }

    public byte read_octet() {
        return in_.read_octet();
    }

    public short read_short() {
        return in_.read_short();
    }

    public short read_ushort() {
        return in_.read_ushort();
    }

    public int read_long() {
        return in_.read_long();
    }

    public int read_ulong() {
        return in_.read_ulong();
    }

    public long read_longlong() {
        return in_.read_longlong();
    }

    public long read_ulonglong() {
        return in_.read_ulonglong();
    }

    public float read_float() {
        return in_.read_float();
    }

    public double read_double() {
        return in_.read_double();
    }

    public String read_string() {
        return in_.read_string();
    }

    public String read_wstring() {
        return in_.read_wstring();
    }

    public org.omg.CORBA.Object read_Object() {
        return in_.read_Object();
    }

    public Object read_Abstract() {
        return in_.read_abstract_interface();
    }

    public Serializable read_Value() {
        return in_.read_value();
    }

    public TypeCode read_TypeCode() {
        return in_.read_TypeCode();
    }

    public void read_any_array(AnySeqHolder seq, int offset,
            int length) {
        for (int i = offset; i < offset + length; i++)
            seq.value[i] = in_.read_any();
    }

    public void read_boolean_array(BooleanSeqHolder seq,
            int offset, int length) {
        in_.read_boolean_array(seq.value, offset, length);
    }

    public void read_char_array(CharSeqHolder seq, int offset,
            int length) {
        in_.read_char_array(seq.value, offset, length);
    }

    public void read_wchar_array(WCharSeqHolder seq, int offset,
            int length) {
        in_.read_wchar_array(seq.value, offset, length);
    }

    public void read_octet_array(OctetSeqHolder seq, int offset,
            int length) {
        in_.read_octet_array(seq.value, offset, length);
    }

    public void read_short_array(ShortSeqHolder seq, int offset,
            int length) {
        in_.read_short_array(seq.value, offset, length);
    }

    public void read_ushort_array(UShortSeqHolder seq,
            int offset, int length) {
        in_.read_ushort_array(seq.value, offset, length);
    }

    public void read_long_array(LongSeqHolder seq, int offset,
            int length) {
        in_.read_long_array(seq.value, offset, length);
    }

    public void read_ulong_array(ULongSeqHolder seq, int offset,
            int length) {
        in_.read_ulong_array(seq.value, offset, length);
    }

    public void read_ulonglong_array(ULongLongSeqHolder seq,
            int offset, int length) {
        in_.read_ulonglong_array(seq.value, offset, length);
    }

    public void read_longlong_array(LongLongSeqHolder seq,
            int offset, int length) {
        in_.read_longlong_array(seq.value, offset, length);
    }

    public void read_float_array(FloatSeqHolder seq, int offset,
            int length) {
        in_.read_float_array(seq.value, offset, length);
    }

    public void read_double_array(DoubleSeqHolder seq,
            int offset, int length) {
        in_.read_double_array(seq.value, offset, length);
    }
}
