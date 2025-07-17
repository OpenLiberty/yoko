/*
 * Copyright 2025 IBM Corporation and others.
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
package org.apache.yoko.orb.OCI;

//
// IDL:orb.yoko.apache.org/OCI/ConFactoryRegistry:1.0
//
/**
 *
 * A registry for Connector factories.
 *
 * @see Connector
 * @see ConFactory
 *
 **/

public interface ConFactoryRegistryOperations
{
    //
    // IDL:orb.yoko.apache.org/OCI/ConFactoryRegistry/add_factory:1.0
    //
    /**
     *
     * Adds a Connector factory to the registry.
     *
     * @param factory The Connector factory to add.
     *
     * @exception FactoryAlreadyExists If a factory already exists with the
     * same plugin id as the given factory.
     *
     **/

    void
    add_factory(ConFactory factory)
        throws FactoryAlreadyExists;

    //
    // IDL:orb.yoko.apache.org/OCI/ConFactoryRegistry/get_factory:1.0
    //
    /**
     *
     * Returns the factory with the given plugin id.
     *
     * @param id The plugin id.
     *
     * @return The Connector factory.
     *
     * @exception NoSuchFactory If no factory was found with a matching
     * plugin id.
     *
     **/

    ConFactory
    get_factory(String id)
        throws NoSuchFactory;

    //
    // IDL:orb.yoko.apache.org/OCI/ConFactoryRegistry/get_factories:1.0
    //
    /**
     *
     * Returns all registered factories.
     *
     * @return The Connector factories.
     *
     **/

    ConFactory[]
    get_factories();
}
