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
package org.omg.CORBA;

//
// IDL:omg.org/CORBA/WrongTransaction:1.0
//
/***/

final public class WrongTransaction extends org.omg.CORBA.UserException
{
    private static final String _ob_id = "IDL:omg.org/CORBA/WrongTransaction:1.0";

    public
    WrongTransaction()
    {
        super(_ob_id);
    }

    public
    WrongTransaction(String _reason)
    {
        super(_ob_id + " " + _reason);
    }
}
