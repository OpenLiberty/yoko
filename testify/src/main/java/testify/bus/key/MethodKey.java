/*
 * Copyright 2023 IBM Corporation and others.
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
package testify.bus.key;

import java.lang.reflect.Method;

import static testify.bus.key.MemberKey.memberToString;
import static testify.bus.key.MemberKey.stringToMember;

/**
 * A specialised type spec that handles {@link Method} objects.
 */
public interface MethodKey extends TypeKey<Method> {
    @Override
    default String stringify(Method method) { return memberToString(method); }
    @Override
    default Method unstringify(String s) { return (Method) stringToMember(s); }
}
