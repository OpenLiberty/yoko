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

import static org.apache.yoko.codecs.CharCodec.NULL_CODEC;

// This class may look immutable, but charCodec can contain state (while reading a surrogate pair from UTF-8)
public final class CodeConverters {
    static final CodeConverters COLLOCATED = new CodeConverters(NULL_CODEC, NULL_CODEC);

    public final CharCodec charCodec;
    public final WcharCodec wcharCodec;

    private CodeConverters(CharCodec cc, WcharCodec wc) {
        this.charCodec = cc;
        this.wcharCodec = wc;
    }

    private CodeConverters(CodeConverters that) {
        this(that.charCodec.getInstanceOrCopy(), that.wcharCodec.getInstanceOrCopy());
    }

    public static CodeConverters createCopy(CodeConverters template) {
        if (template == null) return COLLOCATED;
        if (template == COLLOCATED) return COLLOCATED;
        return new CodeConverters(template);
    }

    public static CodeConverters create(int tcs, int twcs) {
        CharCodec cc = CharCodec.forRegistryId(tcs);
        WcharCodec wc = WcharCodec.forRegistryId(twcs);
        return new CodeConverters(cc, wc);
    }

    public static CodeConverters createForWcharWriteOnly() {
        return new CodeConverters(null, WcharCodec.getDefault());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CodeConverters)) return false;
        CodeConverters that = (CodeConverters) o;
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
