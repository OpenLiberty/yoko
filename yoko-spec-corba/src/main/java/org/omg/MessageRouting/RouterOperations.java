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
package org.omg.MessageRouting;

//
// IDL:omg.org/MessageRouting/Router:1.0
//
/***/

public interface RouterOperations
{
    //
    // IDL:omg.org/MessageRouting/Router/send_request:1.0
    //
    /***/

    void
    send_request(RequestInfo req);

    //
    // IDL:omg.org/MessageRouting/Router/send_multiple_requests:1.0
    //
    /***/

    void
    send_multiple_requests(RequestInfo[] reqSeq);

    //
    // IDL:omg.org/MessageRouting/Router/admin:1.0
    //
    /***/

    RouterAdmin
    admin();
}
