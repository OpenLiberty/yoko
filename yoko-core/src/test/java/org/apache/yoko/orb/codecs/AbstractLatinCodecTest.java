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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.stream.Stream;

import static java.util.stream.IntStream.range;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    AbstractLatinCodecTest(String name) {
        this.codec = CharCodec.forName(name);
        this.charset = Charset.forName(name);
        expectedChars = Charset.forName(name).decode(inputBytes).asReadOnlyBuffer();
        expectedBytes = Charset.forName(name).encode(expectedChars);
    }

    @BeforeEach
    void resetBuffers() {
        writeBuffer.rewind(0);
        writeBuffer.ensureAvailable(1);
        readBuffer.rewind(0);
    }

    Stream<Object[]> args() { return range(0,256).mapToObj(i -> new Object[]{String.format("0x%02X", i), i, expectedChars.get(i), expectedBytes.get(i)}); }

    @ParameterizedTest(name = "Decode byte {0} to char {2}")
    @MethodSource("args")
    public void testDecode(String hex, int b, char expectedChar, byte expectedByte) {
        char expected = expectedChars.get(b);
        writeBuffer.writeByte(b);
        char actual = codec.readChar(readBuffer);
        assertEquals(expected, actual);
    }

    @Test
    void testEndOfInput() {
        ReadBuffer in = writeBuffer.trim().readFromStart();
        assertThrows(IndexOutOfBoundsException.class, () -> codec.readChar(in));
    }
}

class Iso_8859_2Test extends AbstractLatinCodecTest { Iso_8859_2Test() { super("ISO-8859-2"); } }
class Iso_8859_3Test extends AbstractLatinCodecTest { Iso_8859_3Test() { super("ISO-8859-3"); } }
class Iso_8859_4Test extends AbstractLatinCodecTest { Iso_8859_4Test() { super("ISO-8859-4"); } }
class Iso_8859_5Test extends AbstractLatinCodecTest { Iso_8859_5Test() { super("ISO-8859-5"); } }
class Iso_8859_6Test extends AbstractLatinCodecTest { Iso_8859_6Test() { super("ISO-8859-6"); } }
class Iso_8859_7Test extends AbstractLatinCodecTest { Iso_8859_7Test() { super("ISO-8859-7"); } }
class Iso_8859_9Test extends AbstractLatinCodecTest { Iso_8859_9Test() { super("ISO-8859-9"); } }
