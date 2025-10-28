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

import java.nio.charset.UnsupportedCharsetException;
import java.util.Optional;

public interface WcharCodec extends CharCodec {
    static WcharCodec getDefault() { return SimpleWcharCodec.UTF_16; }

    static WcharCodec forName(String name) {
        return Optional.of(name)
                .map(CharCodec::forName)
                .filter(WcharCodec.class::isInstance)
                .map(WcharCodec.class::cast)
                .orElseThrow(() -> new UnsupportedCharsetException(name + " not supported for wchar"));
    }

    static WcharCodec forRegistryId(int id) {
        return Optional.of(id)
                .map(CharCodec::forRegistryId)
                .filter(WcharCodec.class::isInstance)
                .map(WcharCodec.class::cast)
                .orElseThrow(() -> new UnsupportedCharsetException("Charset with registry id " + id + " not supported for wchar"));
    }

    char readWchar();

    /** Provides an identical object that can be used concurrently with this one */
    default WcharCodec getInstanceOrCopy() {
        return this;
    }
}
