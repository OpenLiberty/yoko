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

import org.omg.CORBA.BAD_PARAM;
import org.omg.CosNaming.NameComponent;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.joining;
import static org.apache.yoko.logging.VerboseLogging.NAMING_LOG;
import static org.apache.yoko.util.MinorCodes.MinorBadSchemeSpecificPart;
import static org.apache.yoko.util.MinorCodes.describeBadParam;
import static org.omg.CORBA.CompletionStatus.COMPLETED_NO;

// This class parses the components of a corbaname URI fragment. Pass
// as string or octet sequence to the constructor. The isValid()
// method determines if the path is a valid stringified name. The
// getContents() method returns a string sequence of {id, kind} pairs.
//
enum Corbanames {
    ;
    /**
     * splits a string on every unescaped slash character and a single unescaped dot in each subsequent part.
     * e.g. id.kind/id.kind/id.kind
     */
    static NameComponent[] extractNameComponents(URI uri) {
        final String path = uri.getFragment();
        if (path == null || path.isEmpty()) return null;
        List<NameComponent> components = new ArrayList<>();
        final StringBuilder sb = new StringBuilder();
        boolean escaping = false;
        int dotIndex = -1;
        int dotCount = 0;
        for (int i = 0; i < path.length(); i++) {
            final char c = path.charAt(i);
            if (escaping) {
                // check escaped character is a valid escape character
                if (c != '.' && c != '/' && c != '\\') {
                    String msg = ": corbaname path contains illegal escape sequence starting at index " + (i - 1) + ": " + path;
                    throw new BAD_PARAM(describeBadParam(MinorBadSchemeSpecificPart) + msg, MinorBadSchemeSpecificPart, COMPLETED_NO);
                }
                escaping = false;
            } else switch (c) {
                case '\\':
                    escaping = true;
                    continue; // don't copy the escape character

                case '/': // reached the end of a component
                    components.add(createNameComponent(sb, dotCount, dotIndex));
                    // reset for next component
                    sb.setLength(0);
                    dotIndex = -1;
                    dotCount = 0;
                    continue; // don't copy the path separator character

                case '.':
                    dotCount++;
                    dotIndex = sb.length();
                    // copy the dot,
                    // but note: we might split on it (and remove it) later
            }
            sb.append(c);
        }
        if (escaping) {
            String msg = "corbaname path ends in unterminated escape character: " + path;
            throw new BAD_PARAM(describeBadParam(MinorBadSchemeSpecificPart) + msg, MinorBadSchemeSpecificPart, COMPLETED_NO);
        }

        components.add(createNameComponent(sb, dotCount, dotIndex));
        NAMING_LOG.finer(() -> "parsed path: " + components.stream().map(Objects::toString).collect(joining("/")));
        return components.stream().toArray(NameComponent[]::new);
    }

    private static NameComponent createNameComponent(StringBuilder sb, final int dotCount, final int dotIndex) {
        // optionally split into id and kind
        if (dotCount > 1) NAMING_LOG.finest(() -> "treating illegal name part as if all dots were escaped: " + sb);
        final NameComponent result = 1 == dotCount ?
                new NameComponent(sb.substring(0, dotIndex), sb.substring(dotIndex + 1)) :
                new NameComponent(sb.toString(), "");
        NAMING_LOG.finest(() -> "name component = " + result);
        return result;
    }
}
