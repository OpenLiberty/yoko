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
package org.apache.yoko.orb.OB;

import org.omg.CONV_FRAME.CodeSetComponent;
import org.omg.CORBA.CODESET_INCOMPATIBLE;

import java.util.stream.IntStream;

import static org.apache.yoko.orb.OB.CodeSetInfo.areCompatibleCodesets;

enum CodeSetDatabase {
    ;

    static CodeConverterBase getConverter(int from, int to) {
        CodeSetInfo fromSet = CodeSetInfo.forRegistryId(from);
        CodeSetInfo toSet = CodeSetInfo.forRegistryId(to);
        return getConverter(fromSet, toSet);
    }

    static CodeConverterBase getConverter(CodeSetInfo fromSet, CodeSetInfo toSet) {
        // Optimization: don't use converter for identical narrow codesets
        if (toSet != null && toSet == fromSet && toSet.max_bytes == 1) return null;

        if (fromSet == null || toSet == null) return new CodeConverterNone(fromSet, toSet);

        // the unsupported codesets should have been filtered out by the initial handshake
        return new CodeConverterImpl(fromSet, toSet);
    }

    /**
     * The logic of this method is taken from "Code Set Negotiation" (see CORBA 3.0.3 section 13.10.2.6).
     * Given what we know about the client (i.e. our) codeset, it could be simplified further,
     * but then it would not be as observably correct.
     */
    static int determineTCS(CodeSetComponent clientCS, CodeSetComponent serverCS, int fallback) {
        // Check if the server declares a native codeset
        if (serverCS.native_code_set != 0 ) {
            // If the server and client native codeset match, use that
            if (clientCS.native_code_set == serverCS.native_code_set) return serverCS.native_code_set;

            // If the client can convert to the server native codeset, use the server native codeset
            if (checkCodeSetId(clientCS, serverCS.native_code_set)) return serverCS.native_code_set;
        }
        if (clientCS.native_code_set != 0) {
            // If the server can convert to the client native codeset, use the client native codeset
            if (checkCodeSetId(serverCS, clientCS.native_code_set)) return clientCS.native_code_set;
        }

        // We've ruled out the native code sets.
        // Try to find a common conversion codeset,
        // using the server's stated order of preference.
        for (int conversionCodeSet : serverCS.conversion_code_sets) {
            if (checkCodeSetId(clientCS, conversionCodeSet)) return conversionCodeSet;
        }

        // No common codesets exist.
        // If the client and server have native codesets and they are compatible, use the fallback encoding.
        // (The fallback encoding is specified as UTF-8 for char and UTF-16 for wchar.)
        if (clientCS.native_code_set != 0 && serverCS.native_code_set != 0) {
            if (areCompatibleCodesets(clientCS.native_code_set, serverCS.native_code_set)) return fallback;
        }

        // Codeset negotiation has failed :(
        throw new CODESET_INCOMPATIBLE();
    }

    private static boolean checkCodeSetId(CodeSetComponent csc, int id) {
        return IntStream.of(csc.conversion_code_sets).anyMatch(cid -> id == cid);
    }
}
