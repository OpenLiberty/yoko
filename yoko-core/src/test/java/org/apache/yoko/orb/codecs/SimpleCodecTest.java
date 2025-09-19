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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.stream.IntStream.range;
import static org.apache.yoko.orb.codecs.CharCodec.REPLACEMENT_CHAR;
import static org.apache.yoko.orb.codecs.SimpleCodec.ISO_LATIN_1;
import static org.apache.yoko.orb.codecs.SimpleCodec.US_ASCII;
import static org.apache.yoko.orb.codecs.SimpleCodec.UTF_16;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public abstract class SimpleCodecTest {
    @FunctionalInterface  interface CharWriter{ void writeTo(WriteBuffer w, char c); }
    final WriteBuffer out = Buffer.createWriteBuffer(1024);
    final ReadBuffer in = out.readFromStart();
    final SimpleCodec codec;
    final CharWriter charWriter;

    SimpleCodecTest(SimpleCodec codec, CharWriter charWriter) {
        this.codec = codec;
        this.charWriter = charWriter;
    }

    static Stream<Object[]> asciiChars() { return range(0x00,0x80).mapToObj(SimpleCodecTest::convertTo3Args); }
    static Stream<Object[]> isoLatinChars() { return range(0x80, 0x100).mapToObj(SimpleCodecTest::convertTo3Args); }
    static Stream<Object[]> bmpChars() {
        return Stream.of(range(51,11059).map(i->5*i),  // in range 0x0100..0xD7FF (incl)
                        range(3374,3855).map(i->17*i)) // in range 0xE000..0xFFFF (incl)
                .flatMapToInt(Function.identity())
                .mapToObj(SimpleCodecTest::convertTo3Args);
    }
    static Stream<Object[]> highSurrogates() {
        return  IntStream.of(0xD800, 0xD801, 0xDBFE, 0xDBFF).mapToObj(SimpleCodecTest::convertTo3Args);
    }
    static Stream<Object[]> lowSurrogates() {
        return  IntStream.of(0xDC00, 0xDC01, 0xDFFE, 0xDFFF).mapToObj(SimpleCodecTest::convertTo3Args);
    }
    static Object[] convertTo3Args(int i) { return new Object[]{String.format("0x%X", i), i, (char) i}; }

    public static class UsAsciiTest extends SimpleCodecTest {
        UsAsciiTest() { super(US_ASCII, WriteBuffer::writeByte); }

        @ParameterizedTest(name = "Decode {0} \"{2}\"")
        @MethodSource("asciiChars")
        public void decodeAscii(String hex, int codepoint, char c) { assertDecodesCorrectly(c); }

        @ParameterizedTest(name = "Decode {0} \"{2}\"")
        @MethodSource("isoLatinChars")
        void testDecodeIsoLatin1(String hex, int codepoint, char c) { assertDoesNotDecode(c); }
    }

    static class IsoLatin1Test extends SimpleCodecTest {
        IsoLatin1Test() { super(ISO_LATIN_1, WriteBuffer::writeByte); }

        @ParameterizedTest(name = "Decode {0} \"{2}\"")
        @MethodSource("asciiChars")
        public void testDecodeAscii(String hex, int codepoint, char c) {
            assertDecodesCorrectly(c);
        }

        @ParameterizedTest(name = "Decode {0} \"{2}\"")
        @MethodSource("isoLatinChars")
        void testDecodeIsoLatin1(String hex, int codepoint, char c) {
            assertDecodesCorrectly(c);
        }
    }

    static class Utf16Test extends SimpleCodecTest {
        Utf16Test() { super(UTF_16, WriteBuffer::writeChar); }

        @ParameterizedTest(name = "Decode {0} \"{2}\"")
        @MethodSource("asciiChars")
        public void testDecodeAsciiAsUtf16(String hex, int codepoint, char c) { assertDecodesCorrectly(c); }

        @ParameterizedTest(name = "Decode {0} \"{2}\"")
        @MethodSource("isoLatinChars")
        void testDecodeIsoLatin1AsUtf16(String hex, int codepoint, char c)  { assertDecodesCorrectly(c); }

        @ParameterizedTest(name = "Decode {0} \"{2}\"")
        @MethodSource("bmpChars")
        void testDecodeBmpAsUtf16(String hex, int codepoint, char c)  { assertDecodesCorrectly(c); }

        @ParameterizedTest(name = "Decode {0} \"{2}\"")
        @MethodSource("highSurrogates")
        void testDecodeHighSurrogatesAsUtf16(String hex, int codepoint, char c)  { assertDecodesCorrectly(c); }

        @ParameterizedTest(name = "Decode {0} \"{2}\"")
        @MethodSource("lowSurrogates")
        void testDecodeLowSurrogatesAsUtf16(String hex, int codepoint, char c)  { assertDecodesCorrectly(c); }
    }

    void assertDecodesCorrectly(char c) { assertDecoding(c, c); }
    void assertDoesNotDecode(char c) { assertDecoding(c, REPLACEMENT_CHAR); }
    void assertDecoding(char c, char expected) {
        charWriter.writeTo(out, c);
        char actual = codec.readChar(in);
        assertEquals(expected, actual);
        // there is never any state to clean up so this should always work
        codec.close();
        // for these codecs, copy should simply return the same instance
        assertSame(codec, codec.copy());
    }
}
