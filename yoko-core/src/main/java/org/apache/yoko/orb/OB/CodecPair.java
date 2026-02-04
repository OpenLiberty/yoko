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
package org.apache.yoko.orb.OB;

import org.apache.yoko.codecs.CharCodec;
import org.apache.yoko.codecs.WcharCodec;

import java.util.Objects;

import static org.apache.yoko.codecs.CharCodec.DEFAULT_CHAR_CODEC;
import static org.apache.yoko.codecs.CharCodec.NULL_CHAR_CODEC;
import static org.apache.yoko.codecs.WcharCodec.NULL_WCHAR_CODEC;

// This class may look immutable, but charCodec can contain state (while reading/writing a surrogate pair from/to UTF-8)
public final class CodecPair {
    /**
     * The default codecs for GIOP 1.0.
     * TODO: consider whether we need an UNSUPPORTED wchar codec that throws exceptions for wchars
     */
    static final CodecPair DEFAULT = new CodecPair(DEFAULT_CHAR_CODEC, WcharCodec.DEFAULT_WCHAR_CODEC);
    static final CodecPair COLLOCATED = new CodecPair(NULL_CHAR_CODEC, NULL_WCHAR_CODEC);

    public final CharCodec charCodec;
    public final WcharCodec wcharCodec;

    private CodecPair(CharCodec cc, WcharCodec wc) {
        this.charCodec = cc;
        this.wcharCodec = wc;
    }

    private CodecPair(CodecPair that) {
        this(that.charCodec.getInstanceOrCopy(), that.wcharCodec.getInstanceOrCopy());
    }

    public static CodecPair createCopy(CodecPair template) {
        if (template == null) return DEFAULT;
        if (template == DEFAULT) return DEFAULT;
        if (template == COLLOCATED) return COLLOCATED;
        return new CodecPair(template);
    }

    public static CodecPair create(int tcs, int twcs) {
        CharCodec cc = CharCodec.forRegistryId(tcs);
        WcharCodec wc = WcharCodec.forRegistryId(twcs);
        return new CodecPair(cc, wc);
    }

    public static CodecPair createForWcharWriteOnly() {
        return new CodecPair(null, WcharCodec.getDefault());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CodecPair)) return false;
        CodecPair that = (CodecPair) o;
        return Objects.equals(charCodec, that.charCodec) && Objects.equals(wcharCodec, that.wcharCodec);
    }

    @Override
    public int hashCode() {
        return Objects.hash(charCodec, wcharCodec);
    }

    public String toString() {
        return String.format("CodeConverters{%ncharCodec=%s%nwcharCodec=%s%n}", charCodec, wcharCodec);
    }
}
