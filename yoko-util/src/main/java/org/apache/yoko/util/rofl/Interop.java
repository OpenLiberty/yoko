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
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.apache.yoko.util.rofl;

import static org.apache.yoko.util.rofl.Rofl.RemoteOrb.IBM;

public enum Interop {
    ;

    /**
     * There is a mismatch between the defaultWriteObject() behaviour
     * of java.util.Date in Java 8 and Java 11.
     * Some ORBs are sensitive to this.
     * If Yoko is talking to an IBM Java ORB (Java &lt;=8)
     * mask the defaultWriteObject flag.
     * @param customRepId the custom repository ID
     * @return true if the defaultWriteObject flag should be set
     */
    public static boolean flagDefaultWriteObject(String customRepId) {
        if (null == customRepId) return true;
        if (RoflThreadLocal.get().type() != IBM) return true;
        if (!customRepId.endsWith(":686A81014B597419")) return true;
        return !customRepId.startsWith("RMI:org.omg.custom.java.util.Date:");
    }
}
