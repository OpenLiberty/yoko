/*==============================================================================
 * Copyright 2015 IBM Corporation and others.
 * 
 * Licensed under the Apache License, Version 2.0 (the License);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *=============================================================================*/
package org.apache.yoko.orb.OCI;

//
// IDL:orb.yoko.apache.org/OCI/AcceptorInfo:1.0
//
/**
 *
 * Information on an OCI Acceptor object. Objects of this type must be
 * narrowed to an Acceptor information object for a concrete protocol
 * implementation, for example to <code>OCI::IIOP::AcceptorInfo</code>
 * in case the plug-in implements IIOP.
 *
 * @see Acceptor
 *
 **/

public interface AcceptorInfoOperations
{
    //
    // IDL:orb.yoko.apache.org/OCI/AcceptorInfo/id:1.0
    //
    /** The plugin id. */

    String
    id();

    //
    // IDL:orb.yoko.apache.org/OCI/AcceptorInfo/tag:1.0
    //
    /** The profile id tag. */

    int
    tag();

    //
    // IDL:orb.yoko.apache.org/OCI/AcceptorInfo/describe:1.0
    //
    /**
     *
     * Returns a human readable description of the transport.
     *
     * @return The description.
     *
     **/

    String
    describe();

    //
    // IDL:orb.yoko.apache.org/OCI/AcceptorInfo/add_accept_cb:1.0
    //

    //
    // IDL:orb.yoko.apache.org/OCI/AcceptorInfo/remove_accept_cb:1.0
    //
}
