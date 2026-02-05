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
package org.apache.yoko.codecs;

import org.apache.yoko.io.ReadBuffer;
import org.apache.yoko.io.WriteBuffer;
import org.apache.yoko.orb.OB.CodeSetInfo;

import static org.apache.yoko.logging.VerboseLogging.DATA_IN_LOG;
import static org.apache.yoko.codecs.Util.BYTE_ORDER_MARKER;
import static org.apache.yoko.codecs.Util.BYTE_SWAPD_MARKER;
import static org.apache.yoko.codecs.Util.UNICODE_REPLACEMENT_CHAR;
import static org.apache.yoko.codecs.Util.ZERO_WIDTH_NO_BREAK_SPACE;

enum SimpleWcharCodec implements WcharCodec {
    UTF_16 {
        public CodeSetInfo getCodeSetInfo() { return CodeSetInfo.UTF_16; }

        public int octetCount(String s) {
            if (s.startsWith(""+ZERO_WIDTH_NO_BREAK_SPACE)) return 2 * (s.length() + 1);
            return 2 * s.length();
        }

        public int octetCountLengthsAndWchars(int numChars) { return 3 * numChars; }

        public char readCharWithEndianFlag(ReadBuffer in, boolean swapBytes) {
            return swapBytes ? in.readChar_LE() : in.readChar();
        }

        public char readLengthAndChar(ReadBuffer in) {
            byte len = in.readByte();
            switch (len) {
                case 2:
                    return in.readChar();
                case 4:
                    char bom = in.readChar();
                    switch (bom) {
                        case BYTE_ORDER_MARKER: return in.readChar();
                        case BYTE_SWAPD_MARKER: return in.readChar_LE();
                        default:
                            DATA_IN_LOG.warning(String.format("Received 4-byte UTF-16 wchar but first 2 bytes did not correspond to a byte order marker: %04X", bom));
                            in.skipBytes(2);
                            return UNICODE_REPLACEMENT_CHAR;
                    }
                default:
                    DATA_IN_LOG.warning("Unexpected length for UTF-16 wchar: " + len);
                    in.skipBytes(len);
                    return UNICODE_REPLACEMENT_CHAR;
            }
        }

        public CharReader beginToReadString(ReadBuffer in) {
            if (in.isComplete()) return ReadBuffer::readChar;
            switch (in.readChar()) {
                case BYTE_ORDER_MARKER: return ReadBuffer::readChar;
                case BYTE_SWAPD_MARKER: return ReadBuffer::readChar_LE;
                // there was no BOM, so rewind to allow the first character to be read
                default: in.skipBytes(-2); return ReadBuffer::readChar;
            }
        }

        public void beginToWriteString(char c, WriteBuffer out) {
            // if the first (or a single) character is a ZERO WIDTH NO-BREAK SPACE, write a BOM first
            // (this is because they are the same bytes and the first pair will be read as a BOM)
            if (ZERO_WIDTH_NO_BREAK_SPACE == c) out.writeChar(BYTE_ORDER_MARKER);
            out.writeChar(c);
        }

        public void writeLengthAndChar(char c, WriteBuffer out) {
            // Older versions of Yoko ignore the encoded length and just read two bytes anyway.
            // So, never write a BOM, and stick to two bytes.
            out.writeByte(2);
            out.writeChar(c);
        }
    },
    /**
     * This converter is for use in collocated scenarios, where the sender and the receiver
     * are in the same process, using the same ORB instance.
     * <br>
     * It takes shortcuts because it exchanges data with another instance of the very same class.
     * e.g. it never writes any lengths or BOMs, so it never reads any lengths or BOMs.
     */
    COLLOCATED {
        public CodeSetInfo getCodeSetInfo() { return CodeSetInfo.COLLOCATED; }

        public int octetCount(String s) { return 2 * s.length(); }

        public int octetCountLengthsAndWchars(int numChars) { return 2 * numChars; }

        public char readCharWithEndianFlag(ReadBuffer in, boolean swapBytes) { return in.readChar(); }

        public char readLengthAndChar(ReadBuffer in) { return in.readChar(); }

        public CharReader beginToReadString(ReadBuffer in) { return this::readChar; }

        public void beginToWriteString(char c, WriteBuffer out) { out.writeChar(c); }

        public void writeLengthAndChar(char c, WriteBuffer out) { out.writeChar(c);}
    },
    /**
     * This codec is for use when no encoding is permitted.
     */
    UNSPECIFIED {
        @Override
        public CodeSetInfo getCodeSetInfo() { return CodeSetInfo.NONE; }

        @Override
        public boolean isFixedWidth() { throw new UnsupportedOperationException("attempt to use unspecified codec"); }

        @Override
        public int charSize() { throw new UnsupportedOperationException("attempt to use unspecified codec"); }

        @Override
        public char readChar(ReadBuffer in) { throw new UnsupportedOperationException("attempt to use unspecified codec"); }

        @Override
        public int octetCount(char c) { throw new UnsupportedOperationException("attempt to use unspecified codec"); }

        @Override
        public int octetCount(String s) { throw new UnsupportedOperationException("attempt to use unspecified codec"); }

        @Override
        public int octetCountLengthsAndWchars(int numChars) { throw new UnsupportedOperationException("attempt to use unspecified codec"); }

        @Override
        public void writeChar(char c, WriteBuffer out) { throw new UnsupportedOperationException("attempt to use unspecified codec"); }

        @Override
        public void assertNoBufferedCharData() { throw new UnsupportedOperationException("attempt to use unspecified codec"); }

        @Override
        public boolean readFinished() {  throw new UnsupportedOperationException("attempt to use unspecified codec"); }

        @Override
        public boolean writeFinished() { throw new UnsupportedOperationException("attempt to use unspecified codec"); }

        @Override
        public char readCharWithEndianFlag(ReadBuffer in, boolean swapBytes) { throw new UnsupportedOperationException("attempt to use unspecified codec"); }

        @Override
        public char readLengthAndChar(ReadBuffer in) { throw new UnsupportedOperationException("attempt to use unspecified codec"); }

        @Override
        public void writeLengthAndChar(char c, WriteBuffer out) { throw new UnsupportedOperationException("attempt to use unspecified codec"); }

        @Override
        public CharReader beginToReadString(ReadBuffer in) { throw new UnsupportedOperationException("attempt to use unspecified codec"); }

        @Override
        public void beginToWriteString(char firstChar, WriteBuffer out) { throw new UnsupportedOperationException("attempt to use unspecified codec"); }
    };
}
