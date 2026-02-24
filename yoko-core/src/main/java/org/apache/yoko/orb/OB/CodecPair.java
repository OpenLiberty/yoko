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
import org.apache.yoko.codecs.Codex;
import org.apache.yoko.codecs.WcharCodec;
import org.apache.yoko.orb.OCI.GiopVersion;

import java.util.Objects;

import static org.apache.yoko.codecs.Codex.getCharCodec;
import static org.apache.yoko.codecs.Codex.getCollocatedCharCodec;
import static org.apache.yoko.codecs.Codex.getDefaultCharCodec;
import static org.apache.yoko.codecs.Codex.getCollocatedWcharCodec;
import static org.apache.yoko.codecs.Codex.getDefaultWcharCodec;
import static org.apache.yoko.codecs.Codex.getUnspecifiedWcharCodec;
import static org.apache.yoko.codecs.Codex.getWcharCodec;

// This class may look immutable, but charCodec can contain state (while reading/writing a surrogate pair from/to UTF-8)
public final class CodecPair {
    private static final CodecPair GIOP_1_0_DEFAULT = new CodecPair(getDefaultCharCodec(), getDefaultWcharCodec());
    private static final CodecPair GIOP_1_2_DEFAULT = new CodecPair(getCharCodec("UTF-8"), getDefaultWcharCodec());

    /**
     * Codecs to use for collocated invocations
     */
    private static final CodecPair COLLOCATED = new CodecPair(getCollocatedCharCodec(), getCollocatedWcharCodec());

    static CodecPair getCollocatedCodecs() {
        return createCopy(COLLOCATED);
    }

    public final CharCodec charCodec;
    public final WcharCodec wcharCodec;

    private CodecPair(CharCodec cc, WcharCodec wc) {
        this.charCodec = cc;
        this.wcharCodec = wc;
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

    public static CodecPair createCopy(CodecPair template) {
        if (template == null) return createCopy(GIOP_1_0_DEFAULT);
        CharCodec cc = template.charCodec.duplicate();
        WcharCodec wc = template.wcharCodec.duplicate();
        // if the codec instances are IDENTICAL then the template is stateless
        boolean templateIsStateless = template.charCodec == cc && template.wcharCodec == wc;
        return templateIsStateless ? template : new CodecPair(cc, wc);
    }

    public static CodecPair create(int tcs, int twcs) {
        CharCodec cc = getCharCodec(tcs);
        WcharCodec wc = getWcharCodec(twcs);
        return new CodecPair(cc, wc);
    }

    public static CodecPair getDefaultCodecs(GiopVersion version) {
        switch (version) {
            case GIOP1_0:
                return createCopy(GIOP_1_0_DEFAULT);
            case GIOP1_1:
            case GIOP1_2:
            default:
                return createCopy(GIOP_1_2_DEFAULT);
        }
    }
}
