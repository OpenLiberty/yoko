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

enum SimpleCodec implements CharCodec {
    UTF_16 {
        public char readChar(ReadBuffer in) { return in.readChar(); }
        public void writeChar(char c, WriteBuffer out) { out.writeChar(c); }
    },
    US_ASCII {
        public char readChar(ReadBuffer in) throws DATA_CONVERSION { return expect7bit(in.readByteAsChar()); }
        public void writeChar(char c, WriteBuffer out) throws DATA_CONVERSION { out.writeByte(require7bit(c)); }
    },
    ISO_LATIN_1 {
        public char readChar(ReadBuffer in) { return in.readByteAsChar(); } // no checking - a single-byte character can't be > 0xFF
        public void writeChar(char c, WriteBuffer out) throws DATA_CONVERSION { out.writeByte(require8bit(c)); }
    };
}
