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
package testify.annotation.logging;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import testify.annotation.ExpectLog;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static org.junit.platform.commons.support.AnnotationSupport.findRepeatableAnnotations;

/**
 * Backs the {@link ExpectLog} annotation.
 *
 * <p>Before the test runs, attaches a capturing {@link Handler} to each named logger
 * referenced by the annotations on the test method.  After the test runs (whether it
 * passed or threw), asserts that the captured records satisfy every {@code @ExpectLog}
 * expectation, then removes the handler.
 *
 * <p>Assertions are made even when the test body threw — this lets
 * {@code @ExpectLog} compose with {@code assertThrows}.
 */
public final class LogExpectationExtension implements BeforeTestExecutionCallback, AfterTestExecutionCallback {

    private static final Namespace NS = Namespace.create(LogExpectationExtension.class);
    private static final String KEY = "capturingHandler";

    @Override
    public void beforeTestExecution(ExtensionContext ctx) {
        List<ExpectLog> annotations = findRepeatableAnnotations(ctx.getRequiredTestMethod(), ExpectLog.class);
        if (annotations.isEmpty()) return;

        CapturingHandler handler = new CapturingHandler();

        // Attach to every distinct logger named by the annotations.
        // The root logger ("") receives records from all loggers, so attaching
        // there is enough when the logger name is left at its default.
        annotations.stream()
                .map(ExpectLog::logger)
                .distinct()
                .forEach(name -> {
                    Logger logger = Logger.getLogger(name);
                    logger.addHandler(handler);
                    // Ensure the logger itself isn't filtering the records out
                    if (logger.getLevel() == null || logger.getLevel().intValue() > Level.ALL.intValue()) {
                        logger.setLevel(Level.ALL);
                    }
                });

        ctx.getStore(NS).put(KEY, handler);
    }

    @Override
    public void afterTestExecution(ExtensionContext ctx) {
        CapturingHandler handler = ctx.getStore(NS).remove(KEY, CapturingHandler.class);
        if (handler == null) return;

        List<ExpectLog> annotations = findRepeatableAnnotations(ctx.getRequiredTestMethod(), ExpectLog.class);

        // Detach before asserting so that assertion failures don't add spurious records
        annotations.stream()
                .map(ExpectLog::logger)
                .distinct()
                .map(Logger::getLogger)
                .forEach(logger -> logger.removeHandler(handler));

        List<LogRecord> records = handler.records;

        for (ExpectLog expectation : annotations) {
            Pattern pattern = Pattern.compile(expectation.value());
            ExpectLog.LogLevel expectedLevel = expectation.level();

            boolean matched = records.stream().anyMatch(record -> {
                if (expectedLevel != ExpectLog.LogLevel.ANY && !record.getLevel().equals(expectedLevel.level))
                    return false;
                String message = record.getMessage();
                return message != null && pattern.matcher(message).find();
            });

            if (!matched) {
                String description = expectedLevel == ExpectLog.LogLevel.ANY
                        ? "matching /" + expectation.value() + "/"
                        : "at level " + expectedLevel + " matching /" + expectation.value() + "/";
                throw new AssertionError("Expected a log record " + description
                        + " but none was found among " + records.size() + " captured record(s)."
                        + (records.isEmpty() ? "" : " Messages were:\n" + formatRecords(records)));
            }
        }
    }

    private static String formatRecords(List<LogRecord> records) {
        StringBuilder sb = new StringBuilder();
        for (LogRecord r : records) {
            sb.append("  [").append(r.getLevel()).append("] ").append(r.getMessage()).append('\n');
        }
        return sb.toString();
    }

    private static final class CapturingHandler extends Handler {
        final List<LogRecord> records = new ArrayList<>();

        @Override
        public void publish(LogRecord record) { records.add(record); }

        @Override
        public void flush() {}

        @Override
        public void close() {}
    }
}
