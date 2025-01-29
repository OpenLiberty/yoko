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
// IDL:orb.yoko.apache.org/OBPortableInterceptor/ORBInitInfo:1.0
//

import org.omg.CORBA.ORB;

/**
 *
 * This interface is a proprietary extension to the standard ORBInitInfo.
 *
 * @see PortableInterceptor::ORBInitInfo
 *
 **/

public interface ORBInitInfoOperations extends org.omg.PortableInterceptor.ORBInitInfoOperations
{
    //
    // IDL:orb.yoko.apache.org/OBPortableInterceptor/ORBInitInfo/orb:1.0
    //
    /** This ORB. */

    ORB
    orb();
}
