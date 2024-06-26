/*
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
package org.omg.CORBA.TypeCodePackage;

final public class BoundsHelper {
    public static void insert(org.omg.CORBA.Any any,
            org.omg.CORBA.TypeCodePackage.Bounds value) {
        throw new org.omg.CORBA.MARSHAL();
    }

    public static org.omg.CORBA.TypeCodePackage.Bounds extract(
            org.omg.CORBA.Any any) {
        throw new org.omg.CORBA.MARSHAL();
    }

    private static org.omg.CORBA.TypeCode typeCode_;

    public static org.omg.CORBA.TypeCode type() {
        if (typeCode_ == null) {
            org.omg.CORBA.ORB orb = org.omg.CORBA.ORB.init();
            org.omg.CORBA.StructMember[] members = new org.omg.CORBA.StructMember[0];
            typeCode_ = orb.create_exception_tc(id(), "Bounds", members);
        }

        return typeCode_;
    }

    public static java.lang.String id() {
        return "IDL:omg.org/CORBA/TypeCode/Bounds:1.0";
    }

    public static org.omg.CORBA.TypeCodePackage.Bounds read(
            org.omg.CORBA.portable.InputStream input) {
        throw new org.omg.CORBA.MARSHAL();
    }

    public static void write(org.omg.CORBA.portable.OutputStream output,
            org.omg.CORBA.TypeCodePackage.Bounds value) {
        throw new org.omg.CORBA.MARSHAL();
    }
}
