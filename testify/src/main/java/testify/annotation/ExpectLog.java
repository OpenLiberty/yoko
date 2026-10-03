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
package testify.annotation;

import org.junit.jupiter.api.extension.ExtendWith;
import testify.annotation.logging.LogExpectationExtension;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;

/**
 * Asserts that at least one log record emitted during the test matches the given
 * regular expression, optionally restricted to a specific log level and/or logger.
 *
 * <p>The assertion is evaluated after the test method returns (whether or not it
 * threw an exception), so it composes naturally with {@code assertThrows}.
 *
 * <p>Example:
 * <pre>
 * {@literal @}Test
 * {@literal @}ExpectLog(value = "--add-opens=java.base/java.util.concurrent", level = SEVERE)
 * void myTest() { ... }
 * </pre>
 *
 * <p>Multiple expectations can be stacked:
 * <pre>
 * {@literal @}Test
 * {@literal @}ExpectLog("first pattern")
 * {@literal @}ExpectLog(value = "second pattern", level = WARNING)
 * void myTest() { ... }
 * </pre>
 */
@ExtendWith(LogExpectationExtension.class)
@Target(METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ExpectLog.Container.class)
public @interface ExpectLog {
    /**
     * A regular expression that must match the message of at least one log record
     * emitted during the test.
     */
    String value();

    /**
     * If specified, only log records at this level are considered.
     * Defaults to {@link LogLevel#ANY}, which matches all levels.
     */
    LogLevel level() default LogLevel.ANY;

    /**
     * The name of the logger to observe. Defaults to {@code ""} (the root logger),
     * which captures records from all loggers.
     */
    String logger() default "";

    /**
     * Log levels usable in annotations (mirrors {@link java.util.logging.Level} values,
     * plus {@link #ANY} to match all levels).
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
        ExpectLog[] value();
    }
}
