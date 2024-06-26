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
// IDL:omg.org/CORBA/LocalInterfaceDef:1.0
//
/***/

public interface LocalInterfaceDefOperations extends InterfaceDefOperations
{
    //
    // IDL:omg.org/CORBA/LocalInterfaceDef/_OB_create_operation:1.0
    //
    /***/

    OperationDef
    _OB_create_operation(String id,
                         String name,
                         String version,
                         IDLType result,
                         OperationMode mode,
                         ParameterDescription[] params,
                         ExceptionDef[] exceptions,
                         NativeDef[] native_exceptions,
                         String[] contexts);
}
