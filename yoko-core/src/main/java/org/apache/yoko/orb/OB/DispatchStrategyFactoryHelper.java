/*==============================================================================
 * Copyright 2021 IBM Corporation and others.
 * 
 * Licensed under the Apache License, Version 2.0 (the License);
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
 *=============================================================================*/
package org.apache.yoko.orb.OB;

import org.apache.yoko.util.MinorCodes;

//
// IDL:orb.yoko.apache.org/OB/DispatchStrategyFactory:1.0
//
final public class DispatchStrategyFactoryHelper
{
    public static void
    insert(org.omg.CORBA.Any any, DispatchStrategyFactory val)
    {
        any.insert_Object(val, type());
    }

    public static DispatchStrategyFactory
    extract(org.omg.CORBA.Any any)
    {
        if(any.type().equivalent(type()))
            return narrow(any.extract_Object());


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
            typeCode_ = ((org.omg.CORBA_2_4.ORB)orb).create_local_interface_tc(id(), "DispatchStrategyFactory");
        }

        return typeCode_;
    }

    public static String
    id()
    {
        return "IDL:orb.yoko.apache.org/OB/DispatchStrategyFactory:1.0";
    }

    public static DispatchStrategyFactory
    read(org.omg.CORBA.portable.InputStream in)
    {
        throw new org.omg.CORBA.MARSHAL(
                MinorCodes
                        .describeMarshal(MinorCodes.MinorReadUnsupported),
                MinorCodes.MinorReadUnsupported,
                org.omg.CORBA.CompletionStatus.COMPLETED_NO);
    }

    public static void
    write(org.omg.CORBA.portable.OutputStream out, DispatchStrategyFactory val)
    {
        throw new org.omg.CORBA.MARSHAL(
                MinorCodes
                        .describeMarshal(MinorCodes.MinorWriteUnsupported),
                MinorCodes.MinorWriteUnsupported,
                org.omg.CORBA.CompletionStatus.COMPLETED_NO);
    }

    public static DispatchStrategyFactory
    narrow(org.omg.CORBA.Object val)
    {
        try
        {
            return (DispatchStrategyFactory)val;
        }
        catch(ClassCastException ex)
        {
        }

        throw new org.omg.CORBA.BAD_PARAM(MinorCodes
            .describeBadParam(MinorCodes.MinorIncompatibleObjectType),
            MinorCodes.MinorIncompatibleObjectType,
            org.omg.CORBA.CompletionStatus.COMPLETED_NO);
    }
}
