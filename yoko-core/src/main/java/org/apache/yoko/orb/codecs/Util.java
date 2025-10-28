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

enum Util {
    ;
    /**
     * If any character cannot be read by a codec, the codec will return this character instead.
     * Where something has gone wrong with a multi-byte encoding sequence in UTF8,
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
     * Check whether the character is US-ASCII.
     * @return the character, or REPLACEMENT_CHAR if it is not US-ASCII
     */
    static char expect7bit(char c) { return c <= '\u007F' ? c : Util.UNICODE_REPLACEMENT_CHAR; }

    /** If the character fits in 7 bits, return it, otherwise return '?' */
    static char require8bit(char c) { return c <= '\u00FF' ? c : ASCII_REPLACEMENT_CHAR; }

    /** If the character fits in 8 bits, return it, otherwise return '?' */
    static char require7bit(char c) { return c <= '\u007F' ? c : ASCII_REPLACEMENT_CHAR; }

    /** Find a codec by name that encodes the unicode codepoint for a char directly */
    static CharCodec getUnicodeCodec(String name) {
        switch (name.toUpperCase()) {
        case "UTF-8": return new Utf8Codec();
        case "UTF-16": return SimpleWcharCodec.UTF_16;
        case "US-ASCII": return SimpleCodec.US_ASCII;
        case "ISO-8859-1": return SimpleCodec.ISO_LATIN_1;
        default: return null;
        }
    }
}
