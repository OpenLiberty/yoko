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

import org.omg.CORBA.ORB;
import org.omg.CORBA.TypeCode;
import org.omg.CORBA_2_3.portable.InputStream;
import org.omg.CORBA_2_3.portable.OutputStream;

import java.io.PrintWriter;

class AbstractObjectDescriptor extends ValueDescriptor {
    protected AbstractObjectDescriptor(Class<?> type, TypeRepository repository) {
        super(type, repository,
            in -> ((InputStream) in).read_abstract_interface(),
            (out, value) -> ((OutputStream) out).write_abstract_interface(value));
    }

    @Override
    String genRepId() {
        return String.format("IDL:%s:1.0", getType().getName().replace('.', '/'));
    }

    @Override
    protected TypeCode genTypeCode() {
        ORB orb = ORB.init();
        return orb.create_abstract_interface_tc(getRepositoryID(), getType().getName());
    }

    @Override
    long genClassHash() { return 0L; }

    @Override
    Object copyObject(Object value, CopyState state) {
        throw new IllegalStateException("not serializable " + value.getClass().getName());
    }

    @Override
    void writeMarshalValue(PrintWriter pw, String outName, String paramName) {
        pw.print("javax.rmi.CORBA.Util.writeAbstractObject(");
        pw.print(outName);
        pw.print(',');
        pw.print(paramName);
        pw.print(')');
    }

    @Override
    void writeUnmarshalValue(PrintWriter pw, String inName) {
        pw.print(inName);
        pw.print(".read_abstract_interface()");
    }
}
