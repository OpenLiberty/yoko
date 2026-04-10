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
package org.omg.IOP;

import org.omg.CORBA.Any;
import org.omg.CORBA.BAD_OPERATION;
import org.omg.CORBA.ORB;
import org.omg.CORBA.StructMember;
import org.omg.CORBA.TypeCode;
import org.omg.CORBA.portable.InputStream;
import org.omg.CORBA.portable.OutputStream;

//
// IDL:foo:1.0
//
public final class fooHelper {
    public static void insert(Any any, foo val) {
        OutputStream out = any.create_output_stream();
        write(out, val);
        any.read_value(out.create_input_stream(), type());
    }

    public static foo extract(Any any) {
        if(any.type().equivalent(type())) return read(any.create_input_stream());
        else throw new BAD_OPERATION();
    }

    private static TypeCode typeCode_;

    public static TypeCode type() {
        if(typeCode_ == null) {
            ORB orb = ORB.init();
            StructMember[] members = new StructMember[1];
            members[0] = new StructMember();
            members[0].name = "l";
            members[0].type = orb.get_primitive_tc(org.omg.CORBA.TCKind.tk_long);
            typeCode_ = orb.create_struct_tc(id(), "foo", members);
        }
        return typeCode_;
    }

    public static String id() { return "IDL:foo:1.0"; }

    public static foo read(InputStream in) {
        foo _ob_v = new foo();
        _ob_v.l = in.read_long();
        return _ob_v;
    }

    public static void write(OutputStream out, foo val) { out.write_long(val.l); }
}
