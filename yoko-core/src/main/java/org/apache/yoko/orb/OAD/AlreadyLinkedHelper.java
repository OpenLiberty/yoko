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
package org.apache.yoko.orb.OAD;

import static org.apache.yoko.util.MinorCodes.MinorReadIDMismatch;
import static org.apache.yoko.util.MinorCodes.MinorTypeMismatch;
import static org.omg.CORBA.CompletionStatus.COMPLETED_NO;

import org.apache.yoko.util.MinorCodes;
import org.omg.CORBA.Any;
import org.omg.CORBA.BAD_OPERATION;
import org.omg.CORBA.MARSHAL;
import org.omg.CORBA.ORB;
import org.omg.CORBA.StructMember;

import static org.apache.yoko.util.Arrays.emptyArray;
import org.omg.CORBA.TypeCode;
import org.omg.CORBA.portable.InputStream;
import org.omg.CORBA.portable.OutputStream;

//
// IDL:orb.yoko.apache.org/OAD/AlreadyLinked:1.0
//
final public class AlreadyLinkedHelper
{
    public static void
    insert(Any any, AlreadyLinked val)
    {
        OutputStream out = any.create_output_stream();
        write(out, val);
        any.read_value(out.create_input_stream(), type());
    }

    public static AlreadyLinked
    extract(Any any)
    {
        if(any.type().equivalent(type()))
            return read(any.create_input_stream());
        else
            throw new BAD_OPERATION(
                MinorCodes
                        .describeBadOperation(MinorTypeMismatch),
                MinorTypeMismatch, COMPLETED_NO);
    }

    private static TypeCode typeCode_;

    public static TypeCode
    type()
    {
        if(typeCode_ == null)
        {
            ORB orb = ORB.init();
            typeCode_ = orb.create_exception_tc(id(), "AlreadyLinked", emptyArray(StructMember.class));
        }

        return typeCode_;
    }

    public static String
    id()
    {
        return "IDL:orb.yoko.apache.org/OAD/AlreadyLinked:1.0";
    }

    public static AlreadyLinked
    read(InputStream in)
    {
        if(!id().equals(in.read_string())) {
            throw new MARSHAL(
                MinorCodes
                    .describeMarshal(MinorReadIDMismatch),
                MinorReadIDMismatch,
                COMPLETED_NO);
        }

        AlreadyLinked _ob_v = new AlreadyLinked();
        return _ob_v;
    }

    public static void
    write(OutputStream out, AlreadyLinked val)
    {
        out.write_string(id());
    }
}
