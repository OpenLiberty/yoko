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

    /**
     * Tests basic lazy initialization behavior.
     * Verifies that the initializer is called exactly once on first access
     * and subsequent calls return the cached value.
     */
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

    /**
     * Tests thread-safe initialization under concurrent access.
     * Verifies that only one thread performs initialization even when
     * multiple threads attempt to access the value simultaneously.
     */
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

    /**
     * Tests initialization under high contention with many concurrent threads.
     * Repeated 10 times to catch potential race conditions.
     * Verifies that initialization happens exactly once despite high contention.
     */
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

    /**
     * Tests that initialization exceptions are propagated to the caller.
     * Verifies that exceptions thrown during initialization are not swallowed.
     */
    @Test
    void testInitializationWithException() {
        LazyReference<String> ref = new LazyReference<>(() -> {
            throw new RuntimeException("Initialization failed");
        });

        assertThrows(RuntimeException.class, ref::get, "Should propagate initialization exception");
    }

    /**
     * Tests retry behavior when initialization fails.
     * Verifies that with retry enabled, failed initialization can be retried
     * on subsequent calls, and successful initialization is cached.
     */
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

    /**
     * Tests permanent error state when retry is disabled.
     * Verifies that when initialization fails and retry is disabled,
     * all subsequent calls throw InitializationException without retrying,
     * and each exception wraps the same original cause.
     */
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

    /**
     * Tests that Error instances are handled the same as exceptions.
     * Verifies that even Error subclasses (like OutOfMemoryError) are wrapped
     * in InitializationException and result in permanent error state when retry is disabled.
     */
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

    /**
     * Tests that multiple LazyReference instances are independent.
     * Verifies that each instance maintains its own initialization state
     * and cached value.
     */
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

    /**
     * Tests that null values are properly supported.
     * Verifies that null can be returned from initialization and is cached
     * like any other value.
     */
    @Test
    void testNullValue() {
        LazyReference<String> ref = new LazyReference<>(() -> null);

        assertNull(ref.get(), "Should support null values");
        assertTrue(ref.isCompleted(), "Reference should be initialized even with null value");

        // Second call should still return null without re-initialization
        assertNull(ref.get(), "Should return null on subsequent calls");
    }

    /**
     * Tests lazy initialization with complex objects.
     * Verifies that the same object instance is returned on all calls
     * and initialization happens only once.
     */
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

    /**
     * Tests that sequential access doesn't trigger re-initialization.
     * Verifies that many sequential calls to get() only initialize once.
     */
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

    /**
     * Tests detection of recursive initialization without placeholder support.
     * Verifies that when no placeholder generator is provided, recursive calls
     * during initialization throw IllegalStateException with RecursiveInitializationException
     * as the cause, and the reference enters a permanent error state.
     */
    @Test
    void testRecursiveInitializationDetection() {
        // Use array to work around Java's effectively final requirement
        LazyReference<String>[] refHolder = new LazyReference[1];

        // Create a LazyReference that tries to call get() during initialization
        refHolder[0] = new LazyReference<>(() -> {
            // This should throw IllegalStateException due to recursive call
            return refHolder[0].get();
        });

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            refHolder[0]::get,
            "Should detect recursive initialization"
        );

        assertEquals("Initialization failed due to recursive call", exception.getMessage(),
            "Exception message should indicate recursive initialization failure");

        assertNotNull(exception.getCause(), "Should have a cause");
        assertTrue(exception.getCause() instanceof LazyReference.RecursiveInitializationException,
            "Cause should be RecursiveInitializationException");
        assertTrue(
            exception.getCause().getMessage().contains("Recursive initialization detected"),
            "Cause message should indicate recursive initialization"
        );
        assertTrue(
            exception.getCause().getMessage().contains(Thread.currentThread().getName()),
            "Cause message should include thread name"
        );
    }

    /**
     * Tests recursive initialization with placeholder support.
     * Verifies that when a placeholder generator is provided, recursive calls
     * during initialization return the placeholder value instead of throwing an exception.
     * The placeholder is generated once and the final value incorporates it.
     */
    @Test
    void testRecursiveInitializationWithPlaceholder() {
        // Use array to work around Java's effectively final requirement
        LazyReference<String>[] refHolder = new LazyReference[1];
        AtomicInteger placeholderCallCount = new AtomicInteger(0);

        // Create a LazyReference with placeholder support
        refHolder[0] = new LazyReference<>(
            placeholderSupplier -> {
                // Recursive call should return placeholder instead of throwing
                String placeholder = refHolder[0].get();
                return "final-value-with-" + placeholder;
            },
            () -> {
                placeholderCallCount.incrementAndGet();
                return "placeholder";
            }
        );

        String result = refHolder[0].get();
        assertEquals("final-value-with-placeholder", result,
            "Should use placeholder during recursive initialization");
        assertEquals(1, placeholderCallCount.get(),
            "Placeholder generator should be called exactly once");
        assertTrue(refHolder[0].isCompleted(), "Reference should be initialized");

        // Subsequent calls should return the final value
        assertEquals("final-value-with-placeholder", refHolder[0].get(),
            "Should return cached final value");
        assertEquals(1, placeholderCallCount.get(),
            "Placeholder generator should not be called again");
    }

    /**
     * Tests recursive initialization with a custom placeholder value.
     * Verifies that custom placeholder values (non-String types) work correctly
     * and can be used in calculations during initialization.
     */
    @Test
    void testRecursiveInitializationWithCustomPlaceholder() {
        LazyReference<Integer>[] refHolder = new LazyReference[1];

        refHolder[0] = new LazyReference<>(
            placeholderSupplier -> {
                Integer placeholder = refHolder[0].get();
                // Use placeholder in calculation
                return placeholder * 10;
            },
            () -> 42  // Custom placeholder value
        );

        Integer result = refHolder[0].get();
        assertEquals(420, result, "Should use custom placeholder value");
    }

    /**
     * Tests that multiple recursive calls reuse the same placeholder.
     * Verifies that the placeholder generator is called only once even when
     * the initializer makes multiple recursive get() calls, and all recursive
     * calls return the same placeholder instance.
     */
    @Test
    void testMultipleRecursiveCallsReusePlaceholder() {
        LazyReference<String>[] refHolder = new LazyReference[1];
        AtomicInteger placeholderCallCount = new AtomicInteger(0);

        refHolder[0] = new LazyReference<>(
            placeholderSupplier -> {
                // Multiple recursive calls should reuse the same placeholder
                String first = refHolder[0].get();
                String second = refHolder[0].get();
                String third = refHolder[0].get();

                assertEquals(first, second, "Multiple recursive calls should return same placeholder");
                assertEquals(second, third, "Multiple recursive calls should return same placeholder");

                return "final-" + first;
            },
            () -> {
                placeholderCallCount.incrementAndGet();
                return "placeholder";
            }
        );

        String result = refHolder[0].get();
        assertEquals("final-placeholder", result);
        assertEquals(1, placeholderCallCount.get(),
            "Placeholder should be generated only once despite multiple recursive calls");
    }

    /**
     * Tests that null placeholder values are handled correctly.
     * Verifies that a placeholder generator can return null and the
     * initializer can distinguish between null and non-null placeholders.
     */
    @Test
    void testPlaceholderWithNullValue() {
        LazyReference<String>[] refHolder = new LazyReference[1];

        refHolder[0] = new LazyReference<>(
            placeholderSupplier -> {
                String placeholder = refHolder[0].get();
                return placeholder == null ? "null-placeholder" : "non-null-placeholder";
            },
            () -> null  // Null placeholder
        );

        String result = refHolder[0].get();
        assertEquals("null-placeholder", result, "Should handle null placeholder correctly");
    }

    /**
     * Tests that placeholder generator is not called without recursion.
     * Verifies that when initialization completes without any recursive calls,
     * the placeholder generator is never invoked, avoiding unnecessary work.
     */
    @Test
    void testPlaceholderNotCalledWithoutRecursion() {
        AtomicInteger placeholderCallCount = new AtomicInteger(0);
        AtomicInteger initCallCount = new AtomicInteger(0);

        LazyReference<String> ref = new LazyReference<>(
            placeholderSupplier -> {
                initCallCount.incrementAndGet();
                // No recursive call, so placeholder should not be generated
                return "normal-value";
            },
            () -> {
                placeholderCallCount.incrementAndGet();
                return "placeholder";
            }
        );

        String result = ref.get();
        assertEquals("normal-value", result);
        assertEquals(1, initCallCount.get(), "Initializer should be called once");
        assertEquals(0, placeholderCallCount.get(),
            "Placeholder generator should not be called without recursion");
    }

    /**
     * Tests thread safety when using placeholders with concurrent access.
     * Verifies that when one thread is performing initialization with recursive calls,
     * other threads correctly wait and receive the final value (not the placeholder).
     * Only the initializing thread should see the placeholder.
     */
    @Test
    void testConcurrentAccessWithPlaceholder() throws InterruptedException {
        LazyReference<String>[] refHolder = new LazyReference[1];
        AtomicInteger placeholderCallCount = new AtomicInteger(0);
        CountDownLatch recursiveLatch = new CountDownLatch(1);
        CountDownLatch waitLatch = new CountDownLatch(1);

        refHolder[0] = new LazyReference<>(
            placeholderSupplier -> {
                // Signal that we're in initialization
                recursiveLatch.countDown();

                // Make recursive call
                String placeholder = refHolder[0].get();

                // Wait a bit to allow other threads to attempt access
                try {
                    waitLatch.await(100, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                return "final-" + placeholder;
            },
            () -> {
                placeholderCallCount.incrementAndGet();
                return "placeholder";
            }
        );

        // Start initialization in background
        ExecutorService executor = Executors.newFixedThreadPool(2);
        AtomicReference<String> result1 = new AtomicReference<>();
        AtomicReference<String> result2 = new AtomicReference<>();

        executor.submit(() -> {
            result1.set(refHolder[0].get());
        });

        // Wait for initialization to start
        assertTrue(recursiveLatch.await(1, TimeUnit.SECONDS),
            "Initialization should start");

        // Try to access from another thread while initialization is in progress
        executor.submit(() -> {
            result2.set(refHolder[0].get());
        });

        // Release the initialization
        waitLatch.countDown();

        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS),
            "Executor should terminate");

        // Both threads should get the final value
        assertEquals("final-placeholder", result1.get());
        assertEquals("final-placeholder", result2.get());
        assertEquals(1, placeholderCallCount.get(),
            "Placeholder should be generated only once");
    }

    /**
     * Tests that the placeholder supplier is passed to the initializer function.
     * Verifies that the initializer receives a supplier that can provide placeholders,
     * even if it chooses not to use it. Outside of recursive context, the supplier
     * returns null since no placeholder was generated.
     */
    @Test
    void testPlaceholderSupplierAccessibleToInitializer() {
        AtomicReference<Supplier<String>> capturedSupplier = new AtomicReference<>();

        LazyReference<String> ref = new LazyReference<>(
            placeholderSupplier -> {
                // Capture the supplier for verification
                capturedSupplier.set(placeholderSupplier);
                // Don't actually call it to avoid recursion
                return "value";
            },
            () -> "placeholder"
        );

        ref.get();

        assertNotNull(capturedSupplier.get(),
            "Placeholder supplier should be passed to initializer");

        // Verify the supplier works (outside of initialization context)
        // Note: This will return null since we're not in a recursive context
        assertNull(capturedSupplier.get().get(),
            "Placeholder supplier should return null outside recursive context");
    }

    /**
     * Tests that placeholder is cleared after initialization completes.
     * Verifies that once initialization finishes, the placeholder is no longer
     * retrievable, even if a reference to the placeholder supplier was captured
     * during initialization. This ensures the placeholder's role ends with initialization.
     */
    @Test
    void testPlaceholderClearedAfterInitialization() {
        LazyReference<String>[] refHolder = new LazyReference[1];
        AtomicReference<Supplier<String>> capturedSupplier = new AtomicReference<>();

        refHolder[0] = new LazyReference<>(
            placeholderSupplier -> {
                // Capture the supplier for later verification
                capturedSupplier.set(placeholderSupplier);
                // Make recursive call to generate placeholder
                String placeholder = refHolder[0].get();
                return "final-" + placeholder;
            },
            () -> "placeholder"
        );

        // Initialize the reference
        String result = refHolder[0].get();
        assertEquals("final-placeholder", result, "Should use placeholder during initialization");

        // After initialization completes, the placeholder should be cleared
        // The captured supplier should now return null
        assertNull(capturedSupplier.get().get(),
            "Placeholder should be cleared after initialization completes");
    }
}
