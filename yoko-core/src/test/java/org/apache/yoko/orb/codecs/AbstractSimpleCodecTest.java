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
import org.omg.CORBA.DATA_CONVERSION;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.stream.IntStream.iterate;
import static org.apache.yoko.orb.codecs.CharCodec.REPLACEMENT_CHAR;
import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class AbstractSimpleCodecTest {
    @FunctionalInterface  interface CharWriter{ void writeTo(WriteBuffer w, char c); }
    @FunctionalInterface  interface CharReader{ char readFrom(ReadBuffer w); }
    final WriteBuffer out = Buffer.createWriteBuffer(1024);
    final ReadBuffer in = out.readFromStart();
    final SimpleCodec codec;
    final CharWriter charWriter;
    final CharReader charReader;

    AbstractSimpleCodecTest(SimpleCodec codec, CharWriter charWriter, CharReader charReader) {
        this.codec = codec;
        this.charWriter = charWriter;
        this.charReader = charReader;
    }

    static Stream<Object[]> asciiChars() { return rangeToTest(0x00,0x80).mapToObj(AbstractSimpleCodecTest::convertTo3Args); }

    static Stream<Object[]> isoLatinChars() { return rangeToTest(0x80, 0x100).mapToObj(AbstractSimpleCodecTest::convertTo3Args); }

    static Stream<Object[]> bmpChars() {
        return Stream.of(rangeToTest(0x100, 0xD800),
                        rangeToTest(0xE000, 0x10000))
                .flatMapToInt(s -> s)
                .mapToObj(AbstractSimpleCodecTest::convertTo3Args);
    }

    static Stream<Object[]> highSurrogates() {
        return  IntStream.of(0xD800, 0xD801, 0xDBFE, 0xDBFF).mapToObj(AbstractSimpleCodecTest::convertTo3Args);
    }

    static Stream<Object[]> lowSurrogates() {
        return  IntStream.of(0xDC00, 0xDC01, 0xDFFE, 0xDFFF).mapToObj(AbstractSimpleCodecTest::convertTo3Args);
    }

    static Stream<Object[]> wideChars() { return Stream.of(bmpChars(), highSurrogates(), lowSurrogates()).flatMap(s -> s); }

    static Object[] convertTo3Args(int i) { return new Object[]{String.format("0x%X", i), i, (char) i}; }

    void assertValidChar(char c) { assertDecoding(c, c); assertEncoding(c, c); }

    void assertInvalidChar(char c) { assertDecoding(c, REPLACEMENT_CHAR); assertEncodingFails(c);}

    void assertDecoding(char c, char expected) {
        charWriter.writeTo(out, c);
        char actual = codec.readChar(in);
        assertEquals(expected, actual);
        // there is never any state to clean up so this should always work
        codec.assertNoBufferedCharData();
    }

    void assertEncoding(char c, char expected) {
        codec.writeChar(c, out);
        char actual = charReader.readFrom(in);
        assertEquals(expected, actual);
    }

    void assertEncodingFails(char c) {
        assertThrows(DATA_CONVERSION.class, () -> codec.writeChar(c, out));
    }

    static IntStream rangeToTest(int start, int finish) {
        final int STEP = 5;
        int count = (finish - start) / STEP - 2;
        assert count > 0;
        IntStream beginning = IntStream.range(start, start+STEP);
        IntStream middle = iterate(start, n -> n + STEP).limit(count);
        IntStream end = IntStream.range(finish-STEP, finish);
        return Stream.of(beginning, middle, end).flatMapToInt(s -> s);
    }
}
