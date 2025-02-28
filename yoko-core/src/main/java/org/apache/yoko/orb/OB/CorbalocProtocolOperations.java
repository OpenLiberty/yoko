/*
 * Copyright 2024 IBM Corporation and others.
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

import org.omg.IOP.TaggedProfile;

//
// IDL:orb.yoko.apache.org/OB/CorbalocProtocol:1.0
//
/**
 *
 * The <code>corbaloc</code> URL scheme supports multiple protocols,
 * the most common of which is <code>iiop</code>. The CorbalocURLScheme
 * delegates the parsing of protocol address information to a
 * registered CorbalocProtocol object.
 *
 * @see CorbalocURLScheme
 *
 **/

public interface CorbalocProtocolOperations
{
    //
    // IDL:orb.yoko.apache.org/OB/CorbalocProtocol/name:1.0
    //
    /**
     *
     * Each protocol must have a unique name. All protocol names must
     * be in lower case.
     *
     **/

    String
    name();

    //
    // IDL:orb.yoko.apache.org/OB/CorbalocProtocol/parse_address:1.0
    //
    /**
     *
     * Parse a protocol address and create a tagged profile for inclusion
     * in an IOR.
     *
     * @param addr The protocol address, not including the protocol name
     * or trailing object key. For example, the URL
     * <code>corbaloc:iiop:1.2@localhost:5000/Key</code> would result in
     * an address argument of <code>1.2@localhost:5000</code>.
     *
     * @param key The object key from the URL, converted into a sequence
     * of octets.
     *
     * @return A tagged profile.
     *
     * @exception BAD_PARAM In case the address is invalid.
     *
     **/

    TaggedProfile
    parse_address(String addr,
                  byte[] key);

    //
    // IDL:orb.yoko.apache.org/OB/CorbalocProtocol/destroy:1.0
    //
    /**
     *
     * Release any resources held by the object.
     *
     **/

    void
    destroy();
}
