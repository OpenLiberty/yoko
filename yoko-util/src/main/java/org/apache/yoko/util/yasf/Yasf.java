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
package org.apache.yoko.util.yasf;

import java.util.BitSet;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.Arrays.copyOf;
import static org.apache.yoko.util.Collectors.toBitSet;
import static org.apache.yoko.util.Collectors.toUnmodifiableEnumSet;

/**
 * <h2>YASF &mdash; Yoko Auxiliary Stream Format</h2>
 * This class encapsulates all the Yoko fixes that affect the stream format.
 * In order to ensure compatibility with older versions of Yoko,
 * the stream format fix level is communicated between ORBs using two media:
 * <ul>
 *     <li>a component tag in an IOR profile</li>
 *     <li>a service context in a GIOP packet</li>
 * </ul>
 */
public enum Yasf {
    ENUM_FIXED(0),
    NON_SERIALIZABLE_FIELD_IS_ABSTRACT_VALUE(1),
    ;
    // TODO - Get the OMG to assign this value to Yoko
    public static final int TAG_YOKO_AUXILIARY_STREAM_FORMAT = 0xeeeeeeee;
    public static final int YOKO_AUXILIARY_STREAM_FORMAT_SC = 0xeeeeeeee;
    /** Pre-computed octet representation of all supported fixes in this level */
    private static final byte[] BYTES = Stream.of(Yasf.values())
            .collect(toBitSet(y -> y.itemIndex))
            .toByteArray();

    /**
     * The index of this option in any bitmap representation.
     * Must be unique to this Yasf enum member.
     * Should be sequential.
     */
    public final int itemIndex;

    Yasf(int itemIndex) { this.itemIndex = itemIndex; }

    public boolean isSupported() {
        Set<Yasf> set = YasfThreadLocal.get();
        // When there is no thread local set
        // (i.e. when talking to non-Yoko ORBs),
        // assume the format is ON.
        //
        // Note: this assumes each option is spec-compliant and desirable.
        // If this assumption changes, we might need to track 'onByDefault' on a per-member basis.
        return set == null || set.contains(this);
    }

    public boolean isUnsupported() {
        return !isSupported();
    }

    /**
     * Convert the supplied bits into a set of supported Yasf options,
     * ignoring any unknown to this ORB (obviously).
     * @param data  the bitmap of which Yasf options are supported (by the other ORB)
     * @return the subset of known Yasf options that are supported
     */
    public static Set<Yasf> toSet(byte[] data) {
        if (data == null) return null;
        BitSet items = BitSet.valueOf(data);
        return Stream.of(Yasf.values())
                .filter(y -> items.get(y.itemIndex))
                .collect(toUnmodifiableEnumSet(Yasf.class));
    }

    /**
     * Get the bitmap of Yasf options supported in this ORB.
     * @return the bitmap as a byte array
     */
    public static byte[] toData() { return copyOf(BYTES, BYTES.length); }
}
