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

import static org.apache.yoko.orb.codecs.Util.ASCII_REPLACEMENT_CHAR;
import static org.apache.yoko.orb.codecs.Util.UNICODE_REPLACEMENT_CHAR;
import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class AbstractSimpleCodecTest {
    @FunctionalInterface  interface CharWriter{ void writeTo(WriteBuffer w, char c); }
    @FunctionalInterface  interface CharReader{ char readFrom(ReadBuffer w); }
    final WriteBuffer out = Buffer.createWriteBuffer(1024);
    final ReadBuffer in = out.readFromStart();
    final SimpleCodec codec;
    final CharWriter charWriter;
    final CharReader charReader;

    AbstractSimpleCodecTest(String name, CharWriter charWriter, CharReader charReader) {
        this.codec = (SimpleCodec) CharCodec.forName(name);
        this.charWriter = charWriter;
        this.charReader = charReader;
    }

    void assertValidChar(char c) { assertDecoding(c, c); assertEncoding(c, c); }

    void assertInvalidChar(char c) { assertDecoding(c, UNICODE_REPLACEMENT_CHAR); assertEncodingFails(c);}

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
        assertDecoding(c, UNICODE_REPLACEMENT_CHAR);
        assertEncoding(c, isSingleByte() ? ASCII_REPLACEMENT_CHAR : UNICODE_REPLACEMENT_CHAR);
    }

    abstract boolean isSingleByte();
}
