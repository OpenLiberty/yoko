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

import org.omg.CORBA.LocalObject;

import java.util.Vector;

public final class AccFactoryRegistry_impl extends LocalObject
        implements AccFactoryRegistry {
    //
    // All acceptor factories
    //
    Vector<AccFactory> factories_ = new Vector<>();

    // ------------------------------------------------------------------
    // Standard IDL to Java Mapping
    // ------------------------------------------------------------------

    public synchronized void add_factory(AccFactory factory)
            throws FactoryAlreadyExists {
        String id = factory.id();

        for (int i = 0; i < factories_.size(); i++)
            if (id.equals(factories_.elementAt(i).id()))
                throw new FactoryAlreadyExists(id);

        factories_.addElement(factory);
    }

    public synchronized AccFactory get_factory(String id) throws NoSuchFactory {
        for (int i = 0; i < factories_.size(); i++) {
            AccFactory factory = factories_.elementAt(i);
            if (id.equals(factory.id()))
                return factory;
        }

        throw new NoSuchFactory(id);
    }

    public synchronized AccFactory[] get_factories() {
        AccFactory[] result = new AccFactory[factories_.size()];
        factories_.copyInto(result);
        return result;
    }

    // ------------------------------------------------------------------
    // Yoko internal functions
    // Application programs must not use these functions directly
    // ------------------------------------------------------------------

    public AccFactoryRegistry_impl() {
    }
}
