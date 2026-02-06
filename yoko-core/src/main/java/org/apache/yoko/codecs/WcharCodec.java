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
import org.apache.yoko.orb.OB.CodeSetInfo;

public interface WcharCodec {
    @FunctionalInterface interface WcharReader { char readWchar(ReadBuffer in); }

    CodeSetInfo getCodeSetInfo();

    int octetCountWstring_1_2(String s);

    int octetCountWchars_1_2(int numChars);

    /**
     * In GIOP 1.0 and GIOP 1.1, wchars are encoded as EXACTLY 2 bytes,
     * aligned on a 2-byte boundary, and in the message/encapsulation byte ordering.
     * The alignment must be ensured by the caller.
     */
    char readWchar_1_0(ReadBuffer in, boolean swapBytes);

    /**
     * In GIOP 1.2, the wchar encoding requires a length byte before each wchar.
     * In UTF-16, this is typically 2, and the char is encoded as 2 bytes.
     * However, it can be 4, if the wchar is preceded by a BOM.
     * This method should be used to read a BOM and then a wchar, i.e. if the length byte was 4.
     */
    char readWchar_1_2(ReadBuffer in);

    /**
     * Since we only ever write big-endian, no endianness parameter is required.
     */
    default void writeWchar_1_0(char c, WriteBuffer out) { out.writeChar(c); }

    /**
     * In GIOP 1.2, wchars are preceded by a single octet.
     * This contains the number of octets in the char is encoding,
     * including any necessary BOM.
     */
    void writeWchar_1_2(char c, WriteBuffer out);

    /**
     * In GIOP 1.2, for UTF-16, there may be a byte-order marker to indicate the endianness of the encoded bytes.
     * This BOM can only occur at the start of a string, or before an individual character.
     * For a string, a caller should call this method to determine the ordering.
     *
     * @return the endian-informed reader with which to read in the rest of the string.
     */
    WcharReader beginToReadWstring_1_2(ReadBuffer in);

    /**
     * Under certain circumstances, it may be necessary to write a BOM at the start of a string.
     * To support this, this method will be called to write the first character of a wstring.
     */
    void beginToWriteWstring_1_2(char firstChar, WriteBuffer out);

    /** Provides an identical object that can be used concurrently with this one */
    default WcharCodec duplicate() { return this; }
}
