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

//
// IDL:TestInterface:1.0
//

import org.omg.CORBA.StringHolder;
import org.omg.PortableInterceptor.TestInterfacePackage.s;
import org.omg.PortableInterceptor.TestInterfacePackage.sHolder;
import org.omg.PortableInterceptor.TestInterfacePackage.user;

public interface TestInterfaceOperations {
    void noargs();
    void noargs_oneway();
    void systemexception();
    void userexception() throws user;
    void location_forward();
    void test_service_context();
    String string_attrib();
    void string_attrib(String val);
    void one_string_in(String param);
    void one_string_inout(StringHolder param);
    void one_string_out(StringHolder param);
    String one_string_return();
    s struct_attrib();
    void struct_attrib(s val);
    void one_struct_in(s param);
    void one_struct_inout(sHolder param);
    void one_struct_out(sHolder param);
    s one_struct_return();
}
