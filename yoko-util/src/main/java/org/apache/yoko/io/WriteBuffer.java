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
package org.apache.yoko.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.Arrays;
import java.util.logging.Logger;

@SuppressWarnings({"PointlessBitwiseExpression", "OctalInteger"})
public final class WriteBuffer extends Buffer<WriteBuffer> {
    private static final byte PAD_BYTE = (byte) 0xBD;
    /**
     * The base 2 log of the size of our padding array.
     * The array size must be a power of 2, so that bitwise
     * operations can be used in place of arithmetic operations.
     */
    private static final int PADDING_POWER = 5;
    private static final byte[] PADDING = new byte[1<<PADDING_POWER];
    static { Arrays.fill(PADDING, PAD_BYTE); }

    WriteBuffer(Core core) { super(core); }

    public boolean readFrom(InputStream in) throws IOException {
        final byte[] data = checkedBytes(0);
        try {
            int result = in.read(data, position, available());
            if (result <= 0) return false;
            position += result;
            assert position <= length();
            return true;
        } catch (InterruptedIOException ex) {
            position += ex.bytesTransferred;
            throw ex;
        }
    }

    public WriteBuffer readFrom(org.omg.CORBA.portable.InputStream source) {
        source.read_octet_array(checkedBytes(0), position, available());
        position = length();
        return this;
    }

    public WriteBuffer writeBytes(byte[] bytes) {
        return writeBytes(bytes, 0, bytes.length);
    }

    public WriteBuffer writeBytes(byte[] bytes, int offset, int length) {
        System.arraycopy(bytes, offset, checkedBytes(length), position, length);
        position += length;
        return this;
    }

    public WriteBuffer writeByte(int i) {
        checkedBytes(1)[position++] = (byte)i;
        return this;
    }

    public WriteBuffer writeByte(byte b) {
        checkedBytes(1)[position++] = b;
        return this;
    }

    public WriteBuffer writeChar(char value) {
        byte[] data = checkedBytes(2);
        data[position++] = (byte) (value >> 010);
        data[position++] = (byte) (value >> 000);
        return this;
    }

    public WriteBuffer writeShort(short value) {
        byte[] data = checkedBytes(2);
        data[position++] = (byte) (value >> 010);
        data[position++] = (byte) (value >> 000);
        return this;
    }

    public WriteBuffer writeInt(int value) {
        byte[] data = checkedBytes(4);
        data[position++] = (byte) (value >> 030);
        data[position++] = (byte) (value >> 020);
        data[position++] = (byte) (value >> 010);
        data[position++] = (byte) (value >> 000);
        return this;
    }

    public WriteBuffer writeLong(long value) {
        byte[] data = checkedBytes(8);
        data[position++] = (byte) (value >> 070);
        data[position++] = (byte) (value >> 060);
        data[position++] = (byte) (value >> 050);
        data[position++] = (byte) (value >> 040);
        data[position++] = (byte) (value >> 030);
        data[position++] = (byte) (value >> 020);
        data[position++] = (byte) (value >> 010);
        data[position++] = (byte) (value >> 000);
        return this;
    }

    /**
     * Leaves a 4 byte space to write a length. When {@link SimplyCloseable#close()} is called,
     * the number of intervening bytes is written as a length to the remembered location.
     * @param logger the logger to use to log the operations - must not be null
     */
    public SimplyCloseable recordLength(final Logger logger) {
        final int lengthPosition = position;
        logger.finest("Writing a gap value for a length at offset " + lengthPosition);

        pad(4);
        return new SimplyCloseable() {
            public void close() {
                final int length = position - (lengthPosition + 4);
                byte[] data = uncheckedBytes();
                data[lengthPosition + 0] = (byte) (length >> 030);
                data[lengthPosition + 1] = (byte) (length >> 020);
                data[lengthPosition + 2] = (byte) (length >> 010);
                data[lengthPosition + 3] = (byte) (length >> 000);
                logger.finest("Wrote a length value of " + length + " at offset " + lengthPosition);
            }
        };
    }

    private WriteBuffer pad(int n) {
        // fastpath for n < 8
        final byte padByte = PAD_BYTE;
        switch (n) {
        case 7: writeByte(padByte);
        case 6: writeByte(padByte);
        case 5: writeByte(padByte);
        case 4: writeByte(padByte);
        case 3: writeByte(padByte);
        case 2: writeByte(padByte);
        case 1: writeByte(padByte);
        case 0: break;
        default:
            // write as many full copies of PADDING as required
            for (int i = n >> PADDING_POWER; i > 0; i--) writeBytes(PADDING);
            // write any remaining bytes of PADDING
            writeBytes(PADDING, 0, n & ((1 << PADDING_POWER) - 1));
        }
        return this;
    }

    /**
     * Ensure there is space to write from the current position,
     * taking an alignment boundary into account. The writer will be
     * aligned on the provided boundary when this method returns.
     * @param size the number of bytes to be written
     * @param boundary the size of boundary to align on
     * @return <code>true</code> iff an existing buffer had to be resized
     */
    public boolean ensureAvailable(int size, AlignmentBoundary boundary) {
        final int gap = boundary.gap(position);
        try { return ensureAvailable(gap + size); }
        finally { this.pad(gap); }
    }

    /**
     * Ensure there is space to write from the current position.
     * @param size the number of bytes to be written
     * @return <code>true</code> iff an existing buffer had to be resized
     */
    public boolean ensureAvailable(int size) {
        final int shortfall = size - available();
        return shortfall > 0 && growBy(shortfall);
    }

    public WriteBuffer padAll() { return pad(length()); }

    public WriteBuffer trim() { return super.trim();  }

    public ReadBuffer readFromStart() { return newReadBuffer(); }
}
