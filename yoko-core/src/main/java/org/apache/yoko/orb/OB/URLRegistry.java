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

import org.apache.yoko.orb.OB.URLRegistryPackage.SchemeAlreadyExists;
import org.omg.CORBA.BAD_PARAM;

/**
 * The URLRegistry holds all supported URL schemes.
 * @see URLScheme
 */
public interface URLRegistry {
    /**
     * Register a new URL scheme.
     * @param scheme The new scheme.
     * @throws SchemeAlreadyExists Another scheme already exists with the same name.
     */
    void addScheme(URLScheme scheme) throws SchemeAlreadyExists;

    /**
     * Find a scheme with the given name.
     * @param name The scheme name, in lower case.
     * @return The URLScheme, or nil if no match was found.
     */
    URLScheme findScheme(String name);

    /**
     * Convert a URL into an object reference by delegating to a
     * registered URLScheme object.
     * @param url The complete URL, including the scheme.
     * @return An object reference.
     * @throws BAD_PARAM if the URL is invalid.
     */
    org.omg.CORBA.Object parseUrl(String url);
}
