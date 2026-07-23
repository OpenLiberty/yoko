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
import java.lang.invoke.MethodHandle;
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
    final MethodHandle genReadObjectHandle() { return null; }
    @Override
    final Function<Serializable, Serializable> genReadResolver() {
        return identity();
    }
    @Override
    final MethodHandle genWriteObjectHandle() {
        return null;
    }
    @Override
    final Function<Serializable, Serializable> genWriteReplacer() { return identity(); }
    @Override
    final Supplier<Serializable> genBlankInstanceSupplier() { return NULL_SERIALIZABLE_SUPPLIER; }
}
