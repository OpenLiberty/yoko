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

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import testify.annotation.logging.LogPublisher;
import testify.annotation.logging.LogSetting;
import testify.annotation.logging.LoggingExtension;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.platform.commons.support.AnnotationSupport.findRepeatableAnnotations;

/**
 * Backs the {@link ExpectServerLog} annotation.
 *
 * <p>Before the test: collects all (logger, level) pairs from the annotations into a single
 * {@link LogSetting} list and pushes it via {@link LogPublisher#pushSettings}, enabling those
 * loggers in the server JVM.
 *
 * <p>After the test: pops the settings with one {@link LogPublisher#popSettings} call, retrieves
 * the raw {@link LogRecord}s from all recorders (including the server process), and asserts that
 * each {@link ExpectServerLog} expectation is satisfied.
 */
final class ExpectServerLogExtension implements BeforeTestExecutionCallback, AfterTestExecutionCallback {

    @Override
    public void beforeTestExecution(ExtensionContext ctx) {
        List<ExpectServerLog> annotations = findRepeatableAnnotations(ctx.getRequiredTestMethod(), ExpectServerLog.class);
        if (annotations.isEmpty()) return;

        LogPublisher publisher = LoggingExtension.findLogPublisher(ctx);
        if (publisher == null) return;

        List<LogSetting> settings = annotations.stream()
                .map(a -> new LogSetting(a.logger(), effectiveLevel(a.level())))
                .collect(Collectors.toList());
        publisher.pushSettings(settings);
    }

    @Override
    public void afterTestExecution(ExtensionContext ctx) {
        List<ExpectServerLog> annotations = findRepeatableAnnotations(ctx.getRequiredTestMethod(), ExpectServerLog.class);
        if (annotations.isEmpty()) return;

        LogPublisher publisher = LoggingExtension.findLogPublisher(ctx);
        if (publisher == null) return;

        publisher.popSettings();

        List<LogRecord> records = publisher.getLogRecords();

        for (ExpectServerLog expectation : annotations) {
            Pattern pattern = Pattern.compile(expectation.value());
            ExpectServerLog.LogLevel expectedLevel = expectation.level();

            boolean matched = records.stream().anyMatch(record -> {
                if (expectedLevel != ExpectServerLog.LogLevel.ANY && !record.getLevel().equals(expectedLevel.level))
                    return false;
                String message = record.getMessage();
                return message != null && pattern.matcher(message).find();
            });

            if (!matched) {
                String description = expectedLevel == ExpectServerLog.LogLevel.ANY
                        ? "matching /" + expectation.value() + "/"
                        : "at level " + expectedLevel + " matching /" + expectation.value() + "/";
                throw new AssertionError("Expected a server log record " + description
                        + " but none was found among " + records.size() + " captured record(s)."
                        + (records.isEmpty() ? "" : " Messages were:\n" + formatRecords(records)));
            }
        }
    }

    /** Returns the JUL level to use, defaulting to ALL for ANY. */
    private static Level effectiveLevel(ExpectServerLog.LogLevel logLevel) {
        return logLevel.level != null ? logLevel.level : Level.ALL;
    }

    private static String formatRecords(List<LogRecord> records) {
        StringBuilder sb = new StringBuilder();
        for (LogRecord r : records) {
            sb.append("  [").append(r.getLevel()).append("] ").append(r.getMessage()).append('\n');
        }
        return sb.toString();
    }
}
