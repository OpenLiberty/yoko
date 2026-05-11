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
package org.apache.yoko.rmi.impl;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;

class EnumDescriptor extends ValueDescriptor {
    public EnumDescriptor(Class<?> type, TypeRepository repo) {
        super(type, repo);
    }

    @Override
    protected final long getSerialVersionUID() {
        return 0L;
    }

    @Override
    protected final boolean isEnum() {
        return true;
    }

    @Override
    protected boolean includeField(java.lang.reflect.Field f) {
        // Only include the name field, exclude ordinal to match what's marshalled
        return "name".equals(f.getName());
    }
}
