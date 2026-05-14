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
package org.apache.yoko.util.concurrent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static java.util.stream.IntStream.range;
import static org.junit.jupiter.api.Assertions.*;

class LazyReferenceTest {
    private static final int INITIALIZATION_DELAY_MS = 50;
    private static final int EXPENSIVE_INITIALIZATION_DELAY_MS = 10;
    private static final int CONCURRENT_THREAD_COUNT = 10;
    private static final int HIGH_CONTENTION_THREAD_COUNT = 50;
    private static final int TEST_TIMEOUT_SECONDS = 10;
    private static final int EXECUTOR_SHUTDOWN_TIMEOUT_SECONDS = 5;
    private static final int SEQUENTIAL_ACCESS_COUNT = 100;

    @Test
    void testBasicInitialization() {
        AtomicInteger initCount = new AtomicInteger(0);
        LazyReference<String> ref = new LazyReference<>(() -> {
            initCount.incrementAndGet();
            return "initialized";
        });

        assertFalse(ref.isCompleted(), "Reference should not be initialized initially");

        String value = ref.get();
        assertEquals("initialized", value, "Should return initialized value");
        assertEquals(1, initCount.get(), "Initializer should be called exactly once");
        assertTrue(ref.isCompleted(), "Reference should be initialized after first get");

        // Second call should return cached value without re-initialization
        String value2 = ref.get();
        assertEquals("initialized", value2, "Should return same value");
        assertEquals(1, initCount.get(), "Initializer should still be called only once");
    }

    @Test
    void testConcurrentInitialization() throws InterruptedException {
        AtomicInteger initCount = new AtomicInteger(0);
        AtomicInteger maxConcurrentInits = new AtomicInteger(0);
        AtomicInteger currentConcurrentInits = new AtomicInteger(0);

        LazyReference<String> ref = new LazyReference<>(() -> {
            initCount.incrementAndGet();
            int concurrent = currentConcurrentInits.incrementAndGet();
            maxConcurrentInits.updateAndGet(max -> Math.max(max, concurrent));

            // Simulate some work
            try {
                Thread.sleep(INITIALIZATION_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            currentConcurrentInits.decrementAndGet();
            return "initialized";
        });

        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(CONCURRENT_THREAD_COUNT);
        final AtomicReference<Throwable> error = new AtomicReference<>();

        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREAD_COUNT);

        range(0, CONCURRENT_THREAD_COUNT).forEach(i -> {
            executor.submit(() -> {
                try {
                    // Wait for all threads to be ready
                    startLatch.await();

                    // All threads try to get the value simultaneously
                    String value = ref.get();
                    assertEquals("initialized", value, "All threads should get the same value");
                } catch (Throwable t) {
                    error.set(t);
                } finally {
                    doneLatch.countDown();
                }
            });
        });

        // Release all threads at once
        startLatch.countDown();

        // Wait for all threads to complete
        assertTrue(doneLatch.await(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS), "All threads should complete");

        executor.shutdown();
        assertTrue(executor.awaitTermination(EXECUTOR_SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS), "Executor should terminate");

        assertNull(error.get(), "No errors should occur");
        assertEquals(1, initCount.get(), "Initializer should be called exactly once despite concurrent access");
        assertEquals(1, maxConcurrentInits.get(), "Only one thread should be initializing at a time");
    }

    @RepeatedTest(10)
    void testHighContentionInitialization() throws InterruptedException {
        AtomicInteger initCount = new AtomicInteger(0);

        LazyReference<Integer> ref = new LazyReference<>(() -> {
            int count = initCount.incrementAndGet();
            // Simulate expensive initialization
            try {
                Thread.sleep(EXPENSIVE_INITIALIZATION_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return count;
        });

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(HIGH_CONTENTION_THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(HIGH_CONTENTION_THREAD_COUNT);

        for (int i = 0; i < HIGH_CONTENTION_THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    Integer value = ref.get();
                    assertEquals(1, value.intValue(), "All threads should get value 1");
                    successCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS), "All threads should complete");

        executor.shutdown();
        assertTrue(executor.awaitTermination(EXECUTOR_SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS), "Executor should terminate");

        assertEquals(HIGH_CONTENTION_THREAD_COUNT, successCount.get(), "All threads should succeed");
        assertEquals(1, initCount.get(), "Initializer should be called exactly once");
    }

    @Test
    void testInitializationWithException() {
        LazyReference<String> ref = new LazyReference<>(() -> {
            throw new RuntimeException("Initialization failed");
        });

        assertThrows(RuntimeException.class, ref::get, "Should propagate initialization exception");
    }

    @Test
    void testInitializationWithExceptionRetry() {
        AtomicInteger attemptCount = new AtomicInteger(0);
        LazyReference<String> ref = new LazyReference<>(() -> {
            int attempt = attemptCount.incrementAndGet();
            if (attempt == 1) {
                throw new RuntimeException("First attempt failed");
            }
            return "success-" + attempt;
        }, true); // Enable retry

        // First attempt should fail
        assertThrows(RuntimeException.class, ref::get, "First attempt should throw exception");
        assertFalse(ref.isCompleted(), "Reference should not be initialized after exception");

        // Second attempt should succeed
        String value = ref.get();
        assertEquals("success-2", value, "Second attempt should succeed");
        assertTrue(ref.isCompleted(), "Reference should be initialized after successful retry");
        assertEquals(2, attemptCount.get(), "Should have attempted twice");

        // Subsequent calls should return cached value
        assertEquals("success-2", ref.get(), "Should return cached value");
        assertEquals(2, attemptCount.get(), "Should not retry after success");
    }

    @Test
    void testInitializationWithExceptionNoRetry() {
        AtomicInteger attemptCount = new AtomicInteger(0);
        RuntimeException originalException = new RuntimeException("Initialization failed");
        LazyReference<String> ref = new LazyReference<>(() -> {
            attemptCount.incrementAndGet();
            throw originalException;
        }, false); // Disable retry (default behavior)

        // First attempt should fail with wrapped exception
        LazyReference.InitializationException firstException = 
            assertThrows(LazyReference.InitializationException.class, ref::get, 
                "First attempt should throw InitializationException");
        assertSame(originalException, firstException.getCause(), "Should wrap original exception");
        assertTrue(ref.isCompleted(), "Reference should be in error state after exception");
        assertEquals(1, attemptCount.get(), "Should have attempted once");

        // Second attempt should throw new wrapped exception with same cause, without retrying
        LazyReference.InitializationException secondException = 
            assertThrows(LazyReference.InitializationException.class, ref::get, 
                "Second attempt should throw wrapped exception");
        assertSame(originalException, secondException.getCause(), "Should wrap same original exception");
        assertEquals(1, attemptCount.get(), "Should not retry initialization");

        // Subsequent calls should continue throwing new wrapped exceptions with same cause
        LazyReference.InitializationException thirdException = 
            assertThrows(LazyReference.InitializationException.class, ref::get);
        assertSame(originalException, thirdException.getCause(), "Should still wrap same original exception");
        assertEquals(1, attemptCount.get(), "Should still not retry");
    }

    @Test
    void testInitializationWithErrorNoRetry() {
        AtomicInteger attemptCount = new AtomicInteger(0);
        OutOfMemoryError originalError = new OutOfMemoryError("Simulated OOM");
        LazyReference<String> ref = new LazyReference<>(() -> {
            attemptCount.incrementAndGet();
            throw originalError;
        }, false); // Disable retry

        // First attempt should fail with wrapped exception (even for Error)
        LazyReference.InitializationException firstException = 
            assertThrows(LazyReference.InitializationException.class, ref::get, 
                "First attempt should throw InitializationException");
        assertSame(originalError, firstException.getCause(), "Should wrap original Error");
        assertTrue(ref.isCompleted(), "Reference should be in error state after Error");
        assertEquals(1, attemptCount.get(), "Should have attempted once");

        // Second attempt should throw new wrapped exception with same cause, without retrying
        LazyReference.InitializationException secondException = 
            assertThrows(LazyReference.InitializationException.class, ref::get, 
                "Second attempt should throw wrapped exception");
        assertSame(originalError, secondException.getCause(), "Should wrap same original Error");
        assertEquals(1, attemptCount.get(), "Should not retry initialization");
    }

    @Test
    void testMultipleInstances() {
        AtomicInteger initCount = new AtomicInteger(0);
        Supplier<String> supplier = () -> {
            int count = initCount.incrementAndGet();
            return "initialized-" + count;
        };

        LazyReference<String> ref1 = new LazyReference<>(supplier);
        assertEquals("initialized-1", ref1.get());
        assertTrue(ref1.isCompleted());

        LazyReference<String> ref2 = new LazyReference<>(supplier);
        assertEquals("initialized-2", ref2.get());
        assertTrue(ref2.isCompleted());

        // Verify each reference maintains its own value
        assertEquals("initialized-1", ref1.get());
        assertEquals("initialized-2", ref2.get());
        assertEquals(2, initCount.get(), "Initializer should be called once per reference");
    }

    @Test
    void testNullValue() {
        LazyReference<String> ref = new LazyReference<>(() -> null);

        assertNull(ref.get(), "Should support null values");
        assertTrue(ref.isCompleted(), "Reference should be initialized even with null value");

        // Second call should still return null without re-initialization
        assertNull(ref.get(), "Should return null on subsequent calls");
    }

    @Test
    void testComplexObject() {
        class ComplexObject {
            final String name;
            final int value;

            ComplexObject(String name, int value) {
                this.name = name;
                this.value = value;
            }
        }

        AtomicInteger counter = new AtomicInteger(0);
        LazyReference<ComplexObject> ref = new LazyReference<>(() -> {
            counter.incrementAndGet();
            return new ComplexObject("test", 42);
        });

        ComplexObject obj1 = ref.get();
        ComplexObject obj2 = ref.get();

        assertSame(obj1, obj2, "Should return the same instance");
        assertEquals("test", obj1.name);
        assertEquals(42, obj1.value);
        assertEquals(1, counter.get(), "Should initialize only once");
    }

    @Test
    void testSequentialAccess() {
        AtomicInteger initCount = new AtomicInteger(0);
        LazyReference<String> ref = new LazyReference<>(() -> {
            initCount.incrementAndGet();
            return "value";
        });

        // Multiple sequential accesses
        for (int i = 0; i < SEQUENTIAL_ACCESS_COUNT; i++) {
            assertEquals("value", ref.get());
        }

        assertEquals(1, initCount.get(), "Should initialize only once despite many accesses");
    }
}
