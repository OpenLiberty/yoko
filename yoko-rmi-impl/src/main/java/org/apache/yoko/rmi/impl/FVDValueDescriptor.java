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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.collectingAndThen;
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
    ValueDescriptor genSuperDescriptor() { return superDesc; }

    @Override
    protected List<FieldDescriptor> genFields() {
        MARSHAL_LOG.finer(() -> "Computing field descriptors for " + fvd.name + " version " + fvd.version);
        return Arrays.stream(fvd.members)
                .map(vm -> {
                    FieldDescriptor fd = findField(vm);
                    MARSHAL_LOG.finer(() -> String.format("\t%s -> %s", describe(vm), describe(fd)));
                    return fd;
                })
                .collect(collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
    }

    private static String describe(FieldDescriptor fd) {
        return fd == null ? null : String.format("FieldDescriptor[%s in %s]", fd.java_name, Optional.of(fd.declaringClass).map(Class::getName).orElse(""));
    }

    private static String describe(ValueMember vm) {
        return vm == null ? null : String.format("ValueMember[name=\"%s\", id=\"%s\"]", vm.name, vm.id);
    }

    private FieldDescriptor findField(ValueMember valueMember) {
        for (Class<?> c = getType(); c != null; c = c.getSuperclass()) {
            TypeDescriptor desc = repo.getDescriptor(c);
            if (!(desc instanceof ValueDescriptor)) continue;

            ValueDescriptor valueDesc = (ValueDescriptor) desc;
            for (FieldDescriptor fd : valueDesc.getFields()) {
                if (fd.getIDLName().equals(valueMember.name)) {
                    return fd;
                }
            }
        }
        // There was no matching field in the local implementation, so create a field descriptor
        // that will read from the stream but not assign to any local field
        return new ValueMemberFieldDescriptor(getType(), valueMember, repo);
    }

    @Override
    String genRepId() {
        return repid;
    }

    @Override
    FullValueDescription getFullValueDescription() {
        return fvd;
    }

    @Override
    TypeCode genTypeCode() { return fvd.type; }

    @Override
    public boolean isCustomMarshalled() {
        return fvd.is_custom;
    }
}
