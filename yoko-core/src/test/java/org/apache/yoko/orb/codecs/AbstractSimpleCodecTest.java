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

import static org.apache.yoko.orb.codecs.Util.ASCII_REPLACEMENT_CHAR;
import static org.apache.yoko.orb.codecs.Util.UNICODE_REPLACEMENT_CHAR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class AbstractSimpleCodecTest {
    @FunctionalInterface  interface ExpectedCharWriter { void writeTo(WriteBuffer w, char c); }
    @FunctionalInterface  interface ExpectedCharReader { char readFrom(ReadBuffer w); }
    private WriteBuffer out;
    final SimpleCodec codec;
    final ExpectedCharWriter expectedCharWriter;
    final ExpectedCharReader expectedCharReader;

    AbstractSimpleCodecTest(String name, ExpectedCharWriter expectedCharWriter, ExpectedCharReader expectedCharReader) {
        this.codec = (SimpleCodec) CharCodec.forName(name);
        this.expectedCharWriter = expectedCharWriter;
        this.expectedCharReader = expectedCharReader;
    }

    @BeforeEach
    void newWriteBuffer() { out = Buffer.createWriteBuffer(16); }

    void writeExpectedChar(char expectedChar) { expectedCharWriter.writeTo(out, expectedChar); }

    ReadBuffer getReadBuffer() { return out.trim().readFromStart(); }

    void assertValidChar(char c) { assertDecoding(c, c); newWriteBuffer(); assertEncoding(c, c); }

    void assertInvalidChar(char c) { assertDecoding(c, UNICODE_REPLACEMENT_CHAR); newWriteBuffer(); assertEncodingFails(c);}

    void assertDecoding(char c, char expected) {
        writeExpectedChar(c);
        ReadBuffer in = getReadBuffer();
        char actual = codec.readChar(in);
        assertEquals(expected, actual);
        // there is never any state to clean up so this should always work
        codec.assertNoBufferedCharData();
        assertTrue(in.isComplete());
    }

    void assertEncoding(char c, char expected) {
        codec.writeChar(c, out);
        ReadBuffer in = getReadBuffer();
        assertEquals(expected, expectedCharReader.readFrom(in));
        codec.assertNoBufferedCharData();
        assertTrue(in.isComplete());
        // try it using writeNextChar() too
        newWriteBuffer();
        codec.writeNextChar(c, out);
        in = getReadBuffer();
        assertEquals(expected, expectedCharReader.readFrom(in));
    }

    void assertEncodingFails(char c) {
        assertDecoding(c, UNICODE_REPLACEMENT_CHAR);
        newWriteBuffer();
        assertEncoding(c, isDoubleByte() ? UNICODE_REPLACEMENT_CHAR : ASCII_REPLACEMENT_CHAR);
    }

    abstract boolean isDoubleByte();
}
