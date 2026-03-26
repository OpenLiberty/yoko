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

import org.apache.yoko.orb.OB.CorbalocURLSchemePackage.ProtocolAlreadyExists;

/**
 * CorbalocURLScheme implements the <code>corbaloc</code> URL scheme,
 * and serves as a registry for CorbalocProtocol objects.
 *
 * @see CorbalocProtocol
 */
public interface CorbalocURLScheme extends URLScheme {
    /**
     * Register a new <code>corbaloc</code> protocol.
     * @param protocol The new protocol.
     * @throws org.apache.yoko.orb.OB.CorbalocURLSchemePackage.ProtocolAlreadyExists Another protocol already exists with the same name.
     */
    void addProtocol(CorbalocProtocol protocol) throws ProtocolAlreadyExists;
}
