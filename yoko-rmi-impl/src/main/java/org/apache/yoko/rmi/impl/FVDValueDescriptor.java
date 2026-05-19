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

import org.omg.CORBA.TypeCode;
import org.omg.CORBA.ValueDefPackage.FullValueDescription;
import org.omg.CORBA.ValueMember;

import java.util.Arrays;
import java.util.Optional;

import static org.apache.yoko.logging.VerboseLogging.MARSHAL_LOG;

final class FVDValueDescriptor extends ValueDescriptor {
    final FullValueDescription fvd;
    final String repid;
    private final ValueDescriptor superDesc;

    FVDValueDescriptor(FullValueDescription fvd, Class<?> clazz,
            TypeRepository rep, String repid, ValueDescriptor super_desc) {
        super(clazz, rep);

        this.repid = repid;
        this.fvd = fvd;
        this.superDesc = super_desc;

        init();
    }

    @Override
    ValueDescriptor getSuperDescriptor() {
        return superDesc;
    }

    @Override
    protected FieldDescriptor[] genFields() {
        MARSHAL_LOG.finer(() -> "Computing field descriptors for " + fvd.name + " version " + fvd.version);
        return Arrays.stream(fvd.members)
                .map(vm -> {
                    FieldDescriptor fd = findField(vm);
                    MARSHAL_LOG.finer(() -> String.format("\t%s -> %s", describe(vm), describe(fd)));
                    return fd;
                })
                .toArray(FieldDescriptor[]::new);
    }

    private static String describe(FieldDescriptor fd) {
        return fd == null ? null : String.format("FieldDescriptor[%s in %s]", fd.java_name, Optional.of(fd.declaringClass).map(Class::getName).orElse(""));
    }

    private static String describe(ValueMember vm) {
        return vm == null ? null : String.format("ValueMember[name=\"%s\", id=\"%s\"]", vm.name, vm.id);
    }

    private FieldDescriptor findField(ValueMember valueMember) {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            Optional<FieldDescriptor> result = Optional.of(repo.getDescriptor(c))
                    .filter(ValueDescriptor.class::isInstance)
                    .map(ValueDescriptor.class::cast)
                    .map(ValueDescriptor::getFields)
                    .flatMap(fields -> Arrays.stream(fields)
                            .filter(fd -> fd.getIDLName().equals(valueMember.name))
                            .findFirst());
            if (result.isPresent()) return result.get();
        }
        // There was no matching field in the local implementation, so create a field descriptor
        // that will read from the stream but not assign to any local field
        return new ValueMemberFieldDescriptor(type, valueMember, repo);
    }

    @Override
    String getRepositoryID() {
        return repid;
    }

    @Override
    org.omg.CORBA.ValueDefPackage.FullValueDescription getFullValueDescription() {
        return fvd;
    }

    @Override
    TypeCode getTypeCode() { return fvd.type; }

    @Override
    public boolean isCustomMarshalled() {
        return fvd.is_custom;
    }
}
