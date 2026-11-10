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
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.openliberty.yoko.rewrite;

import java.util.Comparator;

/**
 * The Yoko import layout (this is also the IntelliJ default layout):
 * <pre>
 * all other imports, sorted
 *
 * javax.* then java.*, each sorted
 *
 * all static imports, sorted
 * </pre>
 * Sorting is plain {@link String#compareTo} order on the imported name, so upper case sorts before lower case.
 */
final class ImportLayout {
    private ImportLayout() {}

    /** A single import to be laid out. */
    static final class Entry {
        final String name;
        final boolean isStatic;

        Entry(String name, boolean isStatic) {
            this.name = name;
            this.isStatic = isStatic;
        }
    }

    /** The blank-line-separated block an import belongs in. */
    static int block(Entry e) {
        if (e.isStatic) return 2;
        return subgroup(e) == 0 ? 0 : 1;
    }

    /** Orders imports within a block (javax before java, as per the IntelliJ default). */
    private static int subgroup(Entry e) {
        if (e.isStatic) return 3;
        if (e.name.startsWith("javax.")) return 1;
        if (e.name.startsWith("java.")) return 2;
        return 0;
    }

    static final Comparator<Entry> ORDER = Comparator
            .<Entry>comparingInt(ImportLayout::subgroup)
            .thenComparing(e -> e.name);
}
