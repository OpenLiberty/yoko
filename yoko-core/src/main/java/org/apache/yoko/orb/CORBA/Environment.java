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
package org.apache.yoko.orb.CORBA;

final public class Environment extends org.omg.CORBA.Environment {
    private Exception exception_;

    // ------------------------------------------------------------------
    // Standard IDL to Java Mapping
    // ------------------------------------------------------------------

    public void exception(Exception except) {
        exception_ = except;
    }

    public Exception exception() {
        return exception_;
    }

    public void clear() {
        exception_ = null;
    }

    // ------------------------------------------------------------------
    // Yoko internal functions
    // Application programs must not use these functions directly
    // ------------------------------------------------------------------

    public Environment() {
    }
}
