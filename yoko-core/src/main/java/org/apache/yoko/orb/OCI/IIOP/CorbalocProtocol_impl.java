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
package org.apache.yoko.orb.OCI.IIOP;

import org.apache.yoko.orb.CORBA.YokoOutputStream;
import org.apache.yoko.orb.OB.CorbalocProtocol;
import org.omg.CORBA.BAD_PARAM;
import org.omg.IIOP.ProfileBody_1_0;
import org.omg.IIOP.ProfileBody_1_0Helper;
import org.omg.IIOP.ProfileBody_1_1;
import org.omg.IIOP.ProfileBody_1_1Helper;
import org.omg.IIOP.Version;
import org.omg.IOP.TAG_INTERNET_IOP;
import org.omg.IOP.TaggedComponent;
import org.omg.IOP.TaggedProfile;

import static java.lang.Integer.parseInt;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.apache.yoko.logging.VerboseLogging.IOR_LOG;
import static org.apache.yoko.util.MinorCodes.MinorBadAddress;
import static org.apache.yoko.util.MinorCodes.describeBadParam;
import static org.omg.CORBA.CompletionStatus.COMPLETED_NO;

public class CorbalocProtocol_impl implements CorbalocProtocol {
    public String name() {
        return "iiop";
    }

    public TaggedProfile parseAddress(String addr, byte[] key) {
        final byte major = 1, minor;
        IOR_LOG.finest(() -> "parsing address: " + '"' + addr + '"');
        // Do we have an iiop version 'X.Y@' portion? (default is 1.0)
        // e.g. 1.2@localhost:2809
        String[] parts = addr.split("@", 2);
        final String hostAndPort;
        if (parts.length == 1) {
            hostAndPort = parts[0];
            minor = 0;
        } else {
            hostAndPort = parts[1];
            minor = checkMajorAndGetMinor(parts[0]);
        }

        // Hostname is terminated by ':port', or by end of string
        parts = hostAndPort.split(":", 2);
        final String host = parts[0];
        // Empty hostname is illegal (as is port ':YYYY' by itself)
        if (host.isEmpty()) throw new BAD_PARAM(describeBadParam(MinorBadAddress) + ": iiop hostname must be specified: " + addr, MinorBadAddress, COMPLETED_NO);

        // Valid range for port is 1 - 65535
        final short port = getPort(parts);

        // Create profile
        TaggedProfile profile = new TaggedProfile();
        profile.tag = TAG_INTERNET_IOP.value;

        try (YokoOutputStream out = new YokoOutputStream()) {
            // create a CDR encapsulation of the correct IDL profile struct
            out._OB_writeEndian();
            if (minor == 0) ProfileBody_1_0Helper.write(out, new ProfileBody_1_0(new Version(major, minor), host, port, key));
            else ProfileBody_1_1Helper.write(out, new ProfileBody_1_1(new Version(major, minor), host, port, key, new TaggedComponent[0]));
            profile.profile_data = out.copyWrittenBytes();
        }
        return profile;
    }

    private static byte checkMajorAndGetMinor(String ver) {
        IOR_LOG.finest(() -> "parsing version " + '"' + ver + '"');
        final byte minor;
        if (! ver.startsWith("1.")) throw new BAD_PARAM(describeBadParam(MinorBadAddress) + ": iiop version is invalid or unsupported: " + ver, MinorBadAddress, COMPLETED_NO);
        final String minorVer = ver.substring(2);
        try {
            final int n = parseInt(minorVer);
            if (n != (n & 0xFF)) throw new BAD_PARAM(describeBadParam(MinorBadAddress) + ": iiop version is invalid or unsupported: " + ver, MinorBadAddress, COMPLETED_NO);
            minor = (byte) min(n, 2);
        } catch (NumberFormatException e) {
            throw (BAD_PARAM) new BAD_PARAM(describeBadParam(MinorBadAddress) + ": iiop version is invalid or unsupported: " + ver, MinorBadAddress, COMPLETED_NO).initCause(e);
        }
        IOR_LOG.finest(() -> "returning minor version: " + minor);
        return minor;
    }

    private static short getPort(String[] parts) {
        final int port; // default port
        if (parts.length == 1) {
            port = 2809;
        } else {
            final String portStr = parts[1];
            try {
                port = parseInt(portStr);
            } catch (NumberFormatException ex) {
                throw new BAD_PARAM(describeBadParam(MinorBadAddress) + ": iiop port is invalid", MinorBadAddress, COMPLETED_NO);
            }
            if (port < 1 || port > 65535) throw new BAD_PARAM(describeBadParam(MinorBadAddress) + ": iiop port must be between 1 and 65535: " + portStr, MinorBadAddress, COMPLETED_NO);
        }
        return (short) port;
    }
}
