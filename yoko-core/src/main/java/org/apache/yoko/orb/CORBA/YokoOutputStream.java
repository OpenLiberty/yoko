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
package org.apache.yoko.orb.CORBA;

import org.apache.yoko.codecs.CharCodec;
import org.apache.yoko.codecs.WcharCodec;
import org.apache.yoko.io.AlignmentBoundary;
import org.apache.yoko.io.Buffer;
import org.apache.yoko.io.ReadBuffer;
import org.apache.yoko.io.SimplyCloseable;
import org.apache.yoko.io.WriteBuffer;
import org.apache.yoko.orb.OB.CodecPair;
import org.apache.yoko.orb.OB.ORBInstance;
import org.apache.yoko.orb.OB.TypeCodeFactory;
import org.apache.yoko.orb.OB.ValueWriter;
import org.apache.yoko.orb.OCI.GiopVersion;
import org.apache.yoko.util.Assert;
import org.apache.yoko.util.Timeout;
import org.omg.CORBA.Any;
import org.omg.CORBA.BAD_TYPECODE;
import org.omg.CORBA.LocalObject;
import org.omg.CORBA.MARSHAL;
import org.omg.CORBA.NO_IMPLEMENT;
import org.omg.CORBA.Principal;
import org.omg.CORBA.TIMEOUT;
import org.omg.CORBA.TypeCode;
import org.omg.CORBA.TypeCodePackage.BadKind;
import org.omg.CORBA.TypeCodePackage.Bounds;
import org.omg.CORBA.ValueBaseHelper;
import org.omg.CORBA.portable.BoxedValueHelper;
import org.omg.CORBA.portable.ValueOutputStream;
import org.omg.CORBA_2_3.portable.OutputStream;
import org.omg.IOP.IOR;
import org.omg.IOP.IORHelper;
import org.omg.IOP.TaggedProfile;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import static java.util.logging.Level.FINEST;
import static org.apache.yoko.io.AlignmentBoundary.EIGHT_BYTE_BOUNDARY;
import static org.apache.yoko.io.AlignmentBoundary.FOUR_BYTE_BOUNDARY;
import static org.apache.yoko.io.AlignmentBoundary.NO_BOUNDARY;
import static org.apache.yoko.io.AlignmentBoundary.TWO_BYTE_BOUNDARY;
import static org.apache.yoko.logging.VerboseLogging.DATA_OUT_LOG;
import static org.apache.yoko.orb.OCI.GiopVersion.GIOP1_0;
import static org.apache.yoko.util.MinorCodes.MinorIncompleteTypeCode;
import static org.apache.yoko.util.MinorCodes.MinorLocalObject;
import static org.apache.yoko.util.MinorCodes.MinorOther;
import static org.apache.yoko.util.MinorCodes.MinorReadInvTypeCodeIndirection;
import static org.apache.yoko.util.MinorCodes.describeBadTypecode;
import static org.apache.yoko.util.MinorCodes.describeMarshal;
import static org.omg.CORBA.CompletionStatus.COMPLETED_NO;
import static org.omg.CORBA.CompletionStatus.COMPLETED_YES;
import static org.omg.CORBA.TCKind._tk_Principal;
import static org.omg.CORBA.TCKind._tk_TypeCode;
import static org.omg.CORBA.TCKind._tk_abstract_interface;
import static org.omg.CORBA.TCKind._tk_alias;
import static org.omg.CORBA.TCKind._tk_any;
import static org.omg.CORBA.TCKind._tk_array;
import static org.omg.CORBA.TCKind._tk_boolean;
import static org.omg.CORBA.TCKind._tk_char;
import static org.omg.CORBA.TCKind._tk_double;
import static org.omg.CORBA.TCKind._tk_enum;
import static org.omg.CORBA.TCKind._tk_except;
import static org.omg.CORBA.TCKind._tk_fixed;
import static org.omg.CORBA.TCKind._tk_float;
import static org.omg.CORBA.TCKind._tk_long;
import static org.omg.CORBA.TCKind._tk_longdouble;
import static org.omg.CORBA.TCKind._tk_longlong;
import static org.omg.CORBA.TCKind._tk_native;
import static org.omg.CORBA.TCKind._tk_null;
import static org.omg.CORBA.TCKind._tk_objref;
import static org.omg.CORBA.TCKind._tk_octet;
import static org.omg.CORBA.TCKind._tk_sequence;
import static org.omg.CORBA.TCKind._tk_short;
import static org.omg.CORBA.TCKind._tk_string;
import static org.omg.CORBA.TCKind._tk_struct;
import static org.omg.CORBA.TCKind._tk_ulong;
import static org.omg.CORBA.TCKind._tk_ulonglong;
import static org.omg.CORBA.TCKind._tk_union;
import static org.omg.CORBA.TCKind._tk_ushort;
import static org.omg.CORBA.TCKind._tk_value;
import static org.omg.CORBA.TCKind._tk_value_box;
import static org.omg.CORBA.TCKind._tk_void;
import static org.omg.CORBA.TCKind._tk_wchar;
import static org.omg.CORBA.TCKind._tk_wstring;
import static org.omg.CORBA.TCKind.tk_null;
import static org.omg.CORBA_2_4.TCKind._tk_local_interface;

public final class YokoOutputStream extends OutputStream implements ValueOutputStream {
    private ORBInstance orbInstance;
    private final WriteBuffer writeBuffer;
    private final GiopVersion giopVersion;
    private final CodecPair codecs;

    // Handles all OBV marshalling
    private ValueWriter valueWriter;

    // In GIOP 1.2, the body must be aligned on an 8-byte boundary.
    // This flag is used to keep track of when that alignment is necessary.
    private boolean atEndOfGiop_1_2_Header = false;

    private Object invocationContext;

    private Object delegateContext;
    private Timeout timeout = Timeout.NEVER;

    private SimplyCloseable recordLength() {
        addCapacity(4, FOUR_BYTE_BOUNDARY);
        return writeBuffer.recordLength();
    }

    private void writeTypeCodeImpl(TypeCode tc, Map<TypeCode, Integer> history) {
        // Try casting the TypeCode to org.apache.yoko.orb.CORBA.TypeCode. This
        // could fail if the TypeCode was created by a foreign singleton ORB.
        TypeCodeImpl obTC = null;
        try {
            obTC = (TypeCodeImpl) tc;
        } catch (ClassCastException ignored) {
        }

        if (obTC != null) {
            if (obTC.recId_ != null) {
                if (obTC.recType_ == null)
                    throw new BAD_TYPECODE(describeBadTypecode(MinorIncompleteTypeCode), MinorIncompleteTypeCode, COMPLETED_NO);
                writeTypeCodeImpl(obTC.recType_, history);
                return;
            }
        }

        if (DATA_OUT_LOG.isLoggable(FINEST)) DATA_OUT_LOG.finest("Writing a type code of type " + (tc == null ? null : tc.kind()));

        // For performance reasons, handle the primitive TypeCodes first
        switch (tc.kind().value()) {
        case _tk_null:
        case _tk_void:
        case _tk_short:
        case _tk_long:
        case _tk_longlong:
        case _tk_ushort:
        case _tk_ulong:
        case _tk_ulonglong:
        case _tk_float:
        case _tk_double:
        case _tk_longdouble:
        case _tk_boolean:
        case _tk_char:
        case _tk_wchar:
        case _tk_octet:
        case _tk_any:
        case _tk_TypeCode:
        case _tk_Principal:
            write_ulong(tc.kind().value());
            return;
        }

        Integer indirectionPos = history.get(tc);
        if (indirectionPos != null) {
            write_long(-1);
            int offs = indirectionPos - writeBuffer.getPosition();
            DATA_OUT_LOG.finest("Writing an indirect type code for offset " + offs);
            write_long(offs);
        } else {
            write_ulong(tc.kind().value());
            Integer oldPos = writeBuffer.getPosition() - 4;

            try {
                switch (tc.kind().value()) {
                case _tk_fixed: {
                    history.put(tc, oldPos);

                    write_ushort(tc.fixed_digits());
                    write_short(tc.fixed_scale());

                    break;
                }

                case _tk_objref:
                case _tk_local_interface:
                case _tk_abstract_interface:
                case _tk_native: {
                    history.put(tc, oldPos);

                    try  (SimplyCloseable ignored = recordLength()) {
                        _OB_writeEndian();
                        write_string(tc.id());
                        write_string(tc.name());
                    }

                    break;
                }

                case _tk_struct:
                case _tk_except: {
                    history.put(tc, oldPos);

                    try  (SimplyCloseable ignored = recordLength()) {
                        _OB_writeEndian();
                        write_string(tc.id());
                        write_string(tc.name());
                        write_ulong(tc.member_count());
                        for (int i = 0; i < tc.member_count(); i++) {
                            write_string(tc.member_name(i));
                            writeTypeCodeImpl(tc.member_type(i), history);
                        }
                    }

                    break;
                }

                case _tk_union: {
                    history.put(tc, oldPos);

                    try  (SimplyCloseable ignored = recordLength()) {
                        _OB_writeEndian();
                        write_string(tc.id());
                        write_string(tc.name());
                        TypeCode discType = tc.discriminator_type();
                        writeTypeCodeImpl(discType, history);
                        int defaultIndex = tc.default_index();
                        write_long(defaultIndex);
                        write_ulong(tc.member_count());
                        for (int i = 0; i < tc.member_count(); i++) {
                            //
                            // Check for default label value
                            //
                            if (i == defaultIndex) {
                                //
                                // Marshal a dummy value of the appropriate size
                                // for the discriminator type
                                //
                                TypeCode origDiscType = TypeCodeImpl._OB_getOrigType(discType);
                                switch (origDiscType.kind().value()) {
                                case _tk_short:
                                    write_short((short) 0);
                                    break;
                                case _tk_ushort:
                                    write_ushort((short) 0);
                                    break;
                                case _tk_long:
                                    write_long(0);
                                    break;
                                case _tk_ulong:
                                case _tk_enum:
                                    write_ulong(0);
                                    break;
                                case _tk_longlong:
                                    write_longlong(0);
                                    break;
                                case _tk_ulonglong:
                                    write_ulonglong(0);
                                    break;
                                case _tk_boolean:
                                    write_boolean(false);
                                    break;
                                case _tk_char:
                                    write_char((char) 0);
                                    break;
                                default:
                                    throw Assert.fail("Invalid sub-type in tk_union");
                                }
                            } else {
                                tc.member_label(i).write_value(this);
                            }

                            write_string(tc.member_name(i));
                            writeTypeCodeImpl(tc.member_type(i), history);
                        }
                    }

                    break;
                }

                case _tk_enum: {
                    history.put(tc, oldPos);

                    try  (SimplyCloseable ignored = recordLength()) {
                        _OB_writeEndian();
                        write_string(tc.id());
                        write_string(tc.name());
                        write_ulong(tc.member_count());
                        for (int i = 0; i < tc.member_count(); i++)
                            write_string(tc.member_name(i));
                    }

                    break;
                }

                case _tk_string:
                case _tk_wstring:
                    write_ulong(tc.length());
                    break;

                case _tk_sequence:
                case _tk_array: {
                    history.put(tc, oldPos);

                    try  (SimplyCloseable ignored = recordLength()) {
                        _OB_writeEndian();
                        writeTypeCodeImpl(tc.content_type(), history);
                        write_ulong(tc.length());
                    }

                    break;
                }

                case _tk_alias:
                case _tk_value_box: {
                    history.put(tc, oldPos);

                    try  (SimplyCloseable ignored = recordLength()) {
                        _OB_writeEndian();
                        write_string(tc.id());
                        write_string(tc.name());
                        writeTypeCodeImpl(tc.content_type(), history);
                    }

                    break;
                }

                case _tk_value: {
                    history.put(tc, oldPos);

                    TypeCode concreteBase = tc.concrete_base_type();
                    if (concreteBase == null) {
                        concreteBase = TypeCodeFactory.createPrimitiveTC(tk_null);
                    }

                    try  (SimplyCloseable ignored = recordLength()) {
                        _OB_writeEndian();
                        write_string(tc.id());
                        write_string(tc.name());
                        write_short(tc.type_modifier());
                        writeTypeCodeImpl(concreteBase, history);
                        write_ulong(tc.member_count());
                        for (int i = 0; i < tc.member_count(); i++) {
                            write_string(tc.member_name(i));
                            writeTypeCodeImpl(tc.member_type(i), history);
                            write_short(tc.member_visibility(i));
                        }
                    }

                    break;
                }

                default:
                    throw Assert.fail("Invalid typecode");
                }
            } catch (BadKind | Bounds ex) {
                throw Assert.fail(ex);
            }
        }
    }

    private ValueWriter valueWriter() {
        return null == valueWriter ? (valueWriter = new ValueWriter(this, writeBuffer)) : valueWriter;
    }

    private void addCapacity(int size) {
        if (atEndOfGiop_1_2_Header) {
            atEndOfGiop_1_2_Header = false;
            addCapacity(size, EIGHT_BYTE_BOUNDARY);
            return;
        }
        // If a new chunk is required, there will be a recursive call to addCapacity().
        if (writeBuffer.isComplete() && valueWriter != null) valueWriter.checkBeginChunk();

        // If there isn't enough room, then reallocate the buffer
        final boolean resized = writeBuffer.ensureAvailable(size);
        if (resized) checkTimeout();
    }

    private void checkTimeout() {
        if (timeout.isExpired()) {
            // we only ever want to throw the exception once
            timeout = Timeout.NEVER;
            throw new TIMEOUT("Reply timed out on server", MinorOther, COMPLETED_YES);
        }
    }

    private void addCapacity(int size, AlignmentBoundary boundary) {
        Assert.ensure(boundary != NO_BOUNDARY);

        // If a new chunk is required, there will be a recursive call to addCapacity().
        if (writeBuffer.isComplete() && valueWriter != null) valueWriter.checkBeginChunk();

        if (atEndOfGiop_1_2_Header) {
            boundary = EIGHT_BYTE_BOUNDARY;
            atEndOfGiop_1_2_Header = false;
        }

        // If there isn't enough room, then reallocate the buffer
        final boolean resized = writeBuffer.ensureAvailable(size, boundary);
        if (resized) checkTimeout();
    }

    public void write(int b) {
        // this matches the behaviour of this function in the Java ORB
        // and not what is outlined in the java.io.OutputStream
        write_long(b);
    }

    public org.omg.CORBA.ORB orb() { return (orbInstance == null) ? null : orbInstance.getORB(); }

    @Override
    public YokoInputStream create_input_stream() {
        YokoInputStream in = new YokoInputStream(getBufferReader(), false, codecs, giopVersion);
        in._OB_ORBInstance(orbInstance);
        return in;
    }

    public void write_boolean(boolean value) {
        addCapacity(1);
        writeBuffer.writeByte(value ? 1 : 0);
    }

    public void write_char(char value) {
        final CharCodec codec = codecs.charCodec;
        addCapacity(codec.octetCount(value));
        codec.writeChar(value, writeBuffer);
    }

    public void write_wchar(char value) {
        final WcharCodec codec = codecs.wcharCodec;

        switch (giopVersion) {
        case GIOP1_0:
        case GIOP1_1:
            // add aligned space for 1 character
            addCapacity(codec.charSize(), TWO_BYTE_BOUNDARY);
            // write 2-byte character in big endian
            codec.writeChar(value, writeBuffer);
            break;

        default:
            // add unaligned space for 1 length-and-character
            addCapacity(codec.octetCountLengthsAndWchars(1));
            codec.writeLengthAndChar(value, writeBuffer);
            break;
        }
    }

    public void write_octet(byte value) {
        addCapacity(1);
        writeBuffer.writeByte(value);
    }

    public void write_short(short value) {
        addCapacity(2, TWO_BYTE_BOUNDARY);
        writeBuffer.writeShort(value);
    }

    public void write_ushort(short value) { write_short(value); }

    public void write_long(int value) {
        addCapacity(4, FOUR_BYTE_BOUNDARY);
        writeBuffer.writeInt(value);
    }

    public void write_ulong(int value) { write_long(value); }

    public void write_longlong(long value) {
        addCapacity(8, EIGHT_BYTE_BOUNDARY);
        writeBuffer.writeLong(value);
    }

    public void write_ulonglong(long value) { write_longlong(value); }

    public void write_float(float value) {
        addCapacity(4, FOUR_BYTE_BOUNDARY);
        writeBuffer.writeFloat(value);
    }

    public void write_double(double value) {
        addCapacity(8, EIGHT_BYTE_BOUNDARY);
        writeBuffer.writeDouble(value);
    }

    public void write_string(String value) {
        final CharCodec codec = codecs.charCodec;
        DATA_OUT_LOG.finest(() -> String.format("Writing string value %s using codec %s", value, codec));
        final char[] arr = value.toCharArray();

        if (codec.isFixedWidth()) {
            int numOctets = arr.length * codec.charSize() + 1;
            write_ulong(numOctets); // writes the length
            addCapacity(numOctets);
            for (char c: arr) codec.writeChar(c, writeBuffer);
            // write null terminator
            writeBuffer.writeByte(0);
        } else {
            // UTF-8 is the only supported non-fixed-width char encoding.
            // Each Java char can require at most 3 bytes of UTF-8;
            // any 4 byte UTF-8 sequence is a surrogate pair in Java.
            // Use a temporary buffer to count bytes needed and allocate them up front.
            // This keeps the data in a single chunk (not mandatory, but sensible).
            final WriteBuffer tmpWriter = Buffer.createWriteBuffer(arr.length * 3 + 1);
            for (char c : arr) codec.writeChar(c, tmpWriter);
            // write the null terminator and compute the length, ignoring any unused space in the buffer
            int numOctets = tmpWriter.writeByte(0).trim().length();
            // write the length
            write_ulong(numOctets);
            // and write the contents
            addCapacity(numOctets);
            tmpWriter.readFromStart().readBytes(writeBuffer);
        }
    }

    public void write_wstring(String value) {
        switch (giopVersion) {
            case GIOP1_0:
            case GIOP1_1:
                write_wstring_pre_1_2(value);
                break;
            default:
                write_wstring_1_2(value);
        }
    }

    private void write_wstring_pre_1_2(String value) {
        final WcharCodec codec = codecs.wcharCodec;
        DATA_OUT_LOG.finest(() -> String.format("Writing GIOP 1.0 wstring value %s using codec %s", value, codec));
        // write the length of the string in chars
        write_ulong(value.length() + 1);
        // already 4-byte aligned, so just add the needed capacity for len 2-byte chars
        addCapacity(2*(value.length() + 1));
        // now write all the characters
        for (int i = 0; i < value.length(); i++) codec.writeChar(value.charAt(i), writeBuffer);
        // and the null terminator
        codec.writeChar((char) 0, writeBuffer);
    }

    private void write_wstring_1_2(String value) {
        // GIOP 1.2 encodes the length of the string in octets and does not require a null terminator
        // first deal with the empty string case
        if (value.isEmpty()) {
            DATA_OUT_LOG.finest(() -> "Writing GIOP 1.2 empty string");
            write_ulong(0);
            return;
        }
        // now we know there is a first character
        final WcharCodec codec = codecs.wcharCodec;
        int numOctets = codec.octetCount(value);
        DATA_OUT_LOG.finest(() -> String.format("Writing GIOP 1.2 wstring value %s using codec %s using %d byte(s)", value, codec, numOctets));
        // write the length of the string in octets
        write_ulong(numOctets);
        // add unaligned capacity
        addCapacity(numOctets);
        // write the first character, including optional BOM
        codec.beginToWriteString(value.charAt(0), writeBuffer);
        // write the rest of the characters
        for (int i = 1; i < value.length(); i++) codec.writeChar(value.charAt(i), writeBuffer);
    }

    public void write_boolean_array(boolean[] value, int offset, int length) {
        if (length > 0) {
            addCapacity(length);

            for (int i = offset; i < offset + length; i++)
                writeBuffer.writeByte(value[i] ? 1 : 0);
        }
    }

    public void write_char_array(char[] value, int offset, int length) {
        for (int i = offset; i < offset + length; i++) write_char(value[i]);
    }

    public void write_wchar_array(char[] value, int offset, int length) {
        for (int i = offset; i < offset + length; i++) write_wchar(value[i]);
    }

    public void write_octet_array(byte[] value, int offset, int length) {
        if (length <= 0) return;
        addCapacity(length);
        writeBuffer.writeBytes(value, offset, length);
    }

    public void write_short_array(short[] value, int offset, int length) {
        if (length > 0) {
            addCapacity(length * 2, TWO_BYTE_BOUNDARY);

            for (int i = offset; i < offset + length; i++) {
                writeBuffer.writeShort(value[i]);
            }
        }
    }

    public void write_ushort_array(short[] value, int offset, int length) {
        write_short_array(value, offset, length);
    }

    public void write_long_array(int[] value, int offset, int length) {
        if (length > 0) {
            addCapacity(length * 4, FOUR_BYTE_BOUNDARY);

            for (int i = offset; i < offset + length; i++) {
                writeBuffer.writeInt(value[i]);
            }
        }
    }

    public void write_ulong_array(int[] value, int offset, int length) {
        write_long_array(value, offset, length);
    }

    public void write_longlong_array(long[] value, int offset, int length) {
        if (length > 0) {
            addCapacity(length * 8, EIGHT_BYTE_BOUNDARY);

            for (int i = offset; i < offset + length; i++) {
                writeBuffer.writeLong(value[i]);
            }
        }
    }

    public void write_ulonglong_array(long[] value, int offset, int length) {
        write_longlong_array(value, offset, length);
    }

    public void write_float_array(float[] value, int offset, int length) {
        if (length > 0) {
            addCapacity(length * 4, FOUR_BYTE_BOUNDARY);

            for (int i = offset; i < offset + length; i++) {
                int v = Float.floatToIntBits(value[i]);

                writeBuffer.writeInt(v);
            }
        }
    }

    public void write_double_array(double[] value, int offset, int length) {
        if (length > 0) {
            addCapacity(length * 8, EIGHT_BYTE_BOUNDARY);

            for (int i = offset; i < offset + length; i++) {
                long v = Double.doubleToLongBits(value[i]);

                writeBuffer.writeLong(v);
            }
        }
    }

    public void write_Object(org.omg.CORBA.Object value) {
        if (value == null) {
            DATA_OUT_LOG.finest("Writing a null CORBA object value");
            IOR ior = new IOR();
            ior.type_id = "";
            ior.profiles = new TaggedProfile[0];
            IORHelper.write(this, ior);
        } else {
            if (value instanceof LocalObject)
                throw new MARSHAL(describeMarshal(MinorLocalObject), MinorLocalObject, COMPLETED_NO);

            Delegate p = (Delegate) ((org.omg.CORBA.portable.ObjectImpl) value)._get_delegate();

            p._OB_marshalOrigIOR(this);
        }
    }

    public void write_TypeCode(TypeCode t) {
        // NOTE:
        // No data with natural alignment of greater than four octets
        // is needed for TypeCode. Therefore, it is not necessary to do
        // encapsulation in a separate buffer.

        if (t == null) throw new BAD_TYPECODE("TypeCode is nil");

        writeTypeCodeImpl(t, new HashMap<TypeCode, Integer>());
    }

    public void write_any(Any value) {
        DATA_OUT_LOG.finest("Writing an ANY value of type " + value.type().kind());
        write_TypeCode(value.type());
        value.write_value(this);
    }

    public void write_Context(org.omg.CORBA.Context ctx, org.omg.CORBA.ContextList contexts) {
        int count = contexts.count();
        Vector v = new Vector();
        Context ctxImpl = (Context) ctx;
        for (int i = 0; i < count; i++) {
            try {
                String pattern = contexts.item(i);
                ctxImpl._OB_getValues("", 0, pattern, v);
            } catch (org.omg.CORBA.Bounds ex) {
                throw Assert.fail(ex);
            }
        }

        write_ulong(v.size());

        Enumeration e = v.elements();
        while (e.hasMoreElements())
            write_string((String) e.nextElement());
    }

    @SuppressWarnings("deprecation")
    public void write_Principal(Principal value) {
        // Deprecated by CORBA 2.2
        throw new NO_IMPLEMENT();
    }

    public void write_fixed(BigDecimal value) {
        String v = value.abs().toString();

        // Append coded sign to value string
        v += getSignChar(value);

        if ((v.length() & 1) != 0)
            v = "0" + v;

        for (int i = 0; i < v.length(); i += 2) {
            char c1 = v.charAt(i);
            char c2 = v.charAt(i + 1);
            write_octet((byte) ((c1 - '0') << 4 | (c2 - '0')));
        }
    }

    private static int getSignChar(BigDecimal value) {
        return '0' + (char) (value.signum() == -1 ? 0x0d : 0x0c);
    }

    public void write_value(Serializable value) {
        checkTimeout();
        valueWriter().writeValue(value, null);
        checkTimeout();
    }

    public void write_value(Serializable value, String rep_id) {
        checkTimeout();
        valueWriter().writeValue(value, rep_id);
        checkTimeout();
    }

    public void write_value(Serializable value, Class clz) {
        checkTimeout();
        valueWriter().writeValue(value, null);
        checkTimeout();

    }

    public void write_value(Serializable value, BoxedValueHelper helper) {
        checkTimeout();
        valueWriter().writeValueBox(value, null, helper);
        checkTimeout();
    }

    public void write_abstract_interface(Object obj) {
        checkTimeout();
        valueWriter().writeAbstractInterface(obj);
        checkTimeout();
    }

    // ------------------------------------------------------------------
    // Additional Yoko specific functions
    // ------------------------------------------------------------------

    public void write_value(Serializable value, TypeCode tc, BoxedValueHelper helper) {
        checkTimeout();
        valueWriter().writeValueBox(value, tc, helper);
        checkTimeout();
    }

    public void write_InputStream(final org.omg.CORBA.portable.InputStream in, TypeCode tc) {
        try {
            DATA_OUT_LOG.fine("writing a value of type " + tc.kind().value());

            switch (tc.kind().value()) {
                case _tk_null:
                case _tk_void:
                    break;

                case _tk_short:
                case _tk_ushort:
                    write_short(in.read_short());
                    break;

                case _tk_long:
                case _tk_ulong:
                case _tk_float:
                case _tk_enum:
                    write_long(in.read_long());
                    break;

                case _tk_double:
                case _tk_longlong:
                case _tk_ulonglong:
                    write_longlong(in.read_longlong());
                    break;

                case _tk_boolean:
                case _tk_octet:
                    write_octet(in.read_octet());
                    break;

                case _tk_char:
                    write_char(in.read_char());
                    break;

                case _tk_wchar:
                    write_wchar(in.read_wchar());
                    break;

                case _tk_fixed:
                    write_fixed(in.read_fixed());
                    break;

                case _tk_any:
                    copyAnyFrom(in);
                    break;

                case _tk_TypeCode:
                    copyTypeCodeFrom(in);
                    break;

                case _tk_Principal:
                    write_Principal(in.read_Principal());
                    break;

                case _tk_objref:
                    copyObjRefFrom(in);
                    break;

                case _tk_struct:
                    for (int i = 0; i < tc.member_count(); i++)
                        write_InputStream(in, tc.member_type(i));
                    break;

                case _tk_except:
                    write_string(in.read_string());
                    for (int i = 0; i < tc.member_count(); i++)
                        write_InputStream(in, tc.member_type(i));
                    break;

                case _tk_union:
                    copyUnionFrom(in, tc);
                    break;

                case _tk_string:
                    write_string(in.read_string());
                    break;

                case _tk_wstring:
                    write_wstring(in.read_wstring());
                    break;

                case _tk_sequence:
                case _tk_array:
                    copyArrayFrom(in, tc);
                    break;

                case _tk_alias:
                    write_InputStream(in, tc.content_type());
                    break;

                case _tk_value:
                case _tk_value_box:
                    copyValueFrom((org.omg.CORBA_2_3.portable.InputStream) in, tc);
                    break;

                case _tk_abstract_interface:
                    copyAbstractInterfaceFrom(in);
                    break;

                case _tk_local_interface:
                case _tk_native:
                default:
                    throw Assert.fail("unsupported types");
            }
        } catch (BadKind | Bounds ex) {
            throw Assert.fail(ex);
        }
    }

    private void copyObjRefFrom(org.omg.CORBA.portable.InputStream in) {
        // Don't do this: write_Object(in.read_Object())
        // This is faster:
        IOR ior = IORHelper.read(in);
        IORHelper.write(this, ior);
    }

    private void copyAnyFrom(org.omg.CORBA.portable.InputStream in) {
        // Don't do this: write_any(in.read_any())
        // This is faster:
        TypeCode p = in.read_TypeCode();
        write_TypeCode(p);
        write_InputStream(in, p);
    }

    private void copyValueFrom(org.omg.CORBA_2_3.portable.InputStream in, TypeCode tc) {
        if (in instanceof YokoInputStream) {
            ((YokoInputStream)in)._OB_remarshalValue(tc, this);
        } else {
            write_value(in.read_value());
        }
    }

    private void copyAbstractInterfaceFrom(org.omg.CORBA.portable.InputStream in) {
        boolean b = in.read_boolean();
        write_boolean(b);
        if (b) {
            write_Object(in.read_Object());
        } else if (in instanceof YokoInputStream) {
            // We have no TypeCode information about the
            // valuetype, so we must use _tc_ValueBase and
            // rely on the type information sent on the wire
            ((YokoInputStream) in)._OB_remarshalValue(ValueBaseHelper.type(), this);
        } else {
            write_value(((org.omg.CORBA_2_3.portable.InputStream) in).read_value());
        }
    }

    private void copyArrayFrom(org.omg.CORBA.portable.InputStream in, TypeCode tc) throws BadKind {
        final boolean swapInput = (in instanceof YokoInputStream) && ((YokoInputStream)in).swapBytes;
        int len;

        if (tc.kind().value() == _tk_sequence) {
            len = in.read_ulong();
            write_ulong(len);
        } else {
            len = tc.length();
        }

        if (len <= 0) return;

        TypeCode origContentType = TypeCodeImpl._OB_getOrigType(tc.content_type());

        switch (origContentType.kind().value()) {
        case _tk_null:
        case _tk_void:
            break;

        case _tk_short:
        case _tk_ushort:
            copyShortArrayFrom(in, len, swapInput);
            break;

            case _tk_long:
        case _tk_ulong:
        case _tk_float:
            copyIntArrayFrom(in, len, swapInput);
            break;

            case _tk_double:
        case _tk_longlong:
        case _tk_ulonglong:
            copyLongArrayFrom(in, len, swapInput);
            break;

            case _tk_boolean:
        case _tk_octet:
            readFrom(in, len);
            break;

        case _tk_char:
            char[] ch = new char[len];
            in.read_char_array(ch, 0, len);
            write_char_array(ch, 0, len);
            break;

        case _tk_wchar: {
            char[] wch = new char[len];
            in.read_wchar_array(wch, 0, len);
            write_wchar_array(wch, 0, len);
            break;
        }

        case _tk_alias:
            throw Assert.fail("tk_alias not supported in tk_array or tk_sequence");

        default:
            for (int i = 0; i < len; i++)
                write_InputStream(in, tc.content_type());
            break;
        }
    }

    private void copyLongArrayFrom(org.omg.CORBA.portable.InputStream in, int num, boolean swapInput) {
        if (swapInput) {
            long[] l = new long[num];
            in.read_longlong_array(l, 0, num);
            write_longlong_array(l, 0, num);
        } else {
            // Read one value for the alignment
            write_longlong(in.read_longlong());
            final int n = 8 * (num - 1);
            if (n > 0) {
                // Copy the rest
                readFrom(in, n);
            }
        }
    }

    private void copyIntArrayFrom(org.omg.CORBA.portable.InputStream in, int num, boolean swapInput) {
        if (swapInput) {
            int[] i = new int[num];
            in.read_long_array(i, 0, num);
            write_long_array(i, 0, num);
        } else {
            // Read one value for the alignment
            write_long(in.read_long());
            final int n = 4 * (num - 1);

            if (n > 0) {
                // Copy the rest
                readFrom(in, n);
            }
        }
    }

    private void copyShortArrayFrom(org.omg.CORBA.portable.InputStream in, int num, boolean swapInput) {
        if (swapInput) {
            short[] s = new short[num];
            in.read_short_array(s, 0, num);
            write_short_array(s, 0, num);
        } else {
            // Read one value for the alignment
            write_short(in.read_short());
            final int n = 2 * (num - 1);

            if (n > 0) {
                // Copy the rest
                readFrom(in, n);
            }
        }
    }

    private void copyUnionFrom(org.omg.CORBA.portable.InputStream in, TypeCode tc) throws BadKind, Bounds {
        int defaultIndex = tc.default_index();
        int memberIndex = -1;

        TypeCode origDiscType = TypeCodeImpl._OB_getOrigType(tc.discriminator_type());

        switch (origDiscType.kind().value()) {
        case _tk_short: {
            short val = in.read_short();
            write_short(val);

            for (int i = 0; i < tc.member_count(); i++)
                if (i != defaultIndex) {
                    if (val == tc.member_label(i).extract_short()) {
                        memberIndex = i;
                        break;
                    }
                }

            break;
        }

        case _tk_ushort: {
            short val = in.read_ushort();
            write_ushort(val);

            for (int i = 0; i < tc.member_count(); i++)
                if (i != defaultIndex) {
                    if (val == tc.member_label(i).extract_ushort()) {
                        memberIndex = i;
                        break;
                    }
                }

            break;
        }

        case _tk_long: {
            int val = in.read_long();
            write_long(val);

            for (int i = 0; i < tc.member_count(); i++)
                if (i != defaultIndex) {
                    if (val == tc.member_label(i).extract_long()) {
                        memberIndex = i;
                        break;
                    }
                }

            break;
        }

        case _tk_ulong: {
            int val = in.read_ulong();
            write_ulong(val);

            for (int i = 0; i < tc.member_count(); i++)
                if (i != defaultIndex) {
                    if (val == tc.member_label(i).extract_ulong()) {
                        memberIndex = i;
                        break;
                    }
                }

            break;
        }

        case _tk_longlong: {
            long val = in.read_longlong();
            write_longlong(val);

            for (int i = 0; i < tc.member_count(); i++)
                if (i != defaultIndex) {
                    if (val == tc.member_label(i).extract_longlong()) {
                        memberIndex = i;
                        break;
                    }
                }

            break;
        }

        case _tk_ulonglong: {
            long val = in.read_ulonglong();
            write_ulonglong(val);

            for (int i = 0; i < tc.member_count(); i++)
                if (i != defaultIndex) {
                    if (val == tc.member_label(i).extract_ulonglong()) {
                        memberIndex = i;
                        break;
                    }
                }

            break;
        }

        case _tk_char: {
            char val = in.read_char();
            write_char(val);

            for (int i = 0; i < tc.member_count(); i++)
                if (i != defaultIndex) {
                    if (val == tc.member_label(i).extract_char()) {
                        memberIndex = i;
                        break;
                    }
                }

            break;
        }

        case _tk_boolean: {
            boolean val = in.read_boolean();
            write_boolean(val);

            for (int i = 0; i < tc.member_count(); i++)
                if (i != defaultIndex) {
                    if (val == tc.member_label(i).extract_boolean()) {
                        memberIndex = i;
                        break;
                    }
                }

            break;
        }

        case _tk_enum: {
            int val = in.read_long();
            write_long(val);

            for (int i = 0; i < tc.member_count(); i++)
                if (i != defaultIndex) {
                    if (val == tc.member_label(i).create_input_stream().read_long()) {
                        memberIndex = i;
                        break;
                    }
                }

            break;
        }

        default:
            throw Assert.fail("Invalid typecode in tk_union");
        }

        if (memberIndex >= 0)
            write_InputStream(in, tc.member_type(memberIndex));
        else if (defaultIndex >= 0)
            write_InputStream(in, tc.member_type(defaultIndex));

    }

    private void copyTypeCodeFrom(org.omg.CORBA.portable.InputStream in) {
        // Don't do this: write_TypeCode(in.read_TypeCode())
        // This is faster:

        int kind = in.read_ulong();

        //
        // An indirection is not permitted at this level
        //
        if (kind == -1) {
            throw new MARSHAL(
                    describeMarshal(MinorReadInvTypeCodeIndirection),
                    MinorReadInvTypeCodeIndirection,
                    COMPLETED_NO);
        }

        write_ulong(kind);

        switch (kind) {
        case _tk_null:
        case _tk_void:
        case _tk_short:
        case _tk_long:
        case _tk_ushort:
        case _tk_ulong:
        case _tk_float:
        case _tk_double:
        case _tk_boolean:
        case _tk_char:
        case _tk_octet:
        case _tk_any:
        case _tk_TypeCode:
        case _tk_Principal:
        case _tk_longlong:
        case _tk_ulonglong:
        case _tk_longdouble:
        case _tk_wchar:
            break;

        case _tk_fixed:
            write_ushort(in.read_ushort());
            write_short(in.read_short());
            break;

        case _tk_objref:
        case _tk_struct:
        case _tk_union:
        case _tk_enum:
        case _tk_sequence:
        case _tk_array:
        case _tk_alias:
        case _tk_except:
        case _tk_value:
        case _tk_value_box:
        case _tk_abstract_interface:
        case _tk_native:
        case _tk_local_interface: {
            final int len = in.read_ulong();
            write_ulong(len);
            readFrom(in, len);
            break;
        }

        case _tk_string:
        case _tk_wstring: {
            int bound = in.read_ulong();
            write_ulong(bound);
            break;
        }

        default:
            throw new InternalError();
        }
    }

    private void readFrom(org.omg.CORBA.portable.InputStream in, int length) {
        addCapacity(length);
        writeBuffer.readFrom(in);
    }

    public YokoOutputStream() {
        this(Buffer.createWriteBuffer(), null, null);
    }

    public YokoOutputStream(int initialBufferSize) {
        this(Buffer.createWriteBuffer(initialBufferSize), null, null);
    }

    public YokoOutputStream(CodecPair codecs, GiopVersion giopVersion) {
        this(Buffer.createWriteBuffer(), codecs, giopVersion);
    }

    public YokoOutputStream(int initialBufferSize, CodecPair codecs, GiopVersion giopVersion) {
        this(Buffer.createWriteBuffer(initialBufferSize), codecs, giopVersion);
    }

    public YokoOutputStream(WriteBuffer writeBuffer) {
        this(writeBuffer, null, null);
    }

    public YokoOutputStream(WriteBuffer writeBuffer, CodecPair codecs, GiopVersion giopVersion) {
        this.writeBuffer = writeBuffer;
        this.giopVersion = giopVersion == null ? GIOP1_0 : giopVersion;
        this.codecs = CodecPair.createCopy(codecs);
    }

    @Override
    public void close() {}

    boolean writtenBytesEqual(YokoOutputStream that) {
        return writeBuffer.dataEquals(writeBuffer);
    }

    public byte[] copyWrittenBytes() {
        return writeBuffer.trim().readFromStart().copyRemainingBytes();
    }

    public String writtenBytesToAscii() {
        return writeBuffer.trim().readFromStart().toAscii();
    }

    public ReadBuffer getBufferReader() {
        return writeBuffer.readFromStart();
    }

    public int getPosition() {
        return writeBuffer.getPosition();
    }

    public void setPosition(int pos) {
        writeBuffer.setPosition(pos);
    }

    public void markGiop_1_2_HeaderComplete() {
        this.atEndOfGiop_1_2_Header = true;
    }

    public void _OB_writeEndian() {
        write_boolean(false); // false means big endian
    }

    public void _OB_beginValue(int tag, String[] ids, boolean chunked) {
        valueWriter().beginValue(tag, ids, null, chunked);
    }

    public void _OB_endValue() {
        valueWriter().endValue();
    }

    public void _OB_ORBInstance(ORBInstance orbInstance) {
        this.orbInstance = orbInstance;
    }

    public void _OB_invocationContext(Object invocationContext) {
        this.invocationContext = invocationContext;
    }

    public Object _OB_invocationContext() {
        return invocationContext;
    }

    void _OB_delegateContext(Object delegateContext) {
        this.delegateContext = delegateContext;
    }

    Object _OB_delegateContext() {
        return delegateContext;
    }

    @Override
    public void end_value() {
        _OB_endValue();
    }

    @Override
    public void start_value(String rep_id) {
        final int tag = 0x7fffff02;
        final String[] ids = { rep_id };
        _OB_beginValue(tag, ids, true);
    }

    public void setTimeout(Timeout timeout) {
        this.timeout = timeout;
    }
}
