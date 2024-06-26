/*
 * Copyright 2010 IBM Corporation and others.
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
package org.apache.yoko.orb.IMR;

//
// IDL:orb.yoko.apache.org/IMR/ServerFactory:1.0
//
/***/

public interface ServerFactoryOperations
{
    //
    // IDL:orb.yoko.apache.org/IMR/ServerFactory/get_server:1.0
    //
    /**
     *
     * Retrieve a server record.
     *
     * @param server The server name
     *
     * @return The server record.
     *
     **/

    Server
    get_server(String server)
        throws NoSuchServer;

    //
    // IDL:orb.yoko.apache.org/IMR/ServerFactory/create_server_record:1.0
    //
    /**
     *
     * Create server record.
     *
     * @param server The server name
     *
     * @return The ID of the created server
     *
     **/

    Server
    create_server_record(String server)
        throws ServerAlreadyRegistered;

    //
    // IDL:orb.yoko.apache.org/IMR/ServerFactory/list_servers:1.0
    //
    /**
     *
     * List all the server records.
     *
     * @return A sequence of server records.
     *
     **/

    Server[]
    list_servers();

    //
    // IDL:orb.yoko.apache.org/IMR/ServerFactory/list_servers_by_host:1.0
    //
    /**
     *
     * List the server records for a particular OAD/Host.
     *
     * @return A sequence of server records.
     *
     **/

    Server[]
    list_servers_by_host(String host);

    //
    // IDL:orb.yoko.apache.org/IMR/ServerFactory/list_orphaned_servers:1.0
    //
    /**
     *
     * List the orphaned server records.
     *
     * @return A sequence of server records.
     *
     **/

    Server[]
    list_orphaned_servers();
}
