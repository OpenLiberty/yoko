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

public interface Object {
    boolean _is_a(String identifier);

    boolean _is_equivalent(org.omg.CORBA.Object that);

    boolean _non_existent();

    int _hash(int maximum);

    org.omg.CORBA.Object _duplicate();

    void _release();

    /**
     * @deprecated Deprecated by CORBA 2.3.
     */
    InterfaceDef _get_interface();

    org.omg.CORBA.Object _get_interface_def();

    Request _request(String s);

    Request _create_request(Context ctx, String operation, NVList arg_list,
            NamedValue result);

    Request _create_request(Context ctx, String operation, NVList arg_list,
            NamedValue result, ExceptionList exclist, ContextList ctxlist);

    org.omg.CORBA.Policy _get_policy(int policy_type);

    org.omg.CORBA.Object _set_policy_override(org.omg.CORBA.Policy[] policies,
            org.omg.CORBA.SetOverrideType set_add);

    org.omg.CORBA.DomainManager[] _get_domain_managers();
}
