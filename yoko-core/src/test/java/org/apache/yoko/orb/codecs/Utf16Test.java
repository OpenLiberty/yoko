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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.apache.yoko.orb.codecs.SimpleCodec.UTF_16;

class Utf16Test extends AbstractSimpleCodecTest {
    Utf16Test() {
        super(UTF_16, WriteBuffer::writeChar, ReadBuffer::readChar);
    }

    @ParameterizedTest(name = "Decode {0} \"{2}\"")
    @MethodSource("asciiChars")
    public void testDecodeAsciiAsUtf16(String hex, int codepoint, char c) {
        assertValidChar(c);
    }

    @ParameterizedTest(name = "Decode {0} \"{2}\"")
    @MethodSource("isoLatinChars")
    void testDecodeIsoLatin1AsUtf16(String hex, int codepoint, char c) {
        assertValidChar(c);
    }

    @ParameterizedTest(name = "Decode {0} \"{2}\"")
    @MethodSource("bmpChars")
    void testDecodeBmpAsUtf16(String hex, int codepoint, char c) {
        assertValidChar(c);
    }

    @ParameterizedTest(name = "Decode {0} \"{2}\"")
    @MethodSource("highSurrogates")
    void testDecodeHighSurrogatesAsUtf16(String hex, int codepoint, char c) {
        assertValidChar(c);
    }

    @ParameterizedTest(name = "Decode {0} \"{2}\"")
    @MethodSource("lowSurrogates")
    void testDecodeLowSurrogatesAsUtf16(String hex, int codepoint, char c) {
        assertValidChar(c);
    }
}
