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
import org.apache.yoko.orb.OB.CodeSetInfo;
import org.omg.CORBA.DATA_CONVERSION;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;

import static org.apache.yoko.orb.codecs.LatinCodec.getLatinCodec;
import static org.apache.yoko.orb.codecs.Util.getUnicodeCodec;
import static org.apache.yoko.util.MinorCodes.MinorUTF8Encoding;
import static org.apache.yoko.util.MinorCodes.MinorUTF8Overflow;
import static org.omg.CORBA.CompletionStatus.COMPLETED_MAYBE;

/**
 * Java's native character support uses UTF-16.
 * This interface supports encoding from and to a specific wire format.
 * The general contract is to encode everything possible, and to replace characters that cannot be encoded/decoded
 * with the Unicode replacement character, '\uFFFD', wherever possible.
 * This Unicode character is not supported by any of the single-byte encodings, such as US-ASCII or ISO-8859-7.
 * When encoding to such an encoding, a replacement character of '?' (question mark) will be used instead.
 * The reason for replacement rather than throwing an exception is that the text may not be critical and
 * forcing a failure would be unnecessarily restrictive.
 * <p>
 *     There are some error conditions to do with variable multibyte encodings, such as UTF-8.
 *     When decoding these, there are certain illegal sequences of bytes that cannot be interpreted as UTF-8.
 *     These will be treated as undecodable characters and replaced as detailed above.
 *     It may not be possible to determine how many characters were intended,
 *     and the number of replacement characters for a corrupted encoding may vary.
 * </p>
 * <p>
 *     Certain Unicode codepoints are represented in UTF-16 by pairs of "surrogate" characters.
 *     Where these can be encoded directly, e.g. when encoding to UTF-16, they will be encoded verbatim
 *     even when the sequence does not make sense. There are operating systems that allow filenames ending
 *     in an unmatched surrogate character, and it may be necessary to transmit these "illegal" strings.
 * </p>
 * <p>
 *     When encoding surrogates to UTF-8, however, there is no permitted encoding that directly matches a
 *     single surrogate character. The correct approach is to consider the pair together,
 *     determine the intended codepoint, and encode that as a 4-byte UTF-8 encoded character.
 *     If a high surrogate is written, it will be buffered until the matching low surrogate arrives.
 *     If the {@link #assertNoBufferedCharData()} method is invoked when an unmatched surrogate has been written,
 *     this method will throw a DATA_CONVERSION exception, indicating that the characters provided could not be encoded.
 * </p>
 * <p>
 *     When decoding surrogates from UTF-8, two characters may be decoded at once. Since this interface is
 *     character-oriented, only a single character will be returned. The next call the {@link #readChar(ReadBuffer)}
 *     will return the buffered second surrogate. The cursor in the {@link ReadBuffer} will not be moved to the end of
 *     the 4-byte sequence until the second character is consumed.
 *     If the {@link #assertNoBufferedCharData()} method is invoked when a low surrogate is waiting to be read,
 *     this method will throw a DATA_CONVERSION exception, indicating that characters were not read completely.
 * </p>
 * <p>
 *     UTF-8's built-in error detectability also allows for "overlong" encodings. This is where a multibyte sequence is
 *     used to encode a codepoint that belonged in a shorter encoding. This represents a security risk, since it can
 *     potentially be used to sneak characters past validation (consider injection attacks) or to spoof another string
 *     with a different string that displays the same (consider URL spoofing). The desired behaviour is to decode these
 *     prohibited overlong encodings as the Unicode replacement character, '\uFFFD'.
 * </p>
 */
public interface CharCodec {
    @FunctionalInterface interface CharReader { char readChar(ReadBuffer in); }

    /**
     * Get a char codec instance for the named Java charset.
     *
     * @param name the name of the Java charset for which a codec is required
     * @return an instance of the appropriate char codec
     * @throws IllegalCharsetNameException if the provided name is not a valid charset name
     * @throws IllegalArgumentException if the provided name is null
     * @throws UnsupportedCharsetException if the named charset is not supported
     */
    static CharCodec forName(String name) throws IllegalCharsetNameException, IllegalArgumentException, UnsupportedCharsetException {
        // fastest result: directly named unicode codec
        CharCodec result = getUnicodeCodec(name);
        if (null != result) return result;
        // next see if it is an alias for a unicode codec
        Charset charset = Charset.forName(name);
        result = getUnicodeCodec(charset.name());
        if (null != result) return result;
        // the only other codecs currently supported are the Latin ones
        return getLatinCodec(charset);
    }

    static CharCodec forRegistryId(int id) throws UnsupportedCharsetException {
        CodeSetInfo csi = CodeSetInfo.forRegistryId(id);
        switch (csi) {
            case UTF_16: return SimpleWcharCodec.UTF_16;
            case UTF_8: return new Utf8Codec();

        }
        throw new UnsupportedCharsetException("Charset registry id = " + id);
    }

    String name();

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
     */
    void writeChar(char c, WriteBuffer out);

    /**
     * For non-byte-oriented codecs, there may be a byte-order marker to be written at the start of a string or character.
     * A caller should use this method to write subsequent chars to avoid writing extra BOMs.
     */
    default void writeNextChar(char c, WriteBuffer out) { writeChar(c, out); }

    /** Read the next char */
    char readChar(ReadBuffer in);

    /**
     * For non-byte-oriented codecs, there may be a byte-order marker to indicate the endianness of the encoded bytes.
     * This BOM can only occur at the start of a string, or before an individual character.
     * For a string, a caller should call this method to determine the ordering.
     *
     * On byte-oriented codecs, this method will return a byte-oriented (endian-free) reader.
     *
     * @return the endian-informed reader with which to read in the rest of the string.
     */
    default CharReader beginString(ReadBuffer in) { return this::readChar; }

    /**
     * Check there is no unfinished character data.
     * This is only relevant for encodings that encode
     * characters above the Basic Multilingual Plane
     * and do not encode them as surrogate pairs.
     *
     * @throws DATA_CONVERSION if there is unfinished data to be read or written
     */
    default void assertNoBufferedCharData() throws DATA_CONVERSION {
        if (!readFinished()) throw new DATA_CONVERSION("Low surrogate left unread", MinorUTF8Overflow, COMPLETED_MAYBE);
        if (!writeFinished()) throw new DATA_CONVERSION("High surrogate as last character", MinorUTF8Encoding, COMPLETED_MAYBE);
    }

    /** Check whether there is no low surrogate waiting to be read. */
    default boolean readFinished() { return true; }
    /** Check whether the last character was not a high surrogate. */
    default boolean writeFinished() { return true; }

    /** Provides an identical object that can be used concurrently with this one */
    default CharCodec getInstanceOrCopy() { return this; }
}
