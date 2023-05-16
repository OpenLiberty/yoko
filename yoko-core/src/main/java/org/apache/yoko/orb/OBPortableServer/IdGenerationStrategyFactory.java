/*==============================================================================
 * Copyright 2010 IBM Corporation and others.
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
package org.apache.yoko.orb.OBPortableServer;

//
// This id generation strategy is for user ids, hence it doesn't do
// much
//
class UserIdGenerationStrategy implements IdGenerationStrategy {
    public byte[] createId()
            throws org.omg.PortableServer.POAPackage.WrongPolicy {
        throw new org.omg.PortableServer.POAPackage.WrongPolicy();
    }

    public boolean isValid(byte[] oid) {
        return true;
    }
}

//
// Create the appropriate id generation strategy
//
public class IdGenerationStrategyFactory {
    public static IdGenerationStrategy createIdGenerationStrategy(
            POAPolicies policies) {
        if (policies.idAssignmentPolicy() == org.omg.PortableServer.IdAssignmentPolicyValue.SYSTEM_ID)
            return new SystemIdGenerationStrategy(
                    policies.lifespanPolicy() == org.omg.PortableServer.LifespanPolicyValue.PERSISTENT);

        return new UserIdGenerationStrategy();
    }
}
