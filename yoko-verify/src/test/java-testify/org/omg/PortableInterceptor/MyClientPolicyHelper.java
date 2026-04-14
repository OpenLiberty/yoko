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
package org.omg.PortableInterceptor;

import org.omg.CORBA.Any;
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.TypeCode;

public final class MyClientPolicyHelper {
    public static void insert(Any any, MyClientPolicy val)
    {
        any.insert_Object(val, type());
    }

    private static TypeCode typeCode_;

    public static TypeCode type() {
        if(typeCode_ == null) {
            org.omg.CORBA.ORB orb = org.omg.CORBA.ORB.init();
            typeCode_ = ((org.omg.CORBA_2_4.ORB)orb).create_local_interface_tc(id(), "MyClientPolicy");
        }
        return typeCode_;
    }

    public static String id() { return "IDL:MyClientPolicy:1.0"; }

    public static MyClientPolicy narrow(org.omg.CORBA.Object val) {
        try {
            return (MyClientPolicy)val;
        } catch(ClassCastException ex) {
            throw new BAD_PARAM();
        }
    }
}
