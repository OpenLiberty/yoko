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
package org.apache.yoko.orb.OBPortableInterceptor;

//
// IDL:orb.yoko.apache.org/OBPortableInterceptor/ObjectReferenceTemplate:1.0
//

import org.omg.CORBA.portable.ValueBase;

/***/

public interface ObjectReferenceTemplate extends ValueBase,
                                                 org.omg.PortableInterceptor.ObjectReferenceTemplate
{
    //
    // IDL:orb.yoko.apache.org/OBPortableInterceptor/ObjectReferenceTemplate/make_object_for:1.0
    //
    /***/

    org.omg.CORBA.Object
    make_object_for(String repository_id,
                    byte[] id,
                    String[] name);
}
