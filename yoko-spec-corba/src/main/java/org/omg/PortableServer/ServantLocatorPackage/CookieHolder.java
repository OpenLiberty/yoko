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
package org.omg.PortableServer.ServantLocatorPackage;

final public class CookieHolder implements org.omg.CORBA.portable.Streamable {
    public java.lang.Object value;

    public CookieHolder() {
    }

    public CookieHolder(java.lang.Object initial) {
        value = initial;
    }

    public void _read(org.omg.CORBA.portable.InputStream in) {
        throw new org.omg.CORBA.MARSHAL();
    }

    public void _write(org.omg.CORBA.portable.OutputStream out) {
        throw new org.omg.CORBA.MARSHAL();
    }

    public org.omg.CORBA.TypeCode _type() {
        return org.omg.CORBA.ORB.init().create_native_tc(
                "IDL:org.omg/PortableServer/ServantLocator/Cookie:1.0",
                "Cookie");
    }
}
