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

import org.omg.CORBA.LocalObject;
import org.omg.CORBA.Policy;

final public class ProtocolPolicy_impl extends LocalObject
        implements ProtocolPolicy {
    private String[] value_;

    // ------------------------------------------------------------------
    // Standard IDL to Java Mapping
    // ------------------------------------------------------------------

    public String[] value() {
        return value_;
    }

    public boolean contains(String id) {
        for (int i = 0; i < value_.length; i++)
            if (id.equals(value_[i]))
                return true;

        return false;
    }

    public int policy_type() {
        return PROTOCOL_POLICY_ID.value;
    }

    public Policy copy() {
        return this;
    }

    public void destroy() {
    }

    // ------------------------------------------------------------------
    // Yoko internal functions
    // Application programs must not use these functions directly
    // ------------------------------------------------------------------

    public ProtocolPolicy_impl(String[] value) {
        value_ = value;
    }
}
