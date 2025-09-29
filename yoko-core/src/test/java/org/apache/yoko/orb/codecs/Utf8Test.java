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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.stream.Stream;

import static java.lang.Character.MAX_LOW_SURROGATE;
import static java.lang.Character.MIN_HIGH_SURROGATE;
import static java.lang.Character.isSupplementaryCodePoint;
import static java.util.stream.IntStream.concat;
import static org.apache.yoko.orb.codecs.TestData.testRange;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Utf8Test implements TestData {
    static final int MIN_1_BYTE = 0, MIN_2_BYTE = 1<<7, MIN_3_BYTE = 1 << 5+6, MIN_4_BYTE = 1 << 4+6+6;
    static final Charset UTF_8 = Charset.forName("UTF-8");
    final CharCodec codec = CharCodec.forName("UTF-8");
    final WriteBuffer out = Buffer.createWriteBuffer(4);

    static Stream<Object[]> _1_ByteChars() { return testRange(MIN_1_BYTE, MIN_2_BYTE).mapToObj(TestData::toHexIntAndString); }
    static Stream<Object[]> _2_ByteChars() { return testRange(MIN_2_BYTE, MIN_3_BYTE).mapToObj(TestData::toHexIntAndString); }
    static Stream<Object[]> _3_ByteChars() {
        return concat(
                testRange(MIN_3_BYTE, MIN_HIGH_SURROGATE),
                testRange(MAX_LOW_SURROGATE+1, MIN_4_BYTE)
        ).mapToObj(TestData::toHexIntAndString);
    }
    static Stream<Object[]> _4_ByteChars() { return testRange(MIN_4_BYTE, MIN_4_BYTE+0xFFFF).mapToObj(TestData::toHexIntAndString); }

    @ParameterizedTest(name = "Encode {0} {2}") @MethodSource("_1_ByteChars")
    void testDecode1ByteChar(String hex, int codepoint, String c) { checkDecoding(codepoint, c); }

    @ParameterizedTest(name = "Encode {0} {2}") @MethodSource("_1_ByteChars")
    void testEncode1ByteChar(String hex, int codepoint, String c) { checkEncoding(codepoint, c); }

    @ParameterizedTest(name = "Encode {0} {2}") @MethodSource("_2_ByteChars")
    void testDecode2ByteChar(String hex, int codepoint, String c) { checkDecoding(codepoint, c); }

    @ParameterizedTest(name = "Encode {0} {2}") @MethodSource("_2_ByteChars")
    void testEncode2ByteChar(String hex, int codepoint, String c) { checkEncoding(codepoint, c); }

    @ParameterizedTest(name = "Encode {0} {2}") @MethodSource("_3_ByteChars")
    void testDecode3ByteChar(String hex, int codepoint, String c) { checkDecoding(codepoint, c); }

    @ParameterizedTest(name = "Encode {0} {2}") @MethodSource("_3_ByteChars")
    void testEncode3ByteChar(String hex, int codepoint, String c) { checkEncoding(codepoint, c); }

    @ParameterizedTest(name = "Encode {0} {2}") @MethodSource("_4_ByteChars")
    void testDecode4ByteChar(String hex, int codepoint, String c) { checkDecoding(codepoint, c); }

    @ParameterizedTest(name = "Encode {0} {2}") @MethodSource("_4_ByteChars")
    void testEncode4ByteChar(String hex, int codepoint, String c) { checkEncoding(codepoint, c); }

    private void checkDecoding(int codepoint, String expected) {
        ByteBuffer bb = UTF_8.encode(expected);
        ReadBuffer in = out.writeBytes(bb.array(), bb.arrayOffset(), bb.remaining()).trim().newReadBuffer();
        String actual = "" + codec.readChar(in);
        // if this is above the basic multilingual plane, we will need to read another char
        if (isSupplementaryCodePoint(codepoint)) {
            assertFalse(codec.readFinished(), "there should be an unread low surrogate");
            assertNotEquals(0, in.available(), "at least 1 input byte should be left to read");
            // now read the low surrogate
            actual += codec.readChar(in);
        }
        assertEquals(expected, actual, "the character should have been decoded correctly");
        assertTrue(codec.readFinished(), "there should be no outstanding low surrogate");
        assertEquals(0, in.available(), () -> "readChar() should read all the encoded bytes\n" + in.dumpAllDataWithPosition());
        codec.assertNoBufferedCharData();
    }

    private void checkEncoding(int codepoint, String c) {
        codec.writeChar(c.charAt(0), out);
        if (isSupplementaryCodePoint(codepoint)) {
            System.out.println("### supplementary char: " + c);
            // this codepoint is above the basic multilingual plane
            // so there should be a pair of chars to write
            assertFalse(codec.writeFinished(), "there should be an unwritten high surrogate");
            codec.writeChar(c.charAt(1), out);
        }
        ReadBuffer in = out.trim().readFromStart();
        assertTrue(codec.writeFinished(), "there should be no unwritten high surrogate");
        int expectedByteCount = countExpectedBytes(codepoint);
        assertEquals(expectedByteCount, in.available(), "the right number of bytes should be written");
        String actual = UTF_8.decode(ByteBuffer.wrap(in.readBytes(new byte[expectedByteCount]))).toString();
        assertEquals(c, actual);
        codec.assertNoBufferedCharData();
    }

    static int countExpectedBytes(int codepoint) {
        return codepoint < MIN_2_BYTE ? 1 : codepoint < MIN_3_BYTE ? 2 : codepoint < MIN_4_BYTE ? 3 : 4;
    }

    static boolean isSurrogate(int cp) { return MIN_HIGH_SURROGATE <= cp && cp <= MAX_LOW_SURROGATE; }
}
