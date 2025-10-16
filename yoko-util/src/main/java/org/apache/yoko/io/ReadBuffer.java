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

import org.apache.yoko.util.HexConverter;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;

import static org.apache.yoko.util.Hex.formatHexPara;

public final class ReadBuffer extends Buffer<ReadBuffer> {
    ReadBuffer(Core core) { super(core); }

    public boolean empty() { return length() == position; }

    public byte peekByte() { return checkedBytes(1)[position]; }

    public byte readByte() { return checkedBytes(1)[position++]; }

    public char readByteAsChar() { return (char) (readByte()&0xff); }

    public byte[] readBytes(byte[] buffer) { return readBytes(buffer, 0, buffer.length); }

    public byte[] readBytes(byte[] buffer, int offset, int length) {
        System.arraycopy(checkedBytes(length), position, buffer, offset, length);
        position += length;
        return buffer;
    }

    /** Read the available bytes into the provided write buffer. */
    public WriteBuffer readBytes(WriteBuffer buffer) { return buffer.writeBytes(checkedBytes(0), position, available()); }

    public byte[] copyRemainingBytes() { return copyOfRange(checkedBytes(0), position, length()); }

    public char peekChar() {
        final byte[] data = checkedBytes(2);
        return (char)((data[position] << 8) | (data[position + 1] & 0xff));
    }

    public char readChar() {
        final byte[] data = checkedBytes(2);
        return (char) ((data[position++] << 8) | (data[position++] & 0xff));
    }

    public char readChar_LE() {
        final byte[] data = checkedBytes(2);
        return (char) ((data[position++] & 0xff) | (data[position++] << 8));
    }

    public short readShort() {
        final byte[] data = checkedBytes(2);
        return (short) ((data[position++] << 8) | (data[position++] & 0xff));
    }

    public short readShort_LE() {
        final byte[] data = checkedBytes(2);
        return (short) ((data[position++] & 0xff) | (data[position++] << 8));
    }

    public int readInt() {
        final byte[] data = checkedBytes(4);
        return  0
                | (0xFF & data[position++]) << 030
                | (0xFF & data[position++]) << 020
                | (0xFF & data[position++]) << 010
                | (0xFF & data[position++]) << 000;
    }

    public int readInt_LE() {
        final byte[] data = checkedBytes(4);
        return  0
                | (0xFF & data[position++]) << 000
                | (0xFF & data[position++]) << 010
                | (0xFF & data[position++]) << 020
                | (0xFF & data[position++]) << 030;
    }

    public long readLong() {
        final byte[] data = checkedBytes(8);
        return  0
                | (0xFFL & data[position++]) << 070
                | (0xFFL & data[position++]) << 060
                | (0xFFL & data[position++]) << 050
                | (0xFFL & data[position++]) << 040
                | (0xFFL & data[position++]) << 030
                | (0xFFL & data[position++]) << 020
                | (0xFFL & data[position++]) << 010
                | (0xFFL & data[position++]) << 000;
    }

    public long readLong_LE() {
        final byte[] data = checkedBytes(8);
        return  0
                | (0xFFL & data[position++]) << 000
                | (0xFFL & data[position++]) << 010
                | (0xFFL & data[position++]) << 020
                | (0xFFL & data[position++]) << 030
                | (0xFFL & data[position++]) << 040
                | (0xFFL & data[position++]) << 050
                | (0xFFL & data[position++]) << 060
                | (0xFFL & data[position++]) << 070;
    }

    public float readFloat() { return Float.intBitsToFloat(readInt()); }
    public float readFloat_LE() { return Float.intBitsToFloat(readInt_LE()); }
    public double readDouble() { return Double.longBitsToDouble(readLong()); }
    public double readDouble_LE() { return Double.longBitsToDouble(readLong_LE()); }

    public String toAscii() {
        return HexConverter.octetsToAscii(checkedBytes(0), available());
    }

    public String dumpRemainingData() {
        StringBuilder dump = new StringBuilder();
        dump.append(String.format("Read pos=0x%x Core len=0x%x Remaining core data=%n%n", position, length()));
        return formatHexPara(checkedBytes(0), position, available(), dump).toString();
    }

    public String dumpAllDataWithPosition() {
        return dumpAllDataWithPosition(new StringBuilder(), "pos").toString();
    }

    public StringBuilder dumpAllDataWithPosition(StringBuilder sb, String label) {
        final byte[] data = checkedBytes(0);
        formatHexPara(data, 0, position, sb);
        sb.append(String.format("%n       >>>>>>>> %4s: 0x%08X  <<<<<<<<%n", label, position));
        formatHexPara(data, position, available(), sb);
        return sb;
    }

    public void dumpSomeData(StringBuilder sb, String indent, int len) {
        formatHexPara(indent, checkedBytes(0), position, len, sb);
    }

    public ReadBuffer writeTo(OutputStream out) throws IOException {
        final byte[] data = checkedBytes(0);
        try {
            out.write(data, position, available());
            out.flush();
            position = length();
            return this;
        } catch (InterruptedIOException ex) {
            position += ex.bytesTransferred;
            throw ex;
        }
    }

    public ReadBuffer rewindToStart() {
        position = 0;
        return this;
    }

    public ReadBuffer align(AlignmentBoundary boundary) {
        position = boundary.newIndex(position);
        return this;
    }

    public ReadBuffer skipBytes(int n) {
        int newPos = position + n;
        if (newPos < 0) throw new IndexOutOfBoundsException(); // n can be negative!
        if (newPos > length()) throw new IndexOutOfBoundsException();
        position = newPos;
        return this;
    }

    // TODO: should this (like the parent method) reset the position to 0?
    public ReadBuffer newReadBuffer() { return clone(); }
}
