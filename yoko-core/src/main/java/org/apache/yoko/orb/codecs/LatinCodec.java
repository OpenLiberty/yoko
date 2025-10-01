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
import org.apache.yoko.util.Collectors;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.HashMap;
import java.util.Map;

import static java.nio.ByteBuffer.allocate;
import static java.util.Collections.unmodifiableMap;
import static java.util.stream.IntStream.range;
import static org.apache.yoko.orb.codecs.Util.ASCII_REPLACEMENT_BYTE;
import static org.apache.yoko.orb.codecs.Util.UNICODE_REPLACEMENT_CHAR;
import static org.apache.yoko.util.Collectors.neverCombine;

/**
 * Pre-populate tables for a given latin charset so lookups use arrays and hashes.
 */
class LatinCodec implements CharCodec {
    static LatinCodec getLatinCodec(Charset charset) {
        if (!charset.canEncode()) throw new UnsupportedCharsetException(charset.name());
        switch (charset.name()) {
            case Latin2.NAME: return Latin2.INSTANCE;
            case Latin3.NAME: return Latin3.INSTANCE;
            case Latin4.NAME: return Latin4.INSTANCE;
            case Latin5.NAME: return Latin5.INSTANCE;
            case Latin7.NAME: return Latin7.INSTANCE;
            case Latin9.NAME: return Latin9.INSTANCE;
            default: throw new UnsupportedCharsetException(charset.name());
        }
    }

    // Use holder classes for the codec instances to allow SEPARATE, lazy initialization of each instance.
    // (e.g. if only Latin-2 is used, the others are never created.)
    // N.B. NAME is a compile-time constant and gets inlined so using it does not drive class initialization
    // whereas dereferencing INSTANCE forces initialization.  (See JLS 12.4)
    private interface Latin2 { String NAME = "ISO-8859-2"; LatinCodec INSTANCE = new LatinCodec(NAME); }
    private interface Latin3 { String NAME = "ISO-8859-3"; LatinCodec INSTANCE = new LatinCodec(NAME); }
    private interface Latin4 { String NAME = "ISO-8859-4"; LatinCodec INSTANCE = new LatinCodec(NAME); }
    private interface Latin5 { String NAME = "ISO-8859-5"; LatinCodec INSTANCE = new LatinCodec(NAME); }
    private interface Latin7 { String NAME = "ISO-8859-7"; LatinCodec INSTANCE = new LatinCodec(NAME); }
    private interface Latin9 { String NAME = "ISO-8859-9"; LatinCodec INSTANCE = new LatinCodec(NAME); }

    final char[] decoderArray;
    final Map<Character, Byte> encoderMap;

    private LatinCodec(String name) {
        ByteBuffer bytes = range(0, 256)
                .collect(() -> allocate(256), (bb, b) -> bb.put(b, (byte) b), neverCombine());
        CharBuffer chars = Charset.forName(name).decode(bytes);
        decoderArray = chars.array();
        encoderMap = unmodifiableMap(range(0, 256)
                .filter(i -> UNICODE_REPLACEMENT_CHAR != decoderArray[i])
                .collect(HashMap::new, (m, i) -> m.put(decoderArray[i], (byte) i), Map::putAll));
    }

    public void writeChar(char c, WriteBuffer out) {
        out.writeByte(encoderMap.getOrDefault(c, ASCII_REPLACEMENT_BYTE));
    }

    public char readChar(ReadBuffer in) {
        return decoderArray[in.readByteAsChar()];
    }
}
