/*==============================================================================
 * Copyright 2010 IBM Corporation and others.
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
package org.apache.yoko.orb.Messaging;

final public class RequestStartTimePolicy_impl extends
        org.omg.CORBA.LocalObject implements
        org.omg.Messaging.RequestStartTimePolicy {
    private org.omg.TimeBase.UtcT value_;

    // ------------------------------------------------------------------
    // Standard IDL to Java Mapping
    // ------------------------------------------------------------------

    public org.omg.TimeBase.UtcT start_time() {
        return value_;
    }

    public int policy_type() {
        return org.omg.Messaging.REQUEST_START_TIME_POLICY_TYPE.value;
    }

    public org.omg.CORBA.Policy copy() {
        return this;
    }

    public void destroy() {
    }

    // ------------------------------------------------------------------
    // Yoko internal functions
    // Application programs must not use these functions directly
    // ------------------------------------------------------------------

    public RequestStartTimePolicy_impl(org.omg.TimeBase.UtcT value) {
        value_ = value;
    }
}
