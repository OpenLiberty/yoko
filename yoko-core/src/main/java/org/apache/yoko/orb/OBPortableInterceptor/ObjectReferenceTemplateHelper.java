/*
 * Copyright 2021 IBM Corporation and others.
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
package org.apache.yoko.orb.OBPortableInterceptor;

import org.apache.yoko.util.MinorCodes;

//
// IDL:orb.yoko.apache.org/OBPortableInterceptor/ObjectReferenceTemplate:1.0
//
final public class ObjectReferenceTemplateHelper
{
    public static void
    insert(org.omg.CORBA.Any any, ObjectReferenceTemplate val)
    {
        any.insert_Value(val, type());
    }

    public static ObjectReferenceTemplate
    extract(org.omg.CORBA.Any any)
    {
        if(any.type().equivalent(type()))
        {
            java.io.Serializable _ob_v = any.extract_Value();
            if(_ob_v == null || _ob_v instanceof ObjectReferenceTemplate)
                return (ObjectReferenceTemplate)_ob_v;
        }


        throw new org.omg.CORBA.BAD_OPERATION(
            MinorCodes
                    .describeBadOperation(MinorCodes.MinorTypeMismatch),
            MinorCodes.MinorTypeMismatch, org.omg.CORBA.CompletionStatus.COMPLETED_NO);
    }

    private static org.omg.CORBA.TypeCode typeCode_;

    public static org.omg.CORBA.TypeCode
    type()
    {
        if(typeCode_ == null)
        {
            org.omg.CORBA.ORB orb = org.omg.CORBA.ORB.init();
            org.omg.CORBA.ValueMember[] members = new org.omg.CORBA.ValueMember[0];

            typeCode_ = orb.create_value_tc(id(), "ObjectReferenceTemplate", org.omg.CORBA.VM_ABSTRACT.value, null, members);
        }

        return typeCode_;
    }

    public static String
    id()
    {
        return "IDL:orb.yoko.apache.org/OBPortableInterceptor/ObjectReferenceTemplate:1.0";
    }

    public static ObjectReferenceTemplate
    read(org.omg.CORBA.portable.InputStream in)
    {
        if(!(in instanceof org.omg.CORBA_2_3.portable.InputStream)) {
            throw new org.omg.CORBA.BAD_PARAM(MinorCodes
                .describeBadParam(MinorCodes.MinorIncompatibleObjectType),
                MinorCodes.MinorIncompatibleObjectType,
                org.omg.CORBA.CompletionStatus.COMPLETED_NO);
        }
        return (ObjectReferenceTemplate)((org.omg.CORBA_2_3.portable.InputStream)in).read_value(id());
    }

    public static void
    write(org.omg.CORBA.portable.OutputStream out, ObjectReferenceTemplate val)
    {
        if(!(out instanceof org.omg.CORBA_2_3.portable.OutputStream)) {
            throw new org.omg.CORBA.BAD_PARAM(MinorCodes
                .describeBadParam(MinorCodes.MinorIncompatibleObjectType),
                MinorCodes.MinorIncompatibleObjectType,
                org.omg.CORBA.CompletionStatus.COMPLETED_NO);
        }
        ((org.omg.CORBA_2_3.portable.OutputStream)out).write_value(val, id());
    }
}
