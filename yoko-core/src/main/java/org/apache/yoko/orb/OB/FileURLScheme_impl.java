/*
 * Copyright 2021 IBM Corporation and others.
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

import org.apache.yoko.util.MinorCodes;

public class FileURLScheme_impl extends org.omg.CORBA.LocalObject implements
        URLScheme {
    private boolean relative_;

    private URLRegistry registry_;

    // ------------------------------------------------------------------
    // FileURLScheme_impl constructor
    // ------------------------------------------------------------------

    public FileURLScheme_impl(boolean relative, URLRegistry registry) {
        relative_ = relative;
        registry_ = registry;
    }

    // ------------------------------------------------------------------
    // Standard IDL to Java Mapping
    // ------------------------------------------------------------------

    public String name() {
        return (relative_ ? "relfile" : "file");
    }

    public org.omg.CORBA.Object parse_url(String url) {
        int startIdx;
        if (relative_)
            startIdx = 8; // skip "relfile:"
        else
            startIdx = 5; // skip "file:"

        int len = url.length();

        //
        // Allow up to three leading '/''s to match commonly used forms
        // of the "file:" URL scheme
        // 
        for (int n = 0; startIdx < len && n < 3; n++, startIdx++)
            if (url.charAt(startIdx) != '/')
                break;

        if (startIdx >= len)
            throw new org.omg.CORBA.BAD_PARAM(MinorCodes
                    .describeBadParam(MinorCodes.MinorBadSchemeSpecificPart)
                    + ": no file name specified",
                    MinorCodes.MinorBadSchemeSpecificPart,
                    org.omg.CORBA.CompletionStatus.COMPLETED_NO);

        String fileName;
        if (relative_)
            fileName = "";
        else
            fileName = "/";
        fileName += URLUtil.unescapeURL(url.substring(startIdx));

        try {
            java.io.FileInputStream file = new java.io.FileInputStream(fileName);
            java.io.BufferedReader in = new java.io.BufferedReader(
                    new java.io.InputStreamReader(file));
            String ref = in.readLine();
            file.close();

            return registry_.parse_url(ref);
        } catch (java.io.IOException ex) {
            throw new org.omg.CORBA.BAD_PARAM(MinorCodes
                    .describeBadParam(MinorCodes.MinorBadSchemeSpecificPart)
                    + ": file error", MinorCodes.MinorBadSchemeSpecificPart,
                    org.omg.CORBA.CompletionStatus.COMPLETED_NO);
        }
    }

    public void destroy() {
        registry_ = null;
    }
}
