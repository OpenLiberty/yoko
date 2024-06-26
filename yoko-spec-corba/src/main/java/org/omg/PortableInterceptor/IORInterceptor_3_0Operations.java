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
// IDL:omg.org/PortableInterceptor/IORInterceptor_3_0:1.0
//
/***/

public interface IORInterceptor_3_0Operations extends IORInterceptorOperations
{
    //
    // IDL:omg.org/PortableInterceptor/IORInterceptor_3_0/components_established:1.0
    //
    /***/

    void
    components_established(IORInfo info);

    //
    // IDL:omg.org/PortableInterceptor/IORInterceptor_3_0/adapter_manager_state_changed:1.0
    //
    /***/

    void
    adapter_manager_state_changed(String id,
                                  short state);

    //
    // IDL:omg.org/PortableInterceptor/IORInterceptor_3_0/adapter_state_changed:1.0
    //
    /***/

    void
    adapter_state_changed(ObjectReferenceTemplate[] templates,
                          short state);
}
