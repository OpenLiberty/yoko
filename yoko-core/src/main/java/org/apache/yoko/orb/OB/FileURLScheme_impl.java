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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URI;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.apache.yoko.util.MinorCodes.MinorBadSchemeSpecificPart;
import static org.apache.yoko.util.MinorCodes.describeBadParam;
import static org.omg.CORBA.CompletionStatus.COMPLETED_NO;

public class FileURLScheme_impl implements URLScheme {
    private final boolean relative_;
    private final URLRegistry registry_;

    public FileURLScheme_impl(boolean relative, URLRegistry registry) {
        relative_ = relative;
        registry_ = registry;
    }

    public String name() { return (relative_ ? "relfile" : "file"); }

    public org.omg.CORBA.Object parse(URI uri) {
        String ssp = uri.getSchemeSpecificPart();

        int len = ssp.length();

        // Allow up to three leading '/''s to match commonly used forms of the "file:" URL scheme
        final String prefix = relative_ ? "" : "/";
        final String filename = ssp.replaceFirst("^/{0,3}", prefix);
        if (filename.equals(prefix)) throw new BAD_PARAM(describeBadParam(MinorBadSchemeSpecificPart) + ": no file name specified", MinorBadSchemeSpecificPart, COMPLETED_NO);

        try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(filename), US_ASCII))) {
            String ref = in.readLine();
            return registry_.parse_url(ref);
        } catch (Exception e) {
            throw (BAD_PARAM)new BAD_PARAM(describeBadParam(MinorBadSchemeSpecificPart) + ": file error", MinorBadSchemeSpecificPart, COMPLETED_NO).initCause(e);
        }
    }
}
