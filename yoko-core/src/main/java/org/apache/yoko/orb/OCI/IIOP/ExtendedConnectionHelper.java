/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
*  contributor license agreements.  See the NOTICE file distributed with
*  this work for additional information regarding copyright ownership.
*  The ASF licenses this file to You under the Apache License, Version 2.0
*  (the "License"); you may not use this file except in compliance with
*  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */


/**
 * @version $Rev: 491396 $ $Date: 2006-12-30 22:06:13 -0800 (Sat, 30 Dec 2006) $
 */

package org.apache.yoko.orb.OCI.IIOP;

import org.omg.CORBA.ORB;
import org.omg.CORBA.Policy;
import org.omg.CSIIOP.TransportAddress;
import org.omg.IOP.TaggedComponent;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public interface ExtendedConnectionHelper
{
    void init(ORB orb, String parms);

    /**
     * The host may be encoded as described in {@link #getEndpoints(TaggedComponent, Policy[])}.
     * Implementors should use and {@link Util#isEncodedHost(String)}, {@link Util#decodeHost(String)},
     * and {@link Util#decodeHostInfo(String)} to retrieve the encoded information.
     */
    Socket createSocket(String host, int port) throws IOException;

    Socket createSelfConnection(InetAddress address, int port) throws IOException;

    ServerSocket createServerSocket(int port, int backlog, String[] params)  throws IOException;

    ServerSocket createServerSocket(int port, int backlog, InetAddress address, String[] params) throws IOException;

    /**
     * The component tags this helper knows about, e.g. TAG_CSI_SEC_MECH_LIST.
     *
     * @return an array of known tags, possibly empty but not null
     */
    int[] tags();

    /**
     * The policy-compliant endpoints from the specified tagged component.
     * <br>
     * Note that the host strings in the endpoints may encode additional information.
     * Implementors should use {@link Util#encodeHost(String, String, String)} to encode the information.
     *
     * @param taggedComponent the tagged component to examine for endpoints:
     *                        note that <code>taggedComponent.tag</code> must be in <code>this.tags()</code>
     *
     * @param policies the policies against which to filter the possible endpoints
     *
     * @return a possibly empty but non-null array of endpoints
     */
    TransportAddress[] getEndpoints(TaggedComponent taggedComponent, Policy[] policies);
}

