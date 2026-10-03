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

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;

/**
 * Asserts that at least one log record emitted by the <em>server</em> JVM during the test
 * matches the given regular expression, optionally restricted to a specific log level
 * and/or logger.
 *
 * <p>Unlike {@link testify.annotation.ExpectLog}, which checks in-process log records,
 * this annotation observes records captured by the testify logging bus from the server
 * process.  It also implies the corresponding {@link testify.annotation.Logging} setting:
 * the named logger will be enabled at the specified level in the server JVM automatically,
 * so no separate {@code @Logging} annotation is required.
 *
 * <p>Example:
 * <pre>
 * {@literal @}Test
 * {@literal @}EnabledForJreRange(min = JRE.JAVA_17)
 * {@literal @}ExpectServerLog(value = "ERROR: Yoko cannot reflectively access ",
 *                    level = SEVERE, logger = "org.apache.yoko.util.PrivilegedActions")
 * {@literal @}ExpectServerLog(value = "--add-opens=java.base/java.util.concurrent",
 *                    level = SEVERE, logger = "org.apache.yoko.util.PrivilegedActions")
 * public void testServerWithoutAddOpensFails(MapService stub) { ... }
 * </pre>
 */
@ExtendWith(ExpectServerLogExtension.class)
@Target(METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ExpectServerLog.Container.class)
public @interface ExpectServerLog {

    /**
     * A regular expression that must match the message of at least one log record
     * emitted by the server during the test.
     */
    String value();

    /**
     * If specified, only log records at this level are considered.
     * Defaults to {@link LogLevel#ANY}, which matches all levels.
     * Also used to configure the minimum level for the named logger in the server JVM.
     */
    LogLevel level() default LogLevel.ANY;

    /**
     * The name of the logger to observe (and to enable in the server JVM).
     * Defaults to {@code ""} (root logger).
     */
    String logger() default "";

    /**
     * Log levels usable in annotations.  Mirrors {@link java.util.logging.Level} values,
     * plus {@link #ANY} to match all levels.
     */
    enum LogLevel {
        ANY(null),
        SEVERE(java.util.logging.Level.SEVERE),
        WARNING(java.util.logging.Level.WARNING),
        INFO(java.util.logging.Level.INFO),
        CONFIG(java.util.logging.Level.CONFIG),
        FINE(java.util.logging.Level.FINE),
        FINER(java.util.logging.Level.FINER),
        FINEST(java.util.logging.Level.FINEST);

        /** The corresponding JUL level, or {@code null} for {@link #ANY}. */
        public final java.util.logging.Level level;
        LogLevel(java.util.logging.Level level) { this.level = level; }
    }

    @Target(METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Container {
        ExpectServerLog[] value();
    }
}
