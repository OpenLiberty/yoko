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

import static java.lang.Character.digit;
import static org.apache.yoko.logging.VerboseLogging.IOR_LOG;
import static org.apache.yoko.util.Hex.formatHexPara;
import static org.apache.yoko.util.MinorCodes.MinorBadAddress;
import static org.apache.yoko.util.MinorCodes.MinorBadSchemeSpecificPart;
import static org.apache.yoko.util.MinorCodes.describeBadParam;
import static org.omg.CORBA.CompletionStatus.COMPLETED_NO;

import java.net.URI;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.apache.yoko.orb.OB.CorbalocURLSchemePackage.ProtocolAlreadyExists;
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.IOP.IOR;
import org.omg.IOP.TaggedProfile;

public class CorbalocURLScheme_impl implements CorbalocURLScheme {
    private final ORBInstance orbInstance_;

    private final Hashtable<String,CorbalocProtocol> protocols_ = new Hashtable<>();

    public CorbalocURLScheme_impl(ORBInstance orbInstance) {
        orbInstance_ = orbInstance;
    }

    public String name() { return "corbaloc"; }

    public org.omg.CORBA.Object parse(URI uri) {
        // Use the undecoded scheme-specific part because the object key in it might be percent-encoded binary data
        String ssp = uri.getRawSchemeSpecificPart();
        int slash = ssp.indexOf('/');

        // Although an object key is optional according to the specification, we consider this to be an invalid URL
        if (slash == -1) throw new BAD_PARAM(describeBadParam(MinorBadSchemeSpecificPart) + ": no key specified", MinorBadSchemeSpecificPart, COMPLETED_NO);
        if (0 == slash) throw new BAD_PARAM(describeBadParam(MinorBadAddress) + ": no protocol address", MinorBadAddress, COMPLETED_NO);

        String key = ssp.substring(slash + 1);
        String addressList = ssp.substring(0, slash);
        return parseAddresses(addressList, key);
    }

    public void addProtocol(CorbalocProtocol protocol) throws ProtocolAlreadyExists {
        String name = protocol.name();
        if (protocols_.containsKey(name)) throw new ProtocolAlreadyExists();
        protocols_.put(name, protocol);
    }

    private org.omg.CORBA.Object parseAddresses(String addressList, String keyStr) {
        // Check for rir:
        if (addressList.startsWith("rir:")) {
            if (addressList.contains(",")) throw new BAD_PARAM(describeBadParam(MinorBadSchemeSpecificPart) + ": rir cannot be used with other protocols", MinorBadSchemeSpecificPart, COMPLETED_NO);
            if (addressList.length() > 4) throw new BAD_PARAM(describeBadParam(MinorBadAddress) + ": rir does not allow an address", MinorBadAddress, COMPLETED_NO);

            try {
                InitialServiceManager initialServiceManager = orbInstance_.getInitialServiceManager();
                return initialServiceManager.resolveInitialReferences(keyStr);
            } catch (InvalidName ex) {
                throw new BAD_PARAM(describeBadParam(MinorBadSchemeSpecificPart) + ": invalid initial reference token \"" + keyStr + "\"", MinorBadSchemeSpecificPart, COMPLETED_NO);
            }
        }

        byte[] key = getKeyBytes(keyStr);

        // Convert addresses (separated by ',') into IOR profiles
        String[] addresses =  addressList.split(",");
        List<TaggedProfile> profiles = new ArrayList<>();

        for (String address: addresses) {
            String[] parts = address.split(":", 2);
            if (parts.length == 1) throw new BAD_PARAM(describeBadParam(MinorBadSchemeSpecificPart) + ": no protocol", MinorBadSchemeSpecificPart, COMPLETED_NO);
            String protocol = parts[0].isEmpty() ? "iiop" : parts[0].toLowerCase();
            // Check for rir (again)
            if (protocol.equals("rir")) throw new BAD_PARAM(describeBadParam(MinorBadSchemeSpecificPart) + ": rir cannot be used with other protocols", MinorBadSchemeSpecificPart, COMPLETED_NO);
            String addr = parts[1];

            CorbalocProtocol p = protocols_.get(protocol);
            if (p == null) {
                IOR_LOG.warning(() -> "ignoring unknown protocol: " + protocol);
                continue;
            }
            TaggedProfile profile = p.parseAddress(addr, key);
            profiles.add(profile);
        }

        if (profiles.isEmpty()) throw new BAD_PARAM(describeBadParam(MinorBadSchemeSpecificPart) + ": no valid protocol addresses", MinorBadSchemeSpecificPart, COMPLETED_NO);

        IOR ior = new IOR("", profiles.stream().toArray(TaggedProfile[]::new));
        ObjectFactory objectFactory = orbInstance_.getObjectFactory();
        return objectFactory.createObject(ior);
    }

    private static byte[] getKeyBytes(String keyStr) {
        byte[] bytes = new byte[keyStr.length()];
        int byteCount = 0;
        for (int i = 0; i < keyStr.length(); i++) {
            char ch = keyStr.charAt(i);
            // if the char is a %, treat the next two chars as hex digits.
            if (ch == '%') {
                // check there are enough chars remainng
                if (i + 3 > keyStr.length()) throw new BAD_PARAM(describeBadParam(MinorBadSchemeSpecificPart) + ": invalid percent encoding in key", MinorBadSchemeSpecificPart, COMPLETED_NO);
                // read each hex char
                final int hi = digit(keyStr.charAt(++i), 16);
                final int lo = digit(keyStr.charAt(++i), 16);
                if (-1 == hi || -1 == lo) throw new BAD_PARAM(describeBadParam(MinorBadSchemeSpecificPart) + ": invalid percent encoding in key", MinorBadSchemeSpecificPart, COMPLETED_NO);
                // combine the high and low nybbles into a single byte
                bytes[byteCount++] = (byte) (hi << 4 | lo);
            } else {
                // otherwise just check it is an 8-bit (Latin 1) character.
                if (ch > 0xFF) throw new BAD_PARAM(describeBadParam(MinorBadSchemeSpecificPart) + ": invalid character in key, char value = 0x" + Integer.toHexString(ch), MinorBadSchemeSpecificPart, COMPLETED_NO);
                bytes[byteCount++] = (byte) ch;
            }
        }
        if (byteCount == bytes.length) {
            IOR_LOG.finest(() -> "read corbaloc key as latin string:" + keyStr);
            return bytes;
        }
        byte[] result = new byte[byteCount];
        System.arraycopy(bytes, 0, result, 0, byteCount);
        IOR_LOG.fine(() -> "converted percent encoded corbaloc key \n\t" + keyStr + "\nto bytes: \n" + formatHexPara(result));
        return result;
    }
}
