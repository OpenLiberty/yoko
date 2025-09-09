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

import org.apache.yoko.util.Assert;
import org.apache.yoko.orb.OB.CodeConverterBase;
import org.apache.yoko.orb.OB.CodeConverters;
import org.apache.yoko.orb.OB.CodeSetWriter;
import org.apache.yoko.orb.OB.ORBInstance;
import org.apache.yoko.orb.OB.TypeCodeFactory;
import org.apache.yoko.orb.OB.ValueWriter;
import org.apache.yoko.io.AlignmentBoundary;
import org.apache.yoko.io.Buffer;
import org.apache.yoko.io.ReadBuffer;
import org.apache.yoko.io.WriteBuffer;
import org.apache.yoko.orb.OCI.GiopVersion;
import org.apache.yoko.io.SimplyCloseable;
import org.apache.yoko.util.Timeout;
import org.omg.CORBA.BAD_TYPECODE;
import org.omg.CORBA.DATA_CONVERSION;
import org.omg.CORBA.LocalObject;
import org.omg.CORBA.MARSHAL;
import org.omg.CORBA.NO_IMPLEMENT;
import org.omg.CORBA.Principal;
import org.omg.CORBA.TIMEOUT;
import org.omg.CORBA.TypeCodePackage.BadKind;
import org.omg.CORBA.TypeCodePackage.Bounds;
import org.omg.CORBA.ValueBaseHelper;
import org.omg.CORBA.portable.BoxedValueHelper;
import org.omg.CORBA.portable.ValueOutputStream;
import org.omg.IOP.IOR;
import org.omg.IOP.IORHelper;
import org.omg.IOP.TaggedProfile;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Vector;
import java.util.logging.Logger;

import static org.apache.yoko.util.Assert.ensure;
import static org.apache.yoko.util.MinorCodes.MinorIncompleteTypeCode;
import static org.apache.yoko.util.MinorCodes.MinorLocalObject;
import static org.apache.yoko.util.MinorCodes.MinorOther;
import static org.apache.yoko.util.MinorCodes.MinorReadInvTypeCodeIndirection;
import static org.apache.yoko.util.MinorCodes.describeBadTypecode;
import static org.apache.yoko.util.MinorCodes.describeMarshal;
import static org.apache.yoko.io.AlignmentBoundary.EIGHT_BYTE_BOUNDARY;
import static org.apache.yoko.io.AlignmentBoundary.FOUR_BYTE_BOUNDARY;
import static org.apache.yoko.io.AlignmentBoundary.NO_BOUNDARY;
import static org.apache.yoko.io.AlignmentBoundary.TWO_BYTE_BOUNDARY;
import static org.apache.yoko.orb.OCI.GiopVersion.GIOP1_0;
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

public final class OutputStream extends org.omg.CORBA_2_3.portable.OutputStream implements ValueOutputStream {
    private static final Logger LOGGER = Logger.getLogger(OutputStream.class.getName());

    private ORBInstance orbInstance_;
    private final WriteBuffer writeBuffer;
    private final GiopVersion giopVersion_;
    private final CodeConverters codeConverters_;
    private final boolean charWriterRequired_;
    private final boolean charConversionRequired_;
    private final boolean wCharWriterRequired_;
    private final boolean wCharConversionRequired_;

    // Handles all OBV marshalling
    private ValueWriter valueWriter_;

    // In GIOP 1.2, the body must be aligned on an 8-byte boundary.
    // This flag is used to keep track of when that alignment is necessary.
    private boolean atEndOfGiop_1_2_Header = false;

    private Object invocationContext_;

    private Object delegateContext_;
    private Timeout timeout = Timeout.NEVER;

    private SimplyCloseable recordLength() {
        addCapacity(4, FOUR_BYTE_BOUNDARY);
        return writeBuffer.recordLength(LOGGER);
    }

    private void writeTypeCodeImpl(org.omg.CORBA.TypeCode tc, Map<org.omg.CORBA.TypeCode, Integer> history) {
        //
        // Try casting the TypeCode to org.apache.yoko.orb.CORBA.TypeCode. This
        // could
        // fail if the TypeCode was created by a foreign singleton ORB.
        //
        TypeCode obTC = null;
        try {
            obTC = (TypeCode) tc;
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

        LOGGER.finest("Writing a type code of type " + tc.kind().value());

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

        Integer indirectionPos = (Integer) history.get(tc);
        if (indirectionPos != null) {
            write_long(-1);
            int offs = indirectionPos - writeBuffer.getPosition();
            LOGGER.finest("Writing an indirect type code for offset " + offs);
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

                    try  (SimplyCloseable sc = recordLength()) {
                        _OB_writeEndian();
                        write_string(tc.id());
                        write_string(tc.name());
                    }

                    break;
                }

                case _tk_struct:
                case _tk_except: {
                    history.put(tc, oldPos);

                    try  (SimplyCloseable sc = recordLength()) {
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

                    try  (SimplyCloseable sc = recordLength()) {
                        _OB_writeEndian();
                        write_string(tc.id());
                        write_string(tc.name());
                        org.omg.CORBA.TypeCode discType = tc.discriminator_type();
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
                                org.omg.CORBA.TypeCode origDiscType = TypeCode._OB_getOrigType(discType);
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

                    try  (SimplyCloseable sc = recordLength()) {
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

                    try  (SimplyCloseable sc = recordLength()) {
                        _OB_writeEndian();
                        writeTypeCodeImpl(tc.content_type(), history);
                        write_ulong(tc.length());
                    }

                    break;
                }

                case _tk_alias:
                case _tk_value_box: {
                    history.put(tc, oldPos);

                    try  (SimplyCloseable sc = recordLength()) {
                        _OB_writeEndian();
                        write_string(tc.id());
                        write_string(tc.name());
                        writeTypeCodeImpl(tc.content_type(), history);
                    }

                    break;
                }

                case _tk_value: {
                    history.put(tc, oldPos);

                    org.omg.CORBA.TypeCode concreteBase = tc.concrete_base_type();
                    if (concreteBase == null) {
                        concreteBase = TypeCodeFactory.createPrimitiveTC(tk_null);
                    }

                    try  (SimplyCloseable sc = recordLength()) {
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

    //
    // Must be called prior to any writes
    //
    private void checkBeginChunk() {
        Assert.ensure(valueWriter_ != null);
        valueWriter_.checkBeginChunk();
    }

    private ValueWriter valueWriter() {
        if (valueWriter_ == null) valueWriter_ = new ValueWriter(this, writeBuffer);
        return valueWriter_;
    }

    private void addCapacity(int size) {
        if (atEndOfGiop_1_2_Header) {
            atEndOfGiop_1_2_Header = false;
            addCapacity(size, EIGHT_BYTE_BOUNDARY);
        } else {
            //
            // If we're at the end of the current buffer, then we are about
            // to write new data. We must first check if we need to start a
            // chunk, which may result in a recursive call to addCapacity().
            //
            if (writeBuffer.isComplete() && valueWriter_ != null) {
                checkBeginChunk();
            }

            // If there isn't enough room, then reallocate the buffer
            final boolean resized = writeBuffer.ensureAvailable(size);
            if (resized) checkTimeout();
        }
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

        //
        // If we're at the end of the current buffer, then we are about
        // to write new data. We must first check if we need to start a
        // chunk, which may result in a recursive call to addCapacity().
        //
        if (writeBuffer.isComplete() && valueWriter_ != null) {
            checkBeginChunk();
        }

        if (atEndOfGiop_1_2_Header) {
            boundary = EIGHT_BYTE_BOUNDARY;
            atEndOfGiop_1_2_Header = false;
        }

        // If there isn't enough room, then reallocate the buffer
        final boolean resized = writeBuffer.ensureAvailable(size, boundary);
        if (resized) checkTimeout();
    }

    public void write(int b) {
        //
        // this matches the behaviour of this function in the Java ORB
        // and not what is outlined in the java.io.OutputStream
        //
        write_long(b);
    }

    public org.omg.CORBA.ORB orb() {
        return (orbInstance_ == null) ? null : orbInstance_.getORB();
    }

    @Override
    public InputStream create_input_stream() {
        InputStream in = new InputStream(getBufferReader(), false, codeConverters_, giopVersion_);
        in._OB_ORBInstance(orbInstance_);
        return in;
    }

    public void write_boolean(boolean value) {
        addCapacity(1);
        writeBuffer.writeByte(value ? 1 : 0);
    }

    public void write_char(char value) {
        if (value > 255)
            throw new DATA_CONVERSION("char value exceeds 255: " + (int) value);

        addCapacity(1);

        final CodeConverterBase converter = codeConverters_.outputCharConverter;

        if (charConversionRequired_)
            value = converter.convert(value);

        if (charWriterRequired_)
            converter.write_char(writeBuffer, value);
        else
            writeBuffer.writeByte(value);
    }

    public void write_wchar(char value) {
        write_wchar(value, false);
    }

    private void write_wchar(char value, boolean partOfString) {
        final CodeConverterBase converter = codeConverters_.outputWcharConverter;

        //
        // pre-convert the character if necessary
        //
        if (wCharConversionRequired_)
            value = converter.convert(value);

        if (wCharWriterRequired_) {
            if (!partOfString)
                converter.set_writer_flags(CodeSetWriter.FIRST_CHAR);

            //
            // For GIOP 1.1 non byte-oriented wide characters are written
            // as ushort or ulong, depending on their maximum length
            // listed in the code set registry.
            //
            switch (giopVersion_) {
            case GIOP1_0:
                // we don't support special writers for GIOP 1.0 if
                // conversion is required or if a writer is required
                throw Assert.fail();

            case GIOP1_1: {
                // get the length of the character
                int len = converter.write_count_wchar(value);

                // For GIOP 1.1 we are limited to 2-byte wchars
                // so make sure to check for that
                Assert.ensure(len == 2);

                // allocate aligned space
                addCapacity(2, TWO_BYTE_BOUNDARY);

                // write using the writer
                converter.write_wchar(writeBuffer, value);
                break;
            }

            default: {
                // get the length of the character
                int len = converter.write_count_wchar(value);

                // write the octet length at the beginning
                write_octet((byte) len);

                // add unaligned capacity
                addCapacity(len);

                // write the actual character
                converter.write_wchar(writeBuffer, value);
                break;
            }
            }
        } else {
            switch (giopVersion_) {
            case GIOP1_0: {
                // Orbix2000/Orbacus/E compatible 1.0 marshal

                // add aligned capacity
                addCapacity(2, TWO_BYTE_BOUNDARY);

                // write 2-byte character in big endian
                writeBuffer.writeChar(value);
            }
                break;

            case GIOP1_1: {
                write_ushort((short) value);
            }
                break;

            default: {
                // add unaligned space for character
                addCapacity(3);

                // write the octet length at the start
                writeBuffer.writeByte(2);

                // write the character in big endian format
                writeBuffer.writeChar(value);
            }
                break;
            }
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

    public void write_ushort(short value) {
        write_short(value);
    }

    public void write_long(int value) {
        addCapacity(4, FOUR_BYTE_BOUNDARY);
        writeBuffer.writeInt(value);
    }

    public void write_ulong(int value) {
        write_long(value);
    }

    public void write_longlong(long value) {
        addCapacity(8, EIGHT_BYTE_BOUNDARY);
        writeBuffer.writeLong(value);
    }

    public void write_ulonglong(long value) {
        write_longlong(value);
    }

    public void write_float(float value) {
        write_long(Float.floatToIntBits(value));
    }

    public void write_double(double value) {
        write_longlong(Double.doubleToLongBits(value));
    }

    public void write_string(String value) {
        LOGGER.finest("Writing string value " + value);
        final char[] arr = value.toCharArray();
        final CodeConverterBase converter = codeConverters_.outputCharConverter;

        if (!charWriterRequired_) {
            int len = arr.length;
            int capacity = len + 1;
            write_ulong(capacity); // writes the length and ensures a two-byte boundary alignment
            addCapacity(capacity);
            if (charConversionRequired_) {
                for (char c: arr) writeBuffer.writeByte(converter.convert(checkChar(c)));
            } else {
                for (char c: arr) writeBuffer.writeByte(checkChar(c));
            }
            // write null terminator
            writeBuffer.writeByte(0);
        } else {
            // We don't know how much space each character will require: each char could take up to four bytes.
            // To avoid re-allocation, create a large enough temporary buffer up front.
            // NOTE: we need to use a temporary buffer to count the bytes reliably, because
            // chunking can add bytes other than just the chars to be written.
            final WriteBuffer tmpWriter = Buffer.createWriteBuffer(4 + value.length() * 4 + 1);
            if (charConversionRequired_) {
                for (char c : arr) converter.write_char(tmpWriter, converter.convert(checkChar(c)));
            } else {
                for (char c : arr) converter.write_char(tmpWriter, checkChar(c));
            }
            // write the null terminator
            tmpWriter.writeByte(0);
            // ignore any unused space in the buffer
            tmpWriter.trim();
            // write the length
            write_ulong(tmpWriter.length());
            // and write the contents
            addCapacity(tmpWriter.length());
            tmpWriter.readFromStart().readBytes(writeBuffer);
        }
    }

    private static char checkChar(char c) {
        if (c > 0xff) throw new DATA_CONVERSION(String.format("illegal char value for string: 0x%04x", (int)c));
        return c;
    }

    public void write_wstring(String value) {
        final char[] arr = value.toCharArray();
        final int len = arr.length;

        LOGGER.finest("Writing wstring value " + value);
        //
        // get converter/writer instance
        //
        final CodeConverterBase converter = codeConverters_.outputWcharConverter;

        //
        // some writers (specially UTF-16) requires the possible BOM
        // only found at the beginning of a string... this will
        // indicate that we are at the start of the first character
        // of the string to the writer
        if (wCharWriterRequired_)
            converter.set_writer_flags(CodeSetWriter.FIRST_CHAR);

        //
        // for GIOP 1.0/1.1 we don't need to differentiate between
        // strings requiring a writer/converter (or not) since they can
        // be handled by the write_wchar() method
        //
        switch (giopVersion_) {
            case GIOP1_0:
            case GIOP1_1:
                //
                // write the length of the string
                //
                write_ulong(len + 1);

                //
                // now write all the characters
                //
                for (char anArr : arr) write_wchar(anArr, true);

                //
                // and the null terminator
                //
                write_wchar((char) 0, true);
                return;
            default:
        }

        // save the starting position and write the gap to place the length of the string later
        try (SimplyCloseable sc = recordLength()) {
            if (wCharWriterRequired_) {
                for (char anArr : arr) {
                    char v = anArr;

                    //
                    // check if the character requires conversion
                    //
                    if (wCharConversionRequired_) v = converter.convert(v);

                    //
                    // add capacity for the character
                    //
                    addCapacity(converter.write_count_wchar(v));

                    //
                    // write the character
                    //
                    converter.write_wchar(writeBuffer, v);
                }
            } else {
                //
                // since we don't require a special writer, each character
                // MUST be 2-bytes in size
                //
                addCapacity(len << 1);

                for (char anArr : arr) {
                    char v = anArr;

                    //
                    // check for conversion
                    //
                    if (wCharConversionRequired_) v = converter.convert(v);

                    //
                    // write character in big endian format
                    //
                    writeBuffer.writeChar(v);
                }
            }
        }

        //
        // we've handled GIOP 1.0/1.1 above so this must be GIOP 1.2+
        //

        //
        // write the octet length
        //
    }

    public void write_boolean_array(boolean[] value, int offset, int length) {
        if (length > 0) {
            addCapacity(length);

            for (int i = offset; i < offset + length; i++)
                writeBuffer.writeByte(value[i] ? 1 : 0);
        }
    }

    public void write_char_array(char[] value, int offset, int length) {
        if (length > 0) {
            addCapacity(length);

            if (!(charWriterRequired_ || charConversionRequired_)) {
                for (int i = offset; i < offset + length; i++) {
                    if (value[i] > 255)
                        throw new DATA_CONVERSION("char value exceeds 255: " + (int) value[i]);

                    writeBuffer.writeByte(value[i]);
                }
            } else {
                final CodeConverterBase converter = codeConverters_.outputCharConverter;

                //
                // Intermediate variable used for efficiency
                //
                boolean bothRequired = charWriterRequired_
                        && charConversionRequired_;

                for (int i = offset; i < offset + length; i++) {
                    if (value[i] > 255)
                        throw new DATA_CONVERSION("char value exceeds 255: " + (int) value[i]);

                    if (bothRequired)
                        converter.write_char(writeBuffer, converter.convert(value[i]));
                    else if (charWriterRequired_)
                        converter.write_char(writeBuffer, value[i]);
                    else
                        writeBuffer.writeByte(converter.convert(value[i]));
                }
            }
        }
    }

    public void write_wchar_array(char[] value, int offset, int length) {
        for (int i = offset; i < offset + length; i++)
            write_wchar(value[i], false);
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
            LOGGER.finest("Writing a null CORBA object value");
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

    public void write_TypeCode(org.omg.CORBA.TypeCode t) {
        // NOTE:
        // No data with natural alignment of greater than four octets
        // is needed for TypeCode. Therefore it is not necessary to do
        // encapsulation in a separate buffer.

        if (t == null) throw new BAD_TYPECODE("TypeCode is nil");

        writeTypeCodeImpl(t, new HashMap<org.omg.CORBA.TypeCode, Integer>());
    }

    public void write_any(org.omg.CORBA.Any value) {
        LOGGER.finest("Writing an ANY value of type " + value.type().kind());
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

    public void write_value(Serializable value, org.omg.CORBA.TypeCode tc, BoxedValueHelper helper) {
        checkTimeout();
        valueWriter().writeValueBox(value, tc, helper);
        checkTimeout();
    }

    public void write_InputStream(final org.omg.CORBA.portable.InputStream in, org.omg.CORBA.TypeCode tc) {
        try {
            LOGGER.fine("writing a value of type " + tc.kind().value());

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
        org.omg.CORBA.TypeCode p = in.read_TypeCode();
        write_TypeCode(p);
        write_InputStream(in, p);
    }

    private void copyValueFrom(org.omg.CORBA_2_3.portable.InputStream in, org.omg.CORBA.TypeCode tc) {
        if (in instanceof InputStream) {
            ((InputStream)in)._OB_remarshalValue(tc, this);
        } else {
            write_value(in.read_value());
        }
    }

    private void copyAbstractInterfaceFrom(org.omg.CORBA.portable.InputStream in) {
        boolean b = in.read_boolean();
        write_boolean(b);
        if (b) {
            write_Object(in.read_Object());
        } else if (in instanceof InputStream) {
            // We have no TypeCode information about the
            // valuetype, so we must use _tc_ValueBase and
            // rely on the type information sent on the wire
            ((InputStream) in)._OB_remarshalValue(ValueBaseHelper.type(), this);
        } else {
            write_value(((org.omg.CORBA_2_3.portable.InputStream) in).read_value());
        }
    }

    private void copyArrayFrom(org.omg.CORBA.portable.InputStream in, org.omg.CORBA.TypeCode tc) throws BadKind {
        final boolean swapInput = (in instanceof InputStream) && ((InputStream)in).swap_;
        int len;

        if (tc.kind().value() == _tk_sequence) {
            len = in.read_ulong();
            write_ulong(len);
        } else {
            len = tc.length();
        }

        if (len <= 0) return;

        org.omg.CORBA.TypeCode origContentType = TypeCode._OB_getOrigType(tc.content_type());

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
            if (charWriterRequired_ || charConversionRequired_) {
                char[] ch = new char[len];
                in.read_char_array(ch, 0, len);
                write_char_array(ch, 0, len);
            } else {
                readFrom(in, len);
            }
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

    private void copyUnionFrom(org.omg.CORBA.portable.InputStream in, org.omg.CORBA.TypeCode tc) throws BadKind, Bounds {
        int defaultIndex = tc.default_index();
        int memberIndex = -1;

        org.omg.CORBA.TypeCode origDiscType = TypeCode._OB_getOrigType(tc.discriminator_type());

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

        return;
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

    public OutputStream() {
        this(Buffer.createWriteBuffer(), null, null);
    }

    public OutputStream(int initialBufferSize) {
        this(Buffer.createWriteBuffer(initialBufferSize), null, null);
    }

    public OutputStream(CodeConverters converters, GiopVersion giopVersion) {
        this(Buffer.createWriteBuffer(), converters, giopVersion);
    }

    public OutputStream(int initialBufferSize, CodeConverters converters, GiopVersion giopVersion) {
        this(Buffer.createWriteBuffer(initialBufferSize), converters, giopVersion);
    }

    public OutputStream(WriteBuffer writeBuffer) {
        this(writeBuffer, null, null);
    }

    public OutputStream(WriteBuffer writeBuffer, CodeConverters converters, GiopVersion giopVersion) {
        this.writeBuffer = writeBuffer;
        this.giopVersion_ = giopVersion == null ? GIOP1_0 : giopVersion;

        {
            Optional<CodeConverterBase> charConv = Optional.ofNullable(converters).map(c -> c.outputCharConverter);
            this.charWriterRequired_ = charConv.map(cc -> cc.writerRequired()).orElse(false);
            this.charConversionRequired_ = charConv.map(cc -> cc.conversionRequired()).orElse(false);
        }
        {
            Optional<CodeConverterBase> wcharConv = Optional.ofNullable(converters).map(c -> c.outputWcharConverter);
            this.wCharWriterRequired_ = wcharConv.map(cc -> cc.writerRequired()).orElse(false);
            this.wCharConversionRequired_ = wcharConv.map(cc -> cc.conversionRequired()).orElse(false);
        }

        this.codeConverters_ = CodeConverters.createCopy(converters);
    }

    @Override
    public void close() {}

    boolean writtenBytesEqual(OutputStream that) {
        return writeBuffer.dataEquals(writeBuffer);
    }

    public byte[] copyWrittenBytes() {
        return writeBuffer.trim().readFromStart().copyRemainingBytes();
    }

    public String writtenBytesToAscii() {
        return writeBuffer.trim().readFromStart().remainingBytesToAscii();
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
        orbInstance_ = orbInstance;
    }

    public void _OB_invocationContext(Object invocationContext) {
        invocationContext_ = invocationContext;
    }

    public Object _OB_invocationContext() {
        return invocationContext_;
    }

    void _OB_delegateContext(Object delegateContext) {
        delegateContext_ = delegateContext;
    }

    Object _OB_delegateContext() {
        return delegateContext_;
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
