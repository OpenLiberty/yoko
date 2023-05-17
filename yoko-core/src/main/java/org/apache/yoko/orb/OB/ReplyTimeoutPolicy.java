/*==============================================================================
 * Copyright 2018 IBM Corporation and others.
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
package org.apache.yoko.orb.OB;

//
// IDL:orb.yoko.apache.org/OB/RequestTimeoutPolicy:1.0
//

import org.omg.CORBA.Policy;

/**
 *
 * The reply timeout policy. This policy can be used to specify
 * a maximum time limit for replies.
 *
 * @see TimeoutPolicy
 *
 **/

public interface ReplyTimeoutPolicy extends ReplyTimeoutPolicyOperations, Policy {}
