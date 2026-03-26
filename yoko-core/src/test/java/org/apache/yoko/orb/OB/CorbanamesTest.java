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

import org.junit.jupiter.api.Test;
import org.omg.CosNaming.NameComponent;

import java.net.URI;
import java.net.URISyntaxException;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static org.apache.yoko.orb.OB.Corbanames.extractNameComponents;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class CorbanamesTest {

    @Test void emptyString() throws Exception {
        NameComponent[] components = getComponents("");
        assertNull(components);
    }

    @Test void simpleString() throws Exception { assertComponents("x", "[x][]"); }
    @Test void stringWithKind() throws Exception { assertComponents("x.y", "[x][y]"); }
    @Test void twoComponentsNoKind() throws Exception { assertComponents("x/y", "[x][]/[y][]"); }
    @Test void twoComponentsWithKinds() throws Exception {assertComponents("a.b/c.d", "[a][b]/[c][d]"); }
    @Test void escapedDotInId() throws Exception { assertComponents("x\\.y", "[x.y][]"); }
    @Test void escapedSlashInId() throws Exception { assertComponents("x\\/y", "[x/y][]"); }
    @Test void escapedBackslashInId() throws Exception { assertComponents("x\\\\y", "[x\\y][]"); }
    @Test void multipleDotsAllEscaped() throws Exception { assertComponents("a.b.c", "[a.b.c][]"); }
    @Test void emptyIdAndKind() throws Exception { assertComponents(".", "[][]"); }
    @Test void complexMixedPath() throws Exception { assertComponents("foo.kind1/bar/baz.kind2", "[foo][kind1]/[bar][]/[baz][kind2]"); }
    @Test void escapedDotInKind() throws Exception { assertComponents("id.kind\\.more", "[id][kind.more]"); }
    @Test void allEscapedCharacters() throws Exception { assertComponents("a\\.b\\/c\\\\d", "[a.b/c\\d][]"); }
    @Test void emptyComponentsInPath() throws Exception { assertComponents("//", "[][]/[][]/[][]"); }
    @Test void kindOnly() throws Exception { assertComponents(".kind", "[][kind]"); }

    private static NameComponent[] getComponents(String input) throws URISyntaxException {
        URI uri = new URI("corbaname", "localhost:2809/NameService", input);
        return extractNameComponents(uri);
    }

    private static void assertComponents(String input, String expected) throws URISyntaxException {
        NameComponent[] components = getComponents(input);
        assertNotNull(components);
        assertEquals(expected, stream(components).map(nc -> String.format("[%s][%s]", nc.id, nc.kind)).collect(joining("/")));
    }


}