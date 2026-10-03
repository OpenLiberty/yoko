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

import org.junit.jupiter.api.Test;
import org.junit.platform.launcher.TagFilter;
import org.junit.platform.testkit.engine.EngineTestKit;
import testify.annotation.ExpectLog;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.instanceOf;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.message;
import static testify.annotation.ExpectLog.LogLevel.INFO;
import static testify.annotation.ExpectLog.LogLevel.WARNING;

public class ExpectLogTest {

    private static final Logger LOG = Logger.getLogger(ExpectLogTest.class.getName());

    // --- passing cases ---

    @Test
    @ExpectLog("hello world")
    void matchesMessageByRegex() {
        LOG.info("hello world");
    }

    @Test
    @ExpectLog("hel+o")
    void matchesMessageByRegexPattern() {
        LOG.info("hello");
    }

    @Test
    @ExpectLog(value = "hello", level = INFO)
    void matchesMessageAndLevel() {
        LOG.info("hello");
    }

    @Test
    @ExpectLog(value = "hello", logger = "testify.annotation.logging.ExpectLogTest")
    void matchesOnNamedLogger() {
        LOG.info("hello");
    }

    @Test
    @ExpectLog("first")
    @ExpectLog("second")
    void matchesMultipleExpectations() {
        LOG.info("first message");
        LOG.info("second message");
    }

    @Test
    @ExpectLog(value = "important", level = WARNING)
    void ignoresRecordsAtWrongLevel() {
        LOG.info("important but at INFO");   // should be ignored for the WARNING expectation
        LOG.warning("important at WARNING"); // this one satisfies it
    }

    // --- failing cases (driven via EngineTestKit so failures don't escape) ---

    @Test
    void failsWhenNoRecordMatchesRegex() {
        long failures = EngineTestKit.engine("junit-jupiter")
                .selectors(selectClass(FailingFixtures.class))
                .filters(TagFilter.includeTags("no-match"))
                .execute()
                .testEvents()
                .failed()
                .count();
        assertEquals(1, failures);
    }

    @Test
    void failsWhenRecordIsAtWrongLevel() {
        long failures = EngineTestKit.engine("junit-jupiter")
                .selectors(selectClass(FailingFixtures.class))
                .filters(TagFilter.includeTags("wrong-level"))
                .execute()
                .testEvents()
                .failed()
                .count();
        assertEquals(1, failures);
    }

    @Test
    void failsWhenOneOfMultipleExpectationsIsUnsatisfied() {
        long failures = EngineTestKit.engine("junit-jupiter")
                .selectors(selectClass(FailingFixtures.class))
                .filters(TagFilter.includeTags("partial-match"))
                .execute()
                .testEvents()
                .failed()
                .count();
        assertEquals(1, failures);
    }

    @Test
    void failureMessageContainsExpectedPattern() {
        EngineTestKit.engine("junit-jupiter")
                .selectors(selectClass(FailingFixtures.class))
                .filters(TagFilter.includeTags("no-match"))
                .execute()
                .testEvents()
                .assertThatEvents()
                .haveExactly(1, finishedWithFailure(
                        instanceOf(AssertionError.class),
                        message(m -> m.contains("will-never-appear"))));
    }

    /**
     * Fixture class containing tests that are expected to fail.
     *
     * <p>This class has no {@code @Nested} annotation. JUnit only discovers
     * inner classes for execution when they carry {@code @Nested}; without it
     * this class is ignored by the normal discovery engine. It can still be
     * targeted explicitly by {@link EngineTestKit#selectors} using a class
     * selector, which is how the tests above drive it.
     */
    static class FailingFixtures {

        private static final Logger LOG = Logger.getLogger(FailingFixtures.class.getName());

        @Test
        @org.junit.jupiter.api.Tag("no-match")
        @ExpectLog("will-never-appear")
        void noRecordMatchesRegex() {
            LOG.info("something completely different");
        }

        @Test
        @org.junit.jupiter.api.Tag("wrong-level")
        @ExpectLog(value = "hello", level = WARNING)
        void recordIsAtWrongLevel() {
            LOG.info("hello"); // INFO, but WARNING is required
        }

        @Test
        @org.junit.jupiter.api.Tag("partial-match")
        @ExpectLog("first")
        @ExpectLog("second-will-not-appear")
        void oneOfMultipleExpectationsIsUnsatisfied() {
            LOG.info("first message");
            // "second-will-not-appear" is never logged
        }
    }
}
