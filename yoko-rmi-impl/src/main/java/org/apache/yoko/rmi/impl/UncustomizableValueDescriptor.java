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

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.function.Function.identity;

abstract class UncustomizableValueDescriptor extends ValueDescriptor {
    UncustomizableValueDescriptor(Class<?> type, TypeRepository repo) {
        super(type, repo);
    }

    @Override
    final boolean isExternalizable() { return false; }

    @Override
    final Optional<Method> getReadObjectMethod() {
        return Optional.empty();
    }

    @Override
    final Function<Serializable, Serializable> getReadResolver() {
        return identity();
    }

    @Override
    final Optional<Method> getWriteObjectMethod() {
        return Optional.empty();
    }

    @Override
    final Optional<Method> getWriteReplaceMethod() { return Optional.empty(); }

    @Override
    final Supplier<Serializable> genBlankInstanceSupplier() {
        return () -> null;
    }
}
