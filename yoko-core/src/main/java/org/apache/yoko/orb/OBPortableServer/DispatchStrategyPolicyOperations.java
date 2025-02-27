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
package org.apache.yoko.orb.OBPortableServer;

//
// IDL:orb.yoko.apache.org/OBPortableServer/DispatchStrategyPolicy:1.0
//

import org.apache.yoko.orb.OB.DispatchStrategy;
import org.omg.CORBA.PolicyOperations;

/**
 *
 * The dispatch strategy policy. This policy controls the method
 * dispatch strategy.
 *
 * @see OB::DispatchStrategy
 *
 **/

public interface DispatchStrategyPolicyOperations extends PolicyOperations
{
    //
    // IDL:orb.yoko.apache.org/OBPortableServer/DispatchStrategyPolicy/value:1.0
    //
    /**
     *
     * The DispatchStrategy value.
     *
     **/

    DispatchStrategy
    value();
}
