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
package org.apache.yoko.util.cmsf;

public enum Cmsf {
    CMSFv1(1), CMSFv2(2);

    private final byte value;

    Cmsf(int value) {
        this.value = (byte)value;
    }

    public static Cmsf get(byte value) {
        return (value > 1) ? CMSFv2 : CMSFv1;
    }

    public final byte getValue() { return value; }

    public final byte[] toData() {
        return new byte[] {
                0, // big-endian
                value
        };
    }
}
