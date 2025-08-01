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
package org.omg.CORBA;

final public class TRANSACTION_UNAVAILABLE extends org.omg.CORBA.SystemException {
    public TRANSACTION_UNAVAILABLE() {
        super("", 0, CompletionStatus.COMPLETED_NO);
    }

    public TRANSACTION_UNAVAILABLE(int minor, CompletionStatus completed) {
        super("", minor, completed);
    }

    public TRANSACTION_UNAVAILABLE(String reason) {
        super(reason, 0, CompletionStatus.COMPLETED_NO);
    }

    public TRANSACTION_UNAVAILABLE(String reason, int minor, CompletionStatus completed) {
        super(reason, minor, completed);
    }
}
