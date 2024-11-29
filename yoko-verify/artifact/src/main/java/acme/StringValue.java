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
package acme;

import java.util.Objects;

public final class StringValue implements AbstractValue, AbstractInterface {
    private static final long serialVersionUID = 1L;
    final String value;
    public StringValue(String value) { this.value = value; }
    public String toString() { return value; }
    public int hashCode() { return Objects.hash(value); }
    public boolean equals(Object o) {
        if (!(o instanceof StringValue)) return false;
        StringValue that = (StringValue)o;
        return Objects.equals(this.value, that.value);
    }
}
