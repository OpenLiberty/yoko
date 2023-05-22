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
package testify.util.function;

import org.opentest4j.AssertionFailedError;

@FunctionalInterface
public interface RawFunction<T, R> extends java.util.function.Function<T, R> {
    R applyRaw(T t) throws Exception;

    default R apply(T t) {
        try {
            return applyRaw(t);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new AssertionFailedError("", e);
        }

    }

    default RawSupplier<R> curry(T t) {
        return () -> applyRaw(t);
    }
}
