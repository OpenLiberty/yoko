/*
 * Copyright 2021 IBM Corporation and others.
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
package org.apache.yoko.io;

import org.apache.yoko.io.Buffer;
import org.apache.yoko.io.ReadBuffer;
import org.apache.yoko.io.WriteBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

class BufferTest {
    WriteBuffer writeBuffer;
    public static final String TEXT = "By these he was generally called by the euphonious name of 'Old Bramble-Buffer,' although the title by which he was known among his own kith and kin was probably quite different, and has not been handed down to posterity.";
    private byte[] bytes;
    private byte[][] snippets;

    @BeforeEach
    public void setup() {
        assertThat(TEXT.length(), greaterThan(200));
        bytes = TEXT.getBytes(UTF_8);
        assertThat(bytes.length, equalTo(TEXT.length()));
        snippets = new byte[][]{
                TEXT.substring(0, 50).getBytes(UTF_8),
                TEXT.substring(50, 100).getBytes(UTF_8),
                TEXT.substring(100, 150).getBytes(UTF_8),
                TEXT.substring(150, 200).getBytes(UTF_8),
                {},
                {},
                TEXT.substring(200).getBytes(UTF_8),
                {}
        };
        writeBuffer = Buffer.createWriteBuffer();
    }

    @Test
    public void testReadFromStartWithNoData() {
        assertThat(writeBuffer.readFromStart().available(), equalTo(0));
    }

    @Test
    public void testReadFromStartWithData() {
        writeBuffer.ensureAvailable(bytes.length);
        writeBuffer.writeBytes(bytes);
        assertThat(writeBuffer.readFromStart().readByte(), equalTo((byte)'B'));
        assertThat(writeBuffer.readFromStart().available(), equalTo(bytes.length));
        assertBufferContains(TEXT);
    }

    @Test
    public void testWriteSeveralByteArrays() {
        int bytesWritten = 0;
        for (byte[] snippet: snippets) {
            writeBuffer.ensureAvailable(snippet.length);
            writeBuffer.writeBytes(snippet);
            bytesWritten += snippet.length;
            assertBufferContains(TEXT.substring(0, bytesWritten));
        }
        assertBufferContains(TEXT);
    }

    @Test
    public void testWriteByteArrayInParts() {
        writeBuffer.ensureAvailable(bytes.length);
        writeBuffer.writeBytes(bytes, 0, 100);
        writeBuffer.writeBytes(bytes, 100, 100);
        writeBuffer.writeBytes(bytes, 200, bytes.length - 200);
        assertBufferContains(TEXT);
    }

    @Test
    public void testReadBytes() {
        int bytesWritten = 0;
        for (byte[] snippet: snippets) {
            writeBuffer.ensureAvailable(snippet.length);
            Buffer.createReadBuffer(snippet).readBytes(writeBuffer);
            bytesWritten += snippet.length;
            assertBufferContains(TEXT.substring(0, bytesWritten));
        }
        assertBufferContains(TEXT);
    }

    @Test
    public void testReadBytesAfterSkipping() {
        ReadBuffer rb = Buffer.createReadBuffer(TEXT.getBytes(UTF_8));
        assertThat(rb.available(), equalTo(TEXT.length()));
        ReadBuffer rb2 = rb.clone();
        rb2.skipBytes(100);
    }

    public void assertBufferContains(String expected) {
        final ReadBuffer readBuffer = writeBuffer.readFromStart();
        final String actual = new String(readBuffer.copyRemainingBytes(), UTF_8);
        assertThat(actual, equalTo(expected));
    }
}
