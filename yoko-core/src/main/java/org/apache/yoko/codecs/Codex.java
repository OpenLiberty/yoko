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

import org.apache.yoko.orb.OB.CodeSetInfo;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Optional;

import static org.apache.yoko.codecs.LatinCodec.getLatinCodec;
import static org.apache.yoko.codecs.SimpleWcharCodec.UTF_16;
import static org.apache.yoko.codecs.Util.getUnicodeCharCodec;

public enum Codex {
    ;

    public static CharCodec getCollocatedCharCodec() { return SimpleWcharCodec.COLLOCATED; }

    public static CharCodec getDefaultCharCodec() { return SimpleCharCodec.ISO_LATIN_1; }

    /**
     * Get a char codec instance for the named Java charset.
     *
     * @param charsetName the charsetName of the Java charset for which a codec is required
     * @return an instance of the appropriate char codec
     * @throws IllegalCharsetNameException if the provided charsetName is not a valid charset charsetName
     * @throws IllegalArgumentException if the provided charsetName is null
     * @throws UnsupportedCharsetException if the named charset is not supported
     */
    public static CharCodec getCharCodec(String charsetName) throws IllegalCharsetNameException, IllegalArgumentException, UnsupportedCharsetException {
        // fastest result: directly named unicode codec
        CharCodec result = getUnicodeCharCodec(charsetName);
        if (null != result) return result;
        // next see if it is an alias for a unicode codec
        Charset charset = Charset.forName(charsetName);
        result = getUnicodeCharCodec(charset.name());
        if (null != result) return result;
        // the only other codecs currently supported are the Latin ones
        return getLatinCodec(charset);
    }

    public static CharCodec getCharCodec(int id) throws UnsupportedCharsetException {
        CodeSetInfo csi = CodeSetInfo.forRegistryId(id);
        if (null == csi) throw new UnsupportedCharsetException(String.format("Unknown registry id: 0x%08x", id));
        switch (csi) {
            case UTF_8: return new Utf8Codec();
            case ISO_LATIN_1: return SimpleCharCodec.ISO_LATIN_1;
            default: return LatinCodec.getLatinCodec(csi); // throws if unknown
        }
    }

    public static WcharCodec getCollocatedWcharCodec() { return SimpleWcharCodec.COLLOCATED; }

    public static WcharCodec getDefaultWcharCodec() { return UTF_16; }

    public static WcharCodec getUnspecifiedWcharCodec() { return SimpleWcharCodec.UNSPECIFIED; }

    public static WcharCodec getWcharCodec(String charsetName) {
        if (charsetName == null) throw new NullPointerException();
        if ("UTF-16".equalsIgnoreCase(charsetName)) return UTF_16;
        if ("UTF-16".equalsIgnoreCase(Charset.forName(charsetName).name())) return UTF_16;
        throw new UnsupportedCharsetException(charsetName + " not supported for wchar");
    }

    public static WcharCodec getWcharCodec(int twcsId) {
        if (CodeSetInfo.UTF_16.id == twcsId) return UTF_16;
        String message = Optional.ofNullable(CodeSetInfo.forRegistryId(twcsId))
                .map(info -> String.format("Charset %s unsupported for wchar", info.name()))
                .orElse(String.format("Unknown registry id 0x%08x unsupported for wchar", twcsId));
        throw new UnsupportedCharsetException(message);
    }
}
