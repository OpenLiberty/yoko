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

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static java.nio.ByteBuffer.allocate;
import static java.util.Collections.unmodifiableMap;
import static java.util.stream.IntStream.range;
import static org.apache.yoko.codecs.Util.ASCII_REPLACEMENT_BYTE;
import static org.apache.yoko.codecs.Util.UNICODE_REPLACEMENT_CHAR;
import static org.apache.yoko.orb.OB.CodeSetInfo.ISO_8859_5;
import static org.apache.yoko.orb.OB.CodeSetInfo.ISO_8859_6;
import static org.apache.yoko.orb.OB.CodeSetInfo.ISO_8859_7;
import static org.apache.yoko.orb.OB.CodeSetInfo.ISO_8859_8;
import static org.apache.yoko.orb.OB.CodeSetInfo.ISO_8859_9;
import static org.apache.yoko.orb.OB.CodeSetInfo.ISO_LATIN_2;
import static org.apache.yoko.orb.OB.CodeSetInfo.ISO_LATIN_3;
import static org.apache.yoko.orb.OB.CodeSetInfo.ISO_LATIN_4;
import static org.apache.yoko.util.Collectors.neverCombine;

/**
 * Pre-populate tables for a given latin charset so lookups use arrays and hashes.
 */
class LatinCodec implements CharCodec {
    static LatinCodec getLatinCodec(Charset charset) {
        if (!charset.canEncode()) throw new UnsupportedCharsetException(charset.name());
        switch (charset.name()) {
            case "ISO-8859-2": return Iso8859_2.INSTANCE;
            case "ISO-8859-3": return Iso8859_3.INSTANCE;
            case "ISO-8859-4": return Iso8859_4.INSTANCE;
            case "ISO-8859-5": return Iso8859_5.INSTANCE;
            case "ISO-8859-6": return Iso8859_6.INSTANCE;
            case "ISO-8859-7": return Iso8859_7.INSTANCE;
            case "ISO-8859-8": return Iso8859_8.INSTANCE;
            case "ISO-8859-9": return Iso8859_9.INSTANCE;
            default: throw new UnsupportedCharsetException(charset.name());
        }
    }

    static LatinCodec getLatinCodec(CodeSetInfo csi) {
        switch (csi) {
            case ISO_LATIN_2: return Iso8859_2.INSTANCE;
            case ISO_LATIN_3: return Iso8859_3.INSTANCE;
            case ISO_LATIN_4: return Iso8859_4.INSTANCE;
            case ISO_8859_5: return Iso8859_5.INSTANCE;
            case ISO_8859_6: return Iso8859_6.INSTANCE;
            case ISO_8859_7: return Iso8859_7.INSTANCE;
            case ISO_8859_8: return Iso8859_8.INSTANCE;
            case ISO_8859_9: return Iso8859_9.INSTANCE;
        }
        throw new UnsupportedCharsetException(csi.name());
    }

    // Use holder classes for the codec instances to allow SEPARATE, lazy initialization of each instance.
    // (e.g. if only Latin-2 is used, the others are never created.)
    // N.B. NAME is a compile-time constant and gets inlined so using it does not drive class initialization
    // whereas dereferencing INSTANCE forces initialization.  (See JLS 12.4)
    private interface Iso8859_2 { LatinCodec INSTANCE = new LatinCodec("ISO-8859-2", ISO_LATIN_2); }
    private interface Iso8859_3 { LatinCodec INSTANCE = new LatinCodec("ISO-8859-3", ISO_LATIN_3); }
    private interface Iso8859_4 { LatinCodec INSTANCE = new LatinCodec("ISO-8859-4", ISO_LATIN_4); }
    private interface Iso8859_5 { LatinCodec INSTANCE = new LatinCodec("ISO-8859-5", ISO_8859_5); }
    private interface Iso8859_6 { LatinCodec INSTANCE = new LatinCodec("ISO-8859-6", ISO_8859_6); }
    private interface Iso8859_7 { LatinCodec INSTANCE = new LatinCodec("ISO-8859-7", ISO_8859_7); }
    private interface Iso8859_8 { LatinCodec INSTANCE = new LatinCodec("ISO-8859-8", ISO_8859_8); }
    private interface Iso8859_9 { LatinCodec INSTANCE = new LatinCodec("ISO-8859-9", ISO_8859_9); }

    private final String name;
    private final CodeSetInfo codeSetInfo;
    private final char[] decoderArray;
    private final Map<Character, Byte> encoderMap;

    private LatinCodec(String name, CodeSetInfo csi) {
        Charset cs  = Charset.forName(name);
        this.name = cs.name();
        this.codeSetInfo = csi;
        ByteBuffer bytes = range(0, 256)
                .collect(() -> allocate(256), (bb, b) -> bb.put(b, (byte) b), neverCombine());
        CharBuffer chars = cs.decode(bytes);
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

    @Override
    public String name() { return name; }

    @Override
    public CodeSetInfo getCodeSetInfo() { return codeSetInfo; }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof LatinCodec)) return false;
        LatinCodec that = (LatinCodec) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }

    @Override
    public String toString() { return name; }
}
