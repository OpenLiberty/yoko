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

import static java.util.logging.Level.WARNING;
import static org.apache.yoko.logging.VerboseLogging.NAMING_LOG;
import static org.apache.yoko.util.MinorCodes.MinorBadAddress;
import static org.apache.yoko.util.MinorCodes.MinorBadSchemeName;
import static org.apache.yoko.util.MinorCodes.MinorBadSchemeSpecificPart;
import static org.apache.yoko.util.MinorCodes.describeBadParam;
import static org.omg.CORBA.CompletionStatus.COMPLETED_NO;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.Hashtable;

import org.apache.yoko.orb.OB.URLRegistryPackage.SchemeAlreadyExists;
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.LocalObject;

public class URLRegistry_impl extends LocalObject implements URLRegistry {
    private final Hashtable<String,URLScheme> schemes_ = new Hashtable<>();

    public void add_scheme(URLScheme scheme) throws SchemeAlreadyExists {
        String name = scheme.name();
        if (schemes_.containsKey(name)) throw new SchemeAlreadyExists();
        schemes_.put(name, scheme);
    }

    public URLScheme find_scheme(String name) {
        return schemes_.get(name);
    }

    public org.omg.CORBA.Object parse_url(String url) {
        try {
            URI uri = new URI(url);
            String schemeName = uri.getScheme().toLowerCase();
            URLScheme scheme = find_scheme(schemeName);
            if (scheme == null) throw new BAD_PARAM(describeBadParam(MinorBadSchemeName), MinorBadSchemeName, COMPLETED_NO);
            return scheme.parse(uri);
        } catch (URISyntaxException e) {
            NAMING_LOG.log(WARNING, e, () -> String.format("problem constructing URI from \"%s\"", url));
            int index = e.getIndex();
            if (index < 1) throw (BAD_PARAM)new BAD_PARAM(e.getMessage(), MinorBadSchemeName, COMPLETED_NO).initCause(e);
            String substring = e.getInput().substring(0, index);
            if (!substring.contains(":")) throw (BAD_PARAM)new BAD_PARAM(e.getMessage(), MinorBadSchemeName, COMPLETED_NO).initCause(e);
            // In java.net.URI, the address in a corbaname/corbaloc is retrieved using URI.getSchemeSpecificPart()
            // Note the naming differs in the OMG minor codes and the address is referred to as address
            if (!substring.contains("#")) throw (BAD_PARAM)new BAD_PARAM(e.getMessage(), MinorBadAddress, COMPLETED_NO).initCause(e);
            // I assume the scheme-specific part is the bit after the # in the OMG minor code nomenclature.
            // In java.net.URI this is retrieved using URI.getFragment()
            throw (BAD_PARAM)new BAD_PARAM(e.getMessage(), MinorBadSchemeSpecificPart, COMPLETED_NO).initCause(e);
        } catch (Exception e) {
            NAMING_LOG.log(WARNING, e, () -> String.format("problem constructing URI from \"%s\"", url));
            throw (BAD_PARAM)new BAD_PARAM("error parsing URL: " + url, MinorBadSchemeName, COMPLETED_NO).initCause(e);
        }
    }
}
