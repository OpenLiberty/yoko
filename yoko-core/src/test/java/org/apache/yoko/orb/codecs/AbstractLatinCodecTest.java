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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.stream.IntStream.range;
import static org.apache.yoko.orb.codecs.AlternateLatinCodec.ISO_LATIN_2;
import static org.apache.yoko.orb.codecs.AlternateLatinCodec.ISO_LATIN_3;
import static org.apache.yoko.orb.codecs.AlternateLatinCodec.ISO_LATIN_4;
import static org.apache.yoko.orb.codecs.AlternateLatinCodec.ISO_LATIN_5;
import static org.apache.yoko.orb.codecs.AlternateLatinCodec.ISO_LATIN_7;
import static org.apache.yoko.orb.codecs.AlternateLatinCodec.ISO_LATIN_9;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

@TestInstance(PER_CLASS)
abstract class AbstractLatinCodecTest {
    final CharCodec codec;
    final Charset charset;
    // create a buffer of every possible byte, in unsigned increasing byte order
    final ByteBuffer inputBytes = range(0, 256)
            .collect(() -> ByteBuffer.allocate(256), (bb,b) -> bb.put(b,(byte)b), (bb1,bb2) -> {throw null;})
            .asReadOnlyBuffer();
    final CharBuffer expectedChars;
    final ByteBuffer expectedBytes;
    final WriteBuffer writeBuffer = Buffer.createWriteBuffer();
    final ReadBuffer readBuffer = writeBuffer.readFromStart();

    AbstractLatinCodecTest(CharCodec codec, Charset charset) {
        this.codec = codec;
        this.charset = charset;
        expectedChars = charset.decode(inputBytes).asReadOnlyBuffer();
        expectedBytes = charset.encode(expectedChars);
    }

    @BeforeEach
    void resetBuffers() {
        writeBuffer.rewind(0);
        writeBuffer.ensureAvailable(1);
        readBuffer.rewind(0);
    }

    Stream<Object[]> args() { return range(0,256).mapToObj(i -> new Object[]{String.format("0x%02X", i), i, expectedChars.get(i), expectedBytes.get(i)}); }

    @ParameterizedTest(name = "Convert char {0} ({2})")
    @MethodSource("args")
    void testDecode(String hex, int b, char expectedChar, byte expectedByte) {
        char expected = expectedChars.get(b);
        writeBuffer.writeByte(b);
        char actual = codec.readChar(readBuffer);
        assertEquals(expected, actual);
    }
}

class IsoLatin2Test extends AbstractLatinCodecTest {  IsoLatin2Test() {super(ISO_LATIN_2, Charset.availableCharsets().get("ISO-8859-2")); } }
class IsoLatin3Test extends AbstractLatinCodecTest {  IsoLatin3Test() {super(ISO_LATIN_3, Charset.availableCharsets().get("ISO-8859-3")); } }
class IsoLatin4Test extends AbstractLatinCodecTest {  IsoLatin4Test() {super(ISO_LATIN_4, Charset.availableCharsets().get("ISO-8859-4")); } }
class IsoLatin5Test extends AbstractLatinCodecTest {  IsoLatin5Test() {super(ISO_LATIN_5, Charset.availableCharsets().get("ISO-8859-5")); } }
class IsoLatin7Test extends AbstractLatinCodecTest {  IsoLatin7Test() {super(ISO_LATIN_7, Charset.availableCharsets().get("ISO-8859-7")); } }
class IsoLatin9Test extends AbstractLatinCodecTest {  IsoLatin9Test() {super(ISO_LATIN_9, Charset.availableCharsets().get("ISO-8859-9")); } }
