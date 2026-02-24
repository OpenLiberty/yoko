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

enum Util {
    ;
    /**
     * If any character cannot be read by a codec, the codec will return this character instead.
     * Where something has gone wrong with a multibyte encoding sequence in UTF8,
     * multiple instances of this char may be returned.
     */
    static final char UNICODE_REPLACEMENT_CHAR = '\uFFFD';
    /** If any character cannot be written by a byte-oriented codec, the codec will write this char instead. */
    static final char ASCII_REPLACEMENT_CHAR = '?';
    /** Like {@link #ASCII_REPLACEMENT_CHAR}, but as a {@link Byte} */
    static final Byte ASCII_REPLACEMENT_BYTE = (byte)ASCII_REPLACEMENT_CHAR;

    static final char ZERO_WIDTH_NO_BREAK_SPACE = 0xFEFF;
    static final char BYTE_ORDER_MARKER = ZERO_WIDTH_NO_BREAK_SPACE;
    static final char BYTE_SWAPD_MARKER = 0xFFFE;

    /**
     * Check whether the character being decoded is 7-bit.
     * @return the character, or {@link #UNICODE_REPLACEMENT_CHAR} if it is not 7-bit.
     */
    static char expect7bit(char c) { return c < (1<<7) ? c : UNICODE_REPLACEMENT_CHAR; }

    /**
     * Check whether the character being encoded is 7-bit.
     * @return the character or {@link #ASCII_REPLACEMENT_CHAR} if it is not 7-bit.
     */
    static char require7bit(char c) { return c < (1<<7) ? c : ASCII_REPLACEMENT_CHAR; }

    /**
     * Check whether the character being encoded is 8-bit.
     * @return the character or {@link #ASCII_REPLACEMENT_CHAR} if it is not 8-bit.
     */
    static char require8bit(char c) { return c < (1<<8) ? c : ASCII_REPLACEMENT_CHAR; }

    /** Find a codec by name that encodes the Unicode codepoint for a char directly */
    static CharCodec getUnicodeCharCodec(String name) {
        switch (name.toUpperCase()) {
        case "UTF-8": return new Utf8Codec();
        case "US-ASCII": return SimpleCharCodec.US_ASCII;
        case "ISO-8859-1": return SimpleCharCodec.ISO_LATIN_1;
        default: return null;
        }
    }
}
