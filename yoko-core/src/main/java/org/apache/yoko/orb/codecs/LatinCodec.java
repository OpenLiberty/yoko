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

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.unmodifiableMap;
import static java.util.stream.IntStream.range;
import static org.apache.yoko.orb.codecs.Util.ASCII_REPLACEMENT_BYTE;
import static org.apache.yoko.orb.codecs.Util.UNICODE_REPLACEMENT_CHAR;

/**
 * Pre-populate tables for a given latin charset so lookups are array indexing or hashmap lookup.
 */
class LatinCodec implements CharCodec {
    private static final Map<Charset, LatinCodec> CACHE = new HashMap<>();

    static LatinCodec getLatinCodec(Charset charset) {
        if (!charset.canEncode()) throw new UnsupportedCharsetException(charset.name());
        if (!charset.name().toUpperCase().startsWith("ISO-8859-")) throw new UnsupportedCharsetException(charset.name());
        return CACHE.computeIfAbsent(charset, LatinCodec::new);
    }

    final char[] decoderTable;
    final Map<Character, Byte> encoderMap;

    private LatinCodec(Charset charset) {
        ByteBuffer bytes = range(0, 256)
                .collect(() -> ByteBuffer.allocate(256), (bb, b) -> bb.put(b, (byte) b), (bb1, bb2) -> {
                    throw null;
                })
                .asReadOnlyBuffer();
        CharBuffer chars = charset.decode(bytes);
        decoderTable = chars.array();
        encoderMap = unmodifiableMap(range(0, 256)
                .filter(i -> decoderTable[i] != UNICODE_REPLACEMENT_CHAR)
                .collect(HashMap::new, (m, i) -> m.put(decoderTable[i], (byte) i), Map::putAll));
    }

    public void writeChar(char c, WriteBuffer out) {
        out.writeByte(encoderMap.getOrDefault(c, ASCII_REPLACEMENT_BYTE));
    }

    public char readChar(ReadBuffer in) {
        return decoderTable[in.readByteAsChar()];
    }
}
