/*
 * Copyright 2023 IBM Corporation and others.
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
package testify.bus.key;

import static java.lang.Integer.parseInt;

/**
 * A specialised type spec that handles {@link Integer} objects.
 */
public interface IntKey extends TypeKey<Integer> {
    default String stringify(Integer integer) { return String.valueOf(integer); }
    default Integer unstringify(String s) { return null == s || "null".equals(s) ? null : parseInt(s); }
}
