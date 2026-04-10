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
package org.omg.PortableInterceptor;

import org.omg.CORBA.*;

final class MyClientPolicy_impl extends LocalObject implements MyClientPolicy {
    private final int value;
    MyClientPolicy_impl(int value) { this.value = value; }
    public int value() { return value; }
    public int policy_type() { return MY_CLIENT_POLICY_ID.value; }
    public Policy copy() { return this; }
    public void destroy() {}
}
