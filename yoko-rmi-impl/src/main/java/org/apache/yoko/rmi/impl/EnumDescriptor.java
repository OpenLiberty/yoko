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

import java.io.ObjectStreamField;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Optional;

class EnumDescriptor extends ValueDescriptor {
    private static final ObjectStreamField[] SERIAL_PERSISTENT_FIELDS = {
        new ObjectStreamField("name", String.class)
    };
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
    ObjectStreamField[] findSerialPersistentFields() {
        // Use serialPersistentFields mechanism to declare only the name field should be marshalled
        return SERIAL_PERSISTENT_FIELDS;
    }

    @Override
    Optional<Method> getReadObjectMethod() {
        return Optional.empty();
    }

    @Override
    Optional<Method> getReadResolveMethod() {
        return Optional.empty();
    }

    @Override
    Optional<Method> getWriteObjectMethod() {
        return Optional.empty();
    }

    @Override
    Optional<Method> getWriteReplaceMethod() {
        return Optional.empty();
    }

    @Override
    Optional<Constructor> getConstructor() {
        return Optional.empty();
    }
}
