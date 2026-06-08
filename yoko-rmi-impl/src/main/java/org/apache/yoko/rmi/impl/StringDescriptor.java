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

import org.omg.CORBA.MARSHAL;
import org.omg.CORBA.TypeCode;
import org.omg.CORBA.WStringValueHelper;
import org.omg.CORBA.portable.OutputStream;

import java.io.Serializable;

class StringDescriptor extends ValueDescriptor {
    StringDescriptor(TypeRepository repository) {
        super(String.class, repository, WStringValueHelper::read, (out, value) -> WStringValueHelper.write(out, (String) value));
    }

    @Override
    final String genIDLName() {
        return "CORBA_WStringValue";
    }

    @Override
    String genPackageName() {
        return "CORBA";
    }

    @Override
    String genTypeName() { return "WStringValue"; }

    @Override
    public void writeValue(OutputStream out, Serializable value) {
        throw new MARSHAL("internal error");
    }

    @Override
    protected final TypeCode genTypeCode() {
        return WStringValueHelper.type();
    }

    @Override
    Object copyObject(Object value, CopyState state) {
        return value;
    }
}
