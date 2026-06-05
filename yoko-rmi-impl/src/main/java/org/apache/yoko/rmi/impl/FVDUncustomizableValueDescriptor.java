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
package org.apache.yoko.rmi.impl;

import org.omg.CORBA.ValueDefPackage.FullValueDescription;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.function.Function.identity;

final class FVDUncustomizableValueDescriptor extends FVDValueDescriptor {
    FVDUncustomizableValueDescriptor(FullValueDescription fvd, Class<?> type, TypeRepository repo, String repId, ValueDescriptor superDesc) {
        this(fvd, type, repo, repId, superDesc, null);
    }

    FVDUncustomizableValueDescriptor(FullValueDescription fvd, Class<?> type, TypeRepository repo, String repId, ValueDescriptor superDesc, Supplier<ValueReader> readerSupplier) {
        super(fvd, type, repo, repId, superDesc, readerSupplier);
    }

    @Override
    boolean isExternalizable() { return false; }

    @Override
    Method genReadObjectMethod() { return null; }

    @Override
    Function<Serializable, Serializable> genReadResolver() { return identity(); }

    @Override
    Method genWriteObjectMethod() { return null; }

    @Override
    Function<Serializable, Serializable> genWriteReplacer() { return identity(); }

    @Override
    Serializable createBlankInstance() { return null; }
}
