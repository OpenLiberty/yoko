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

public abstract class Context {
    public abstract String context_name();

    public abstract Context parent();

    public abstract Context create_child(String child_ctx_name);

    public abstract void set_one_value(String propname, Any propvalue);

    public abstract void set_values(NVList values);

    public abstract void delete_values(String propname);

    public abstract NVList get_values(String start_scope, int op_flags,
            String pattern);
}
