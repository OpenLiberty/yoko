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

import org.apache.yoko.util.concurrent.LazyReference;
import org.omg.CORBA.MARSHAL;

import javax.rmi.CORBA.ClassDesc;
import java.lang.reflect.Field;
import java.security.PrivilegedAction;

import static java.security.AccessController.doPrivileged;
import static org.apache.yoko.util.Exceptions.as;

abstract class ClassBaseDescriptor extends ValueDescriptor {

    ClassBaseDescriptor(Class type, TypeRepository repository) {
        super(type, repository);
    }

    private final LazyReference<Field> repidField = new LazyReference<>(this::genRepIdField);
    Field genRepIdField() {
        return findField("repid");
    }
    final Field getRepidField() {
        return repidField.get();
    }

    private final LazyReference<Field> codebaseField = new LazyReference<>(this::genCodebaseField);
    Field genCodebaseField() {
        return findField("codebase");
    }
    final Field getCodebaseField() {
        return codebaseField.get();
    }

    private Field findField(final String fieldName) {
        return doPrivileged((PrivilegedAction<Field>) () -> {
            try {
                Field f = ClassDesc.class.getDeclaredField(fieldName);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException e) {
                throw as(MARSHAL::new, e, "no such field: " + e);
            }
        });
    }
}
