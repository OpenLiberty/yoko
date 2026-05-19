/*
 * Copyright 2026 IBM Corporation and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package testify.iiop.annotation;

import org.junit.jupiter.api.Tag;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static testify.iiop.annotation.ConfigureServer.Separation.INTER_PROCESS;

/**
 * Marks a test as an interop test that requires a specific Yoko version.
 * Automatically configures the server to run in a separate process with the specified Yoko version.
 *
 * Tests marked with this annotation may be skipped if the required Yoko version is not cached.
 * To cache a version, run: ./gradlew buildYokoVersion -PyokoVersion=X.Y.Z
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Tag("interop")
@ConfigureServer(separation = INTER_PROCESS)
public @interface InteropTest {
    /**
     * The Yoko version to use for the server process.
     */
    YokoVersion value();

    enum YokoVersion {
        V1_5_0, V1_5_1, V1_5_2, V1_5_3, V1_6_0, V1_6_1;
        public final String version = name().substring(1).replace('_', '.');
    }
}
