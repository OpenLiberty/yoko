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
import org.omg.CORBA.DATA_CONVERSION;

import static org.apache.yoko.util.MinorCodes.MinorNoCharacterMapping;
import static org.omg.CORBA.CompletionStatus.COMPLETED_MAYBE;

interface CharCodec {
    /**
     * If any character cannot be read by a codec, the codec will return this character instead.
     * Where something has gone wrong with a multi-byte encoding sequence in UTF8,
     * multiple instances of this char may be returned.
     */
    char REPLACEMENT_CHAR = '\uFFFD';

    /**
     * Check whether the character is US-ASCII.
     * @return the character, or REPLACEMENT_CHAR if it is not US-ASCII
     */
    default char expect7bit(char c) {
        return c > '\u007F' ? REPLACEMENT_CHAR : c;
    }
    /**
     * Check whether the character is ISO-LATIN-1.
     * @return the character, or REPLACEMENT_CHAR if it is not ISO-LATIN-1
     */
    default char expect8bit(char c) { return c > '\u00FF' ? REPLACEMENT_CHAR : c; }
    default char require7bit(char c) throws DATA_CONVERSION {
        if (c > '\u007F') throw new DATA_CONVERSION(String.format("Encountered character outside valid range: 0x%04X", (int)c), MinorNoCharacterMapping, COMPLETED_MAYBE);
        return c;
    }
    default char require8bit(char c) throws DATA_CONVERSION {
        if (c > '\u00FF') throw new DATA_CONVERSION(String.format("Encountered character outside valid range: 0x%04X", (int)c), MinorNoCharacterMapping, COMPLETED_MAYBE);
        return c;
    }

    /**
     * Encodes a character to a buffer.
     * <p>
     *     If the character is a {@link Character#highSurrogate(int)},
     *     it may not be written until the next character is passed in.
     *     The complete encoding will then be written out.
     * </p>
     * <p>
     *     It is an error to pass in different {@link WriteBuffer}s
     *     when writing out the two characters of a surrogate pair.
     *     The behaviour is undefined and may differ between different implementations.
     * </p>
     *
     * @param c the character to write out
     * @param out the buffer to which the character should be written
     * @throws DATA_CONVERSION if the character is not supported by this encoder's encoding.
     */
    void writeChar(char c, WriteBuffer out) throws DATA_CONVERSION;

    /** Reads in the next char */
    char readChar(ReadBuffer in) throws DATA_CONVERSION;

    /**
     * Check there is no unfinished character data.
     * This is only relevant for encodings that encode
     * characters above the Basic Multilingual Plane
     * and do not encode them as surrogate pairs.
     */
    default void assertNoBufferedCharData() {}
}
