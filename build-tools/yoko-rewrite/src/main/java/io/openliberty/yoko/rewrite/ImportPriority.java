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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Decides which of several classes sharing a simple name gets to be imported.
 * The others must then be written out fully qualified.
 * <p>
 * The rules, in order of precedence:
 * <ol>
 * <li>{@code java.*} and {@code javax.*} beat {@code org.omg.*},
 *     which beats anything that is not {@code org.apache.yoko.*},
 *     which beats {@code org.apache.yoko.*}.</li>
 * <li>Within a tier, if one class is a supertype of the other, the supertype wins ("higher up the hierarchy").</li>
 * <li>Within a tier, if one class's package is an ancestor of the other's package, the shallower one wins.</li>
 * <li>Otherwise the class that is already imported wins, to avoid needless changes.</li>
 * <li>Otherwise the lexically first fully qualified name wins, so that the outcome is deterministic.</li>
 * </ol>
 * Additionally, {@code org.omg.CORBA.Object} is never imported, because it hides {@code java.lang.Object}.
 */
final class ImportPriority {
    private ImportPriority() {}

    /** A class that is competing for its simple name. */
    static final class Candidate {
        /** Fully qualified name using '.' for nested classes. */
        final String fqn;
        /** All transitive supertypes, as fully qualified names using '.' for nested classes. */
        final Set<String> supertypes;
        /** True if the file already imports this class, which breaks ties so as to cause the least churn. */
        final boolean imported;

        Candidate(String fqn, Collection<String> supertypes, boolean imported) {
            this.fqn = fqn;
            this.imported = imported;
            this.supertypes = Collections.unmodifiableSet(new LinkedHashSet<>(supertypes));
        }

        @Override public String toString() { return fqn; }
    }

    /** Classes that are never imported, no matter what, because the name is too confusing. */
    static boolean isNeverImported(String fqn) {
        return fqn.equals("org.omg.CORBA.Object");
    }

    /** Lower numbers are preferred. */
    static int tier(String fqn) {
        if (isNeverImported(fqn)) return 4;
        if (fqn.startsWith("java.") || fqn.startsWith("javax.")) return 0;
        if (fqn.startsWith("org.omg.")) return 1;
        if (fqn.startsWith("org.apache.yoko.")) return 3;
        return 2;
    }

    /** Negative if {@code a} should be imported in preference to {@code b}. */
    static int compare(Candidate a, Candidate b) {
        int result = Integer.compare(tier(a.fqn), tier(b.fqn));
        if (result != 0) return result;
        if (a.supertypes.contains(b.fqn)) return 1; // b is higher in the hierarchy
        if (b.supertypes.contains(a.fqn)) return -1; // a is higher in the hierarchy
        String pa = packageOf(a.fqn);
        String pb = packageOf(b.fqn);
        if (pb.startsWith(pa + ".")) return -1; // a is in an ancestor package
        if (pa.startsWith(pb + ".")) return 1;
        if (a.imported != b.imported) return a.imported ? -1 : 1;
        return a.fqn.compareTo(b.fqn);
    }

    /** Pick the candidate that should be imported. */
    static Candidate best(Collection<Candidate> candidates) {
        Candidate best = null;
        for (Candidate c : candidates) if (best == null || compare(c, best) < 0) best = c;
        if (best == null) throw new IllegalArgumentException("no candidates");
        return best;
    }

    static String simpleName(String fqn) {
        return fqn.substring(fqn.lastIndexOf('.') + 1);
    }

    static String packageOf(String fqn) {
        int i = fqn.lastIndexOf('.');
        return i < 0 ? "" : fqn.substring(0, i);
    }
}
