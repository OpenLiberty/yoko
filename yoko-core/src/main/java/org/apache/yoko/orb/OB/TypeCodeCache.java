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

import java.util.Hashtable;

import org.apache.yoko.orb.CORBA.TypeCode;

public final class TypeCodeCache {
    private Hashtable cache_; // The type code cache hashtable

    private static TypeCodeCache instance_ = null; // Singleton instance

    private static final Object instanceMutex_ = new Object();

    // ----------------------------------------------------------------------
    // ORBInstance public member implementation
    // ----------------------------------------------------------------------

    TypeCodeCache() {
        cache_ = new Hashtable(63);
    }

    static public TypeCodeCache instance() {
        synchronized (instanceMutex_) {
            if (instance_ == null) {
                instance_ = new TypeCodeCache();
            }
        }
        return instance_;
    }

    synchronized public TypeCode get(String id) {
        return (TypeCode) cache_.get(id);
    }

    synchronized public void put(String id,
            TypeCode tc) {
        if (cache_.containsKey(id))
            return;

        cache_.put(id, tc);
    }
}
