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
 * distributed under the License is distributed on an AS IS BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.apache.yoko.orb.codecs;

import org.apache.yoko.io.Buffer;
import org.apache.yoko.io.ReadBuffer;
import org.apache.yoko.io.WriteBuffer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.omg.CORBA.DATA_CONVERSION;
import org.omg.CORBA.MARSHAL;

import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Utf8InvalidTest implements TestData {
    static final int MIN_1_BYTE = 0, MIN_2_BYTE = 1<<7, MIN_3_BYTE = 1 << 5+6, MIN_4_BYTE = 1 << 4+6+6;
    final CharCodec codec = CharCodec.forName("UTF-8");
    final WriteBuffer out = Buffer.createWriteBuffer(4);

    @ParameterizedTest(name = "Invalid lead byte: 0x{0}")
    @ValueSource(ints = {
            0b1000_0000, // continuation byte (min)
            0b1011_1111, // continuation byte (max)
            0b1111_1000, // 5-byte encoding (unsupported)
            0b1111_1100, // 6-byte encoding (unsupported)
            0b1111_1110, // 7-byte encoding (unsupported)
            0b1111_1111  // 8-or-more-byte encoding? (unsupported)
    })
    void testInvalidLeadBytes(int b) {
        out.writeByte(b);
        ReadBuffer in = out.trim().readFromStart();
        assertEquals('\uFFFD', codec.readChar(in));
        assertThrows(IndexOutOfBoundsException.class, () -> codec.readChar(in));
    }

    static IntStream validLeadBytes() {return IntStream.of(
            0b1100_0000,  // 2-byte encoding lead byte (min)
            0b1101_1111,  // 2-byte encoding lead byte (max)
            0b1110_0000,  // 3-byte encoding lead byte (min)
            0b1110_1111,  // 3-byte encoding lead byte (max)
            0b1111_0000,  // 4-byte encoding lead byte (min)
            0b1111_0111); // 4-byte encoding lead byte (max)
    }

    @ParameterizedTest(name = "Truncated encoding (one byte): 0x{0}")
    @MethodSource("validLeadBytes")
    void testTruncatedEncodingsOneByte(int b) {
        out.writeByte(b);
        ReadBuffer in = out.trim().readFromStart();
        assertEquals('\uFFFD', codec.readChar(in));
        assertThrows(IndexOutOfBoundsException.class, () -> codec.readChar(in));
    }

    @ParameterizedTest(name = "Truncated encoding (two bytes): 0x{0}")
    @MethodSource("validLeadBytes")
    void testTruncatedEncodingsTwoByteSecondByteIsValidChar(int leadByte) {
        out.writeByte(leadByte); // write byte 1
        out.writeByte('A'); // write byte 2
        ReadBuffer in = out.trim().readFromStart();
        assertEquals('\uFFFD', codec.readChar(in));
        assertEquals('A', codec.readChar(in));
        assertThrows(IndexOutOfBoundsException.class, () -> codec.readChar(in));
    }

    @ParameterizedTest(name = "Truncated encoding (two bytes): 0x{0}")
    @MethodSource("validLeadBytes")
    void testTruncatedEncodingsTwoByteSecondByteIsNotValidChar(int leadByte) {
        out.writeByte(leadByte); // write byte 1
        out.writeByte(leadByte); // write byte 2
        ReadBuffer in = out.trim().readFromStart();
        assertEquals('\uFFFD', codec.readChar(in));
        assertEquals('\uFFFD', codec.readChar(in));
        assertThrows(IndexOutOfBoundsException.class, () -> codec.readChar(in));
    }

    @ParameterizedTest(name = "Truncated encoding (two bytes): 0x{0}")
    @MethodSource("validLeadBytes")
    void testTruncatedEncodingsFollowedByValidTwoByteEncoding(int leadByte) {
        out.writeByte(leadByte); // write byte 1
        out.writeByte(0xC3); // write byte 1 for U diaeresis
        out.writeByte(0x9C); // write byte 2 for U diaeresis
        ReadBuffer in = out.trim().readFromStart();
        assertEquals('\uFFFD', codec.readChar(in));
        assertEquals('\u00DC', codec.readChar(in));
        assertThrows(IndexOutOfBoundsException.class, () -> codec.readChar(in));
    }

    @ParameterizedTest(name = "Test overlong two-byte encoding for {0}")
    @ValueSource(chars = {'/', ';', ':', '?', '!', '@', '#', '$', '%', '&', '\'', '*', '+', ',', '.', '(', ')', '[', ']', '{', '}', '\\', '|', '~'})
    void testOverlongTwoByteEncoding(char c) {
        out.writeByte(0xC0 | (c >> 6)); // write byte 1
        out.writeByte(0x80 | (c & 0x3F)); // write byte 2
        ReadBuffer in = out.trim().readFromStart();
        assertEquals('\uFFFD', codec.readChar(in));
        assertEquals('\uFFFD', codec.readChar(in));
        assertThrows(IndexOutOfBoundsException.class, () -> codec.readChar(in));
    }

    @ParameterizedTest(name = "Test overlong three-byte encoding for {0}")
    @ValueSource(chars = {'/', ';', '\u0080', '\u07ff'}) // throw in some known two-byte characters
    void testOverlongThreeByteEncoding(char c) {
        out.writeByte(0xE0 | (c >> 12)); // write byte 1
        out.writeByte(0x80 | ((c >> 6) & 0x3F)); // write byte 2
        out.writeByte(0x80 | (c & 0x3F)); // write byte 3
        ReadBuffer in = out.trim().readFromStart();
        assertEquals('\uFFFD', codec.readChar(in));
        assertEquals('\uFFFD', codec.readChar(in));
        assertEquals('\uFFFD', codec.readChar(in));
        assertThrows(IndexOutOfBoundsException.class, () -> codec.readChar(in));
    }

    @ParameterizedTest(name = "Test overlong four-byte encoding for {0}")
    @ValueSource(chars = {'/', ';', '\u0080', '\u07ff', '\ue000', '\uffdf'}) // throw in some known two- and three-byte characters
    void testOverlongFourByteEncoding(char c) {
        out.writeByte(0xF0 | (c >> 18)); // write byte 1
        out.writeByte(0x80 | ((c >> 12) & 0x3F)); // write byte 2
        out.writeByte(0x80 | ((c >> 6) & 0x3F)); // write byte 3
        out.writeByte(0x80 | (c & 0x3F)); // write byte 4
        ReadBuffer in = out.trim().readFromStart();
        assertEquals('\uFFFD', codec.readChar(in));
        assertEquals('\uFFFD', codec.readChar(in));
        assertEquals('\uFFFD', codec.readChar(in));
        assertEquals('\uFFFD', codec.readChar(in));
    }

    @Test
    void testEncodeHighSurrogateOnly() {
        codec.writeChar('\uD800', out);
        assertEquals(0, out.getPosition(), "Nothing should have been written yet");
        assertThrows(DATA_CONVERSION.class, codec::assertNoBufferedCharData, "High surrogate alone cannot be encoded in UTF-8");
    }

    @Test
    void testEncodeLowSurrogateOnly() {
        assertThrows(DATA_CONVERSION.class, () -> codec.writeChar('\uDC00', out));
        assertEquals(0, out.getPosition(), "No data should have been written");
    }

}
