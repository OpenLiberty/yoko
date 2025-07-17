/*
 * Copyright 2025 IBM Corporation and others.
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
package org.apache.yoko.orb.OCI;

import org.omg.CORBA.Any;
import org.omg.CORBA.BAD_OPERATION;
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.MARSHAL;
import org.omg.CORBA.ORB;
import org.omg.CORBA.TypeCode;
import org.omg.CORBA.portable.InputStream;
import org.omg.CORBA.portable.OutputStream;

import static org.apache.yoko.util.MinorCodes.MinorIncompatibleObjectType;
import static org.apache.yoko.util.MinorCodes.MinorReadUnsupported;
import static org.apache.yoko.util.MinorCodes.MinorTypeMismatch;
import static org.apache.yoko.util.MinorCodes.MinorWriteUnsupported;
import static org.apache.yoko.util.MinorCodes.describeBadOperation;
import static org.apache.yoko.util.MinorCodes.describeBadParam;
import static org.apache.yoko.util.MinorCodes.describeMarshal;
import static org.omg.CORBA.CompletionStatus.COMPLETED_NO;

//
// IDL:orb.yoko.apache.org/OCI/Current:1.0
//
final public class CurrentHelper
{
    public static void
    insert(Any any, Current val)
    {
        any.insert_Object(val, type());
    }

    public static Current
    extract(Any any)
    {
        if(any.type().equivalent(type()))
            return narrow(any.extract_Object());


        throw new BAD_OPERATION(
            describeBadOperation(MinorTypeMismatch),
            MinorTypeMismatch, COMPLETED_NO);
    }

    private static TypeCode typeCode_;

    public static TypeCode
    type()
    {
        if(typeCode_ == null)
        {
            ORB orb = ORB.init();
            typeCode_ = ((org.omg.CORBA_2_4.ORB)orb).create_local_interface_tc(id(), "Current");
        }

        return typeCode_;
    }

    public static String
    id()
    {
        return "IDL:orb.yoko.apache.org/OCI/Current:1.0";
    }

    public static Current
    read(InputStream in)
    {
        throw new MARSHAL(
                describeMarshal(MinorReadUnsupported),
                MinorReadUnsupported,
                COMPLETED_NO);
    }

    public static void
    write(OutputStream out, Current val)
    {
        throw new MARSHAL(
                describeMarshal(MinorWriteUnsupported),
                MinorWriteUnsupported,
                COMPLETED_NO);
    }

    public static Current
    narrow(org.omg.CORBA.Object val)
    {
        try
        {
            return (Current)val;
        }
        catch(ClassCastException ex)
        {
        }

        throw new BAD_PARAM(describeBadParam(MinorIncompatibleObjectType),
            MinorIncompatibleObjectType,
            COMPLETED_NO);
    }
}
