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
package org.apache.yoko.orb.codecs;

import org.apache.yoko.io.ReadBuffer;
import org.apache.yoko.io.WriteBuffer;
import org.omg.CORBA.DATA_CONVERSION;

import static java.lang.Character.highSurrogate;
import static java.lang.Character.isHighSurrogate;
import static java.lang.Character.isLowSurrogate;
import static java.lang.Character.lowSurrogate;
import static java.lang.Character.toCodePoint;
import static java.util.logging.Level.WARNING;
import static org.apache.yoko.logging.VerboseLogging.DATA_IN_LOG;
import static org.apache.yoko.logging.VerboseLogging.DATA_OUT_LOG;
import static org.apache.yoko.orb.codecs.Util.UNICODE_REPLACEMENT_CHAR;
import static org.apache.yoko.util.MinorCodes.MinorUTF8Encoding;
import static org.omg.CORBA.CompletionStatus.COMPLETED_MAYBE;

final class Utf8Codec implements CharCodec {
    static class InternalException extends Exception { InternalException(String message) { super(message); } }

    // These are the minimum acceptable values for the encoding length, indexed by the number of bytes.
    private static final int[] MIN_CODEPOINT = {-1, 0, 0x80, 0x800, 0x10000};

    private char highSurrogate = 0;
    private char lowSurrogate = 0;

    public char readChar(ReadBuffer in) {
        //
        if (0 != lowSurrogate) try {
            return lowSurrogate;
        } finally {
            lowSurrogate = 0;
            in.skipBytes(1);
        }
        char c = in.readByteAsChar();
        // check for single byte char
        if (c < '\u007F') return c;
        // remember buffer position
        final int pos = in.getPosition() - 1;
        try {
            int codepoint = readCodePoint(c, in);
            int numBytes = in.getPosition() - pos;
            if (codepoint < MIN_CODEPOINT[numBytes]) {
                // Permit a two-byte overlong encoding for NUL, because modified UTF-8 uses this.
                if (2 == numBytes && codepoint == 0) return 0;
                // In any other case, complain.
                throw new InternalException(String.format("Overlong encoding: %d bytes used for codepoint 0x%06X", numBytes, codepoint));
            }
            // Note that we have not ruled out surrogate codepoints,
            // which would be encoded as 3-byte sequences by CESU-8 and modified UTF-8.
            // We will simply return the surrogates individually.
            if (numBytes < 4) return (char) codepoint;
            // What we have here is a supplementary codepoint.
            // This is to be returned as a UTF-16 surrogate pair.
            // First save the low surrogate to return later.
            lowSurrogate = lowSurrogate(codepoint);
            // to avoid a caller perceiving this codepoint as read, leave the last byte "unread"
            in.skipBytes(-1);
            // return the high surrogate FIRST
            return highSurrogate(codepoint);
        } catch (InternalException e) {
            // May not have read all the bytes for a utf8 sequence because we stopped at the first junk byte.
            // This could result in additional REPLACEMENT_CHAR in the output.
            DATA_IN_LOG.log(WARNING, e.getMessage(), e);
            DATA_IN_LOG.fine(in.dumpAllDataWithPosition());
            return UNICODE_REPLACEMENT_CHAR;
        }
    }

    /** Read remaining bytes and compute codepoint */
    private static int readCodePoint(int leadByte, ReadBuffer in) throws InternalException {
        switch (leadByte >> 3) {
            case 0b11000:
            case 0b11001:
            case 0b11010:
            case 0b11011: return ((0x1F & leadByte) << 6) | (nextByte(in));
            case 0b11100:
            case 0b11101: return ((0x0F & leadByte) << 12) | (nextByte(in) << 6) | nextByte(in);
            case 0b11110: return ((0x07 & leadByte) << 18) | (nextByte(in) << 12) | (nextByte(in) << 6) | nextByte(in);
            default: throw new InternalException(String.format("Illegal UTF-8 lead byte: 0x%02X", leadByte));
        }
    }

    private static int nextByte(ReadBuffer in) throws InternalException {
        int i = 0xFF & in.readByte();
        if (2 != i >> 6) throw new InternalException(String.format("Illegal UTF-8 continuation byte: 0x%02X", i));
        return i & 0x3F;
    }

    public void writeChar(char c, WriteBuffer out) {
        try {
            final int codepoint;
            if (0 != highSurrogate) {
                try {
                    if (isHighSurrogate(c)) throw new InternalException(String.format("Received two high surrogates in a row: 0x%04X 0x%04X", highSurrogate, c));
                    if (!isLowSurrogate(c)) throw new InternalException(String.format("Expected low surrogate but received: 0x%04X", (int) c));
                    codepoint = toCodePoint(highSurrogate, c);
                } finally {
                    highSurrogate = 0;
                }
            } else {
                if (isLowSurrogate(c)) throw new InternalException(String.format("Received unexpected low surrogate: 0x%04X", (int) c));
                if (isHighSurrogate(c)) {
                    highSurrogate = c;
                    return;
                }
                codepoint = c;
            }
            writeBytes(codepoint, out);
        } catch (InternalException x) {
            DATA_OUT_LOG.warning(x.getMessage());
            throw (DATA_CONVERSION) new DATA_CONVERSION(x.getMessage(), MinorUTF8Encoding, COMPLETED_MAYBE).initCause(x);
        }
    }

    private static void writeBytes(int codepoint, WriteBuffer out) {
        int numBytes = getUtf8Len(codepoint);
        final int CONT_BYTE = 0b1000_0000;
        final int HDR_2BYTE = 0b1100_0000;
        final int HDR_3BYTE = 0b1110_0000;
        final int HDR_4BYTE = 0b1111_0000;
        final int MASK_LAST_3_BITS = 0b0000_0111;
        final int MASK_LAST_4_BITS = 0b0000_1111;
        final int MASK_LAST_5_BITS = 0b0001_1111;
        final int MASK_LAST_6_BITS = 0b0011_1111;
        DATA_OUT_LOG.fine(() -> String.format("Encoding codepoint 0x%06X as %d bytes", codepoint, numBytes));
        switch (numBytes) {
        case 1:
            // write out the byte as-is
            out.writeByte(codepoint);
            return;
        case 2:
            // spread the codepoint across 2 bytes
            out.writeByte(HDR_2BYTE + (MASK_LAST_5_BITS & (codepoint >> 6)));
            out.writeByte(CONT_BYTE + (MASK_LAST_6_BITS & codepoint));
            return;
        case 3:
            // spread the codepoint across 3 bytes
            out.writeByte(HDR_3BYTE + (MASK_LAST_4_BITS & (codepoint >> 12)));
            out.writeByte(CONT_BYTE + (MASK_LAST_6_BITS & (codepoint >> 6)));
            out.writeByte(CONT_BYTE + (MASK_LAST_6_BITS & codepoint));
            return;
        case 4:
            // spread the codepoint across 4 bytes
            out.writeByte(HDR_4BYTE + (MASK_LAST_3_BITS & (codepoint >> 18)));
            out.writeByte(CONT_BYTE + (MASK_LAST_6_BITS & (codepoint >> 12)));
            out.writeByte(CONT_BYTE + (MASK_LAST_6_BITS & (codepoint >> 6)));
            out.writeByte(CONT_BYTE + (MASK_LAST_6_BITS & codepoint));
        }
    }

    private static int getUtf8Len(int codepoint) {
        if (codepoint < 0x80) return 1;
        if (codepoint < 0x800) return 2;
        return codepoint < 0x10000 ? 3 : 4;
    }

    public void assertNoBufferedCharData() throws DATA_CONVERSION {
        if (0 != highSurrogate) throw new DATA_CONVERSION("High surrogate as last character", MinorUTF8Encoding, COMPLETED_MAYBE);
        if (0 != lowSurrogate) throw new DATA_CONVERSION("Low surrogate left unread", MinorUTF8Overflow, COMPLETED_MAYBE);
    }
}
