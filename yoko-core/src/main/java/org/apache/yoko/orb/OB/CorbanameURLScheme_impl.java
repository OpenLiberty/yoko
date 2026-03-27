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

import org.apache.yoko.util.Assert;
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.ORB;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContext;
import org.omg.CosNaming.NamingContextHelper;

import java.net.URI;
import java.net.URISyntaxException;

import static org.apache.yoko.orb.OB.Corbanames.extractNameComponents;
import static org.apache.yoko.util.MinorCodes.MinorBadAddress;
import static org.apache.yoko.util.MinorCodes.MinorOther;
import static org.apache.yoko.util.MinorCodes.describeBadParam;
import static org.omg.CORBA.CompletionStatus.COMPLETED_NO;

public class CorbanameURLScheme_impl implements URLScheme {
    private final ORB orb_;
    private final URLScheme corbaloc_;

    public CorbanameURLScheme_impl(ORB orb, URLRegistry registry) {
        orb_ = orb;
        URLScheme scheme = registry.find_scheme("corbaloc");
        Assert.ensure(scheme != null);
        corbaloc_ = scheme;
        Assert.ensure(corbaloc_ != null);
    }

    public String name() { return "corbaname"; }

    public org.omg.CORBA.Object parse(URI corbaname) throws URISyntaxException {
        URI corbaloc = getCorbalocUri(corbaname);

        // Create object reference from the naming context
        org.omg.CORBA.Object nc = corbaloc_.parse(corbaloc);

        // If there is no URL fragment "#.....", or the stringified name is empty, then the URL refers to the naming context itself
        String fragment = corbaname.getFragment(); // undoes any URI encoding
        if (null == fragment || fragment.isEmpty()) return nc;

        NamingContext ctx = NamingContextHelper.narrow(nc);
        NameComponent[] path = extractNameComponents(corbaname);

        try {
            return ctx.resolve(path);
        } catch (Exception e) {
            throw (BAD_PARAM) new BAD_PARAM(describeBadParam(MinorOther) + ": corbaname evaluation error for \"" + corbaname + "\":" + e.getMessage(), MinorOther, COMPLETED_NO).initCause(e);
        }
    }

    private static URI getCorbalocUri(URI uri) throws URISyntaxException {
        String ssp = uri.getSchemeSpecificPart();
        String[] parts = ssp.split("/", 2);
        final String addressList = parts[0];
        final String service;
        if (parts.length == 1 || parts[1].isEmpty()) {
            // e.g., corbaname::localhost:5000
            // corbaname::localhost:5000/#foo
            // corbaname::localhost:5000#foo/bar
            service = "NameService";
        } else {
            // e.g., corbaname::localhost:5000/blah
            service = parts[1];
        }

        if (addressList.isEmpty()) throw new BAD_PARAM(describeBadParam(MinorBadAddress) + ": no protocol address", MinorBadAddress, COMPLETED_NO);

        // Create a corbaloc URI
        URI corbaloc = new URI("corbaloc", addressList + "/" + service, null);
        return corbaloc;
    }
}
