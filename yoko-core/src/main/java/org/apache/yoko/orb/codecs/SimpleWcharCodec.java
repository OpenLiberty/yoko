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

import static org.apache.yoko.orb.codecs.Util.BYTE_ORDER_MARKER;
import static org.apache.yoko.orb.codecs.Util.BYTE_SWAPD_MARKER;
import static org.apache.yoko.orb.codecs.Util.ZERO_WIDTH_NO_BREAK_SPACE;

enum SimpleWcharCodec implements WcharCodec {
    UTF_16 {
        public char readChar(ReadBuffer in) {
            char ch = in.readChar();
            // if this is the only available character, just return it
            if (0 == in.available()) return ch;
            switch (ch) {
                case BYTE_ORDER_MARKER: return in.readChar();
                case BYTE_SWAPD_MARKER: return in.readChar_LE();
                default: return ch;
            }
        }

        public CharReader beginString(ReadBuffer in) {
            if (in.isComplete()) return ReadBuffer::readChar;
            switch (in.readChar()) {
                case BYTE_ORDER_MARKER: return ReadBuffer::readChar;
                case BYTE_SWAPD_MARKER: return ReadBuffer::readChar_LE;
                default: in.skipBytes(-2); return ReadBuffer::readChar;
            }
        }

        public void writeChar(char c, WriteBuffer out) {
            // if the first (or single) character is a ZERO WIDTH NO-BREAK SPACE, write a BOM first
            // (this is because they are the same bytes and the first pair will be read as a BOM)
            if (ZERO_WIDTH_NO_BREAK_SPACE == c) out.writeChar(BYTE_ORDER_MARKER);
            out.writeChar(c);
        }

        public void writeNextChar(char c, WriteBuffer out) { out.writeChar(c); }
    }
}
