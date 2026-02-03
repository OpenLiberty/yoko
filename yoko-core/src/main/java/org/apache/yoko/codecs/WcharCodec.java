/*
 * Copyright 2026 IBM Corporation and others.
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

package org.apache.yoko.codecs;

import org.apache.yoko.io.ReadBuffer;
import org.apache.yoko.io.WriteBuffer;

import java.nio.charset.UnsupportedCharsetException;
import java.util.Optional;

public interface WcharCodec extends CharCodec {
    static WcharCodec getDefault() { return SimpleWcharCodec.UTF_16; }

    static WcharCodec forName(String name) {
        return Optional.of(name)
                .map(CharCodec::forName)
                .filter(WcharCodec.class::isInstance)
                .map(WcharCodec.class::cast)
                .orElseThrow(() -> new UnsupportedCharsetException(name + " not supported for wchar"));
    }

    static WcharCodec forRegistryId(int id) {
        return Optional.of(id)
                .map(CharCodec::forRegistryId)
                .filter(WcharCodec.class::isInstance)
                .map(WcharCodec.class::cast)
                .orElseThrow(() -> new UnsupportedCharsetException("Charset with registry id " + id + " not supported for wchar"));
    }

    @Override
    default int charSize() { return 2; }

    @Override
    default char readChar(ReadBuffer in) { return in.readChar(); } // UTF-16 chars are already in Java format

    @Override
    default int octetCount(char c) { return 2; }

    int octetCount(String s);

    int octetCountLengthsAndWchars(int numChars);

    @Override
    default void writeChar(char c, WriteBuffer out) { out.writeChar(c); } // Java chars are already in UTF-16 format

    /**
     * In GIOP 1.0 and GIOP 1.1, wchars are encoded as EXACTLY 2 bytes,
     * aligned on a 2-byte boundary, and in the message/encapsulation byte ordering.
     * The alignment must be ensured by the caller.
     */
    char readCharWithEndianFlag(ReadBuffer in, boolean swapBytes);

    // We only ever write big-endian, so no writeWithEndianFlag() method is needed

    /**
     * In GIOP 1.2, the wchar encoding requires a length byte before each wchar.
     * In UTF-16, this is typically 2, and the char is encoded as 2 bytes.
     * However, it can be 4, if the wchar is preceded by a BOM.
     * This method should be used to read a BOM and then a wchar, i.e. if the length byte was 4.
     */
    char readLengthAndChar(ReadBuffer in);

    /**
     * In GIOP 1.2, wchars are preceded by a single octet.
     * This contains the number of octets in the char is encoding,
     * including any necessary BOM.
     */
    void writeLengthAndChar(char c, WriteBuffer out);

    /**
     * In GIOP 1.2, for UTF-16, there may be a byte-order marker to indicate the endianness of the encoded bytes.
     * This BOM can only occur at the start of a string, or before an individual character.
     * For a string, a caller should call this method to determine the ordering.
     *
     * @return the endian-informed reader with which to read in the rest of the string.
     */
    CharReader beginToReadString(ReadBuffer in);

    /**
     * Under certain circumstances, it may be necessary to write a BOM at the start of a string.
     * To support this, this method will be called to write the first character of a wstring.
     */
    void beginToWriteString(char firstChar, WriteBuffer out);

    /** Provides an identical object that can be used concurrently with this one */
    default WcharCodec getInstanceOrCopy() { return this; }
}
