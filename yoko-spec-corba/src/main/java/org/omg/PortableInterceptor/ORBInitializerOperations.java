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
package org.omg.PortableInterceptor;

//
// IDL:omg.org/PortableInterceptor/ORBInitializer:1.0
//
/***/

public interface ORBInitializerOperations
{
    //
    // IDL:omg.org/PortableInterceptor/ORBInitializer/pre_init:1.0
    //
    /***/

    void
    pre_init(ORBInitInfo info);

    //
    // IDL:omg.org/PortableInterceptor/ORBInitializer/post_init:1.0
    //
    /***/

    void
    post_init(ORBInitInfo info);
}
