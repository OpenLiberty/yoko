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

import org.apache.yoko.io.Buffer;
import org.apache.yoko.io.ReadBuffer;
import org.apache.yoko.io.WriteBuffer;
import org.apache.yoko.orb.codecs.CharCodec.CharReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Utf16Test extends AbstractSimpleCodecTest implements TestData {
    static final char BOM = '\uFEFF';
    static final char ANTI_BOM = '\uFFFE';

    private static final ExpectedCharReader READ_CHAR = ReadBuffer::readChar;
    private static final ExpectedCharWriter WRITE_CHAR = WriteBuffer::writeChar;

    Utf16Test() { super("UTF-16", WriteBuffer::writeChar, ReadBuffer::readChar); }

    boolean isDoubleByte() { return true; }

    static Stream<Object[]> bmpCharsExcludingBom() {
        return TestData.bmpChars()
                .filter(args -> 0xFEFF != (int) args[1]) // FEFF is used as a BOM in UTF-16
                .filter(args -> 0xFFFE != (int) args[1]); // FFFE is the BOM with bytes swapped
    }

    @ParameterizedTest(name = "Decode ASCII {0} {2}")
    @MethodSource("asciiChars")
    public void testAscii(String hex, int codepoint, char c) {
        assertValidChar(c);
    }

    @ParameterizedTest(name = "Decode ISO Latin 1 {0} {2}")
    @MethodSource("isoLatinChars")
    void testIsoLatin1(String hex, int codepoint, char c) {
        assertValidChar(c);
    }

    @ParameterizedTest(name = "Decode BMP {0} {2}")
    @MethodSource("bmpCharsExcludingBom")
    void testBmp(String hex, int codepoint, char c) {
        assertValidChar(c);
    }

    @ParameterizedTest(name = "Decode high surrogate {0} {2}")
    @MethodSource("highSurrogates")
    void testHighSurrogates(String hex, int codepoint, char c) {
        assertValidChar(c);
    }

    @ParameterizedTest(name = "Decode low surrogate {0} {2}")
    @MethodSource("lowSurrogates")
    void testLowSurrogates(String hex, int codepoint, char c) {
        assertValidChar(c);
    }

    @Test
    void testBomPlusSingleChar() {
        // BOM should be discarded, next two bytes should be read as char
        writeExpectedChar('\uFEFF');
        writeExpectedChar('A');
        ReadBuffer bomA = getReadBuffer();
        assertEquals('A', codec.readChar(bomA));
        codec.assertNoBufferedCharData();
        assertTrue(bomA.isComplete());
    }

    @Test
    void testBomPlusSingleCharLittleEndian() {
        // swapped BOM should be discarded, next two bytes should be read as other endian char
        writeExpectedChar(ANTI_BOM);
        writeExpectedChar('\u4100'); // byte-swapped 'A'
        ReadBuffer bomA = getReadBuffer();
        assertEquals('A', codec.readChar(bomA));
        codec.assertNoBufferedCharData();
        assertTrue(bomA.isComplete());
    }

    @Test
    void testBomBom() {
        writeExpectedChar(BOM);
        writeExpectedChar(BOM);
        // BOM should be discarded, next two bytes should be read as char
        ReadBuffer bombom = getReadBuffer();
        assertEquals(BOM, codec.readChar(bombom));
        assertTrue(bombom.isComplete());
    }

    @Test
    void testBomBomLittleEndian() {
        writeExpectedChar(ANTI_BOM);
        writeExpectedChar(ANTI_BOM);
        ReadBuffer bombom = getReadBuffer();
        // BOM should be discarded, next two bytes should be read as byte-swapped char
        assertEquals(BOM, codec.readChar(bombom));
        assertTrue(bombom.isComplete());
    }

    @Test
    void testBomOnItsOwn() {
        writeExpectedChar(BOM);
        // If the only character available is a BOM (0xFEFF),
        // then either this was an empty string, or if we are expecting a char
        // it genuinely is a single ZERO WIDTH NO BREAK SPACE character (also 0xFEFF)
        ReadBuffer singleBom = getReadBuffer();
        assertEquals(BOM, codec.readChar(singleBom));
        assertTrue(singleBom.isComplete());
    }

    @Test
    void testBomOnItsOwnLittleEndian() {
        writeExpectedChar(ANTI_BOM);
        // If the only character available is a byte-swapped BOM (0xFFFE),
        // then either this was an empty string, or if we are expecting a char
        // it genuinely is a single reserved unicode character (also 0xFFFE)
        ReadBuffer singleBom = getReadBuffer();
        assertEquals(ANTI_BOM, codec.readChar(singleBom));
        assertTrue(singleBom.isComplete());
    }

    @ParameterizedTest(name = "testStringOfChars[{index}]({arguments})")
    @ValueSource(strings = {"", "hello", "\0", "\uD800\uDC00", "\uDBFF\uDFFF"})
    void testStringOfChars(String expected) {
        testStringOfChars(expected, false);
    }

    @ParameterizedTest(name = "testStringOfCharsBigEndian[{index}]({arguments})")
    @ValueSource(strings = {"", "hello", "\0", "" + BOM, "" + ANTI_BOM, "\uD800\uDC00", "\uDBFF\uDFFF"})
    void testStringOfCharsBigEndian(String expected) {
        writeExpectedChar(BOM);
        testStringOfChars(expected, false);
    }

    @ParameterizedTest(name = "testStringOfCharsLittleEndian[{index}]({arguments})")
    @ValueSource(strings = {"", "hello", "\0", "" + BOM, "" + ANTI_BOM, "\uD800\uDC00", "\uDBFF\uDFFF"})
    void testStringOfCharsLittleEndian(String expected) {
        writeExpectedChar(BOM);
        testStringOfChars(expected, true);
    }

    private void testStringOfChars(String expected, boolean swap) {
        for (char c: expected.toCharArray()) writeExpectedChar(c);
        ReadBuffer in = swap ? getByteSwappedReadBuffer() : getReadBuffer();
        CharReader rdr = codec.beginString(in);
        StringBuilder sb = new StringBuilder();
        while (!in.isComplete()) {
            sb.append(rdr.readChar(in));
        }
        assertEquals(expected, sb.toString());
    }

    private ReadBuffer getByteSwappedReadBuffer() {
        // now swap every pair of bytes around
        byte[] bytes = getReadBuffer().copyRemainingBytes();
        assertTrue(0 == bytes.length % 2);
        for (int i = 0; i < bytes.length; i++) {
            byte tmp = bytes[i];
            bytes[i] = bytes[++i];
            bytes[i] = tmp;
        }
        return Buffer.createReadBuffer(bytes);
    }
}
