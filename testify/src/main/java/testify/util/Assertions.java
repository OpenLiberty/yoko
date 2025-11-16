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
package testify.util;

import org.junit.jupiter.api.function.Executable;
import org.opentest4j.AssertionFailedError;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;

public enum Assertions {
    ;
    public static AssertionFailedError failf(String format, Object... params) {
        throw new AssertionFailedError(String.format(format, params));
    }

    public static void assertThrows(Class<? extends Throwable> expected, Executable executable, Class<? extends Throwable>... causalTypeChain) {
         Throwable t = org.junit.jupiter.api.Assertions.assertThrows(expected, executable);
         for (Class<? extends Throwable> causeType: causalTypeChain) {
             t = t.getCause();
             if (causeType.isInstance(t)) continue;
             fail("Expected caused by " + causeType + " but was caused by " + t, t);
         }
    }

    public static void assertThrowsExactly(Class<? extends Throwable> expected, Executable executable, Class<? extends Throwable>... causalTypeChain) {
        Throwable t = org.junit.jupiter.api.Assertions.assertThrowsExactly(expected, executable);
        for (Class<? extends Throwable> causeType: causalTypeChain) {
            t = t.getCause();
            if (causeType == notNull(t).getClass()) continue;
            fail("Expected caused by exactly " + causeType + " but was caused by " + t, t);
        }
    }

    private static <T> T notNull(T t) {
        assertNotNull(t);
        return t;
    }
}
