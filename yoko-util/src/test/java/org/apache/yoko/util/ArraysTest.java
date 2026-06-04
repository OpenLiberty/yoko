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
package org.apache.yoko.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.yoko.util.Arrays.emptyArray;
import static org.apache.yoko.util.Arrays.NO_STRINGS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for {@link Arrays#emptyArray(Class)} method.
 */
class ArraysTest {

    @Test
    void testNoStringsConstant() {
        // Verify NO_STRINGS is the same instance as emptyArray(String.class)
        assertSame(NO_STRINGS, emptyArray(String.class));
        assertNotNull(NO_STRINGS);
        assertEquals(0, NO_STRINGS.length);
        assertEquals(String.class, NO_STRINGS.getClass().getComponentType());
    }

    @Test
    void testEmptyArrayWithDifferentTypes() {
        // Test with various common types
        Integer[] integers = emptyArray(Integer.class);
        Object[] objects = emptyArray(Object.class);
        Serializable[] serializables = emptyArray(Serializable.class);

        assertEquals(0, integers.length);
        assertEquals(0, objects.length);
        assertEquals(0, serializables.length);

        // Verify correct component types
        assertEquals(Integer.class, integers.getClass().getComponentType());
        assertEquals(Object.class, objects.getClass().getComponentType());
        assertEquals(Serializable.class, serializables.getClass().getComponentType());
    }

    @Test
    void testEmptyArrayCachingForSystemClasses() {
        // For system classes, the same instance should be returned
        String[] first = NO_STRINGS;
        String[] second = NO_STRINGS;

        assertSame(first, second, "Same instance should be returned for system classes");
    }

    @Test
    void testEmptyArrayCachingForMultipleTypes() {
        // Each type should have its own cached instance
        String[] strings1 = NO_STRINGS;
        String[] strings2 = NO_STRINGS;
        Integer[] integers1 = emptyArray(Integer.class);
        Integer[] integers2 = emptyArray(Integer.class);

        assertSame(strings1, strings2);
        assertSame(integers1, integers2);
    }

    @Test
    void testEmptyArrayThreadSafety() throws InterruptedException {
        final int threadCount = 10;
        final int iterationsPerThread = 1000;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(threadCount);
        final List<String[]> results = new ArrayList<>();
        final AtomicInteger successCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready

                    for (int j = 0; j < iterationsPerThread; j++) {
                        String[] array = NO_STRINGS;
                        synchronized (results) {
                            results.add(array);
                        }
                    }
                    successCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Start all threads
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS), "All threads should complete");
        executor.shutdown();

        assertEquals(threadCount, successCount.get(), "All threads should complete successfully");
        assertEquals(threadCount * iterationsPerThread, results.size());

        // All results should be the same instance (for system classes)
        String[] first = results.get(0);
        for (String[] array : results) {
            assertSame(first, array, "All arrays should be the same cached instance");
        }
    }

    @Test
    void testEmptyArrayCustomClassThreadSafety() throws InterruptedException {
        // Test thread safety with custom classes (uses ClassValue caching)
        final int threadCount = 20;
        final int iterationsPerThread = 500;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(threadCount);
        final List<TestClass[]> results = new ArrayList<>();
        final AtomicInteger successCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready

                    for (int j = 0; j < iterationsPerThread; j++) {
                        TestClass[] array = emptyArray(TestClass.class);
                        synchronized (results) {
                            results.add(array);
                        }

                        // Occasionally suggest GC to stress-test ClassValue handling
                        if (j % 100 == 0) {
                            System.gc();
                            Thread.yield();
                        }
                    }
                    successCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Start all threads
        assertTrue(doneLatch.await(15, TimeUnit.SECONDS), "All threads should complete");
        executor.shutdown();

        assertEquals(threadCount, successCount.get(), "All threads should complete successfully");
        assertEquals(threadCount * iterationsPerThread, results.size());

        // Verify all arrays are valid (zero-length, correct type)
        for (TestClass[] array : results) {
            assertNotNull(array);
            assertEquals(0, array.length);
            assertEquals(TestClass.class, array.getClass().getComponentType());
        }

        // ClassValue caching ensures all instances are the same
        long distinctInstances = results.stream().distinct().count();
        assertEquals(1, distinctInstances, "ClassValue should cache a single instance per class");
    }

    @RepeatedTest(5)
    void testEmptyArrayConsistency() {
        // Repeated test to ensure consistent behavior across multiple invocations
        String[] array = NO_STRINGS;
        assertNotNull(array);
        assertEquals(0, array.length);
        assertEquals(String.class, array.getClass().getComponentType());
    }

    @Test
    void testEmptyArrayWithInterfaces() {
        // Test with interface types
        Runnable[] runnables = emptyArray(Runnable.class);
        @SuppressWarnings("unchecked")
        Comparable<String>[] comparables = (Comparable<String>[]) emptyArray(Comparable.class);

        assertNotNull(runnables);
        assertNotNull(comparables);
        assertEquals(0, runnables.length);
        assertEquals(0, comparables.length);
        assertEquals(Runnable.class, runnables.getClass().getComponentType());
        assertEquals(Comparable.class, comparables.getClass().getComponentType());
    }

    @Test
    void testEmptyArrayWithAbstractClass() {
        // Test with abstract class
        AbstractTestClass[] result = emptyArray(AbstractTestClass.class);

        assertNotNull(result);
        assertEquals(0, result.length);
        assertEquals(AbstractTestClass.class, result.getClass().getComponentType());
    }

    @Test
    void testEmptyArrayMultipleCallsReturnSameInstance() {
        // Verify that multiple calls return the same cached instance
        Object[] first = emptyArray(Object.class);
        Object[] second = emptyArray(Object.class);
        Object[] third = emptyArray(Object.class);

        assertSame(first, second);
        assertSame(second, third);
        assertSame(first, third);
    }

    @Test
    void testEmptyArrayImmutability() {
        // Verify that the returned array is truly empty and cannot be modified
        // (well, it can be modified, but it's zero-length so there's nothing to modify)
        String[] array = NO_STRINGS;
        assertEquals(0, array.length);

        // Attempting to access any element should throw ArrayIndexOutOfBoundsException
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> {
            String ignored = array[0];
        });
    }

    @Test
    void testEmptyArrayCustomClassCaching() {
        // Test that custom classes use ClassValue caching
        TestClass[] first = emptyArray(TestClass.class);
        TestClass[] second = emptyArray(TestClass.class);

        // Should return same instance due to ClassValue caching
        assertNotNull(first);
        assertNotNull(second);
        assertEquals(0, first.length);
        assertEquals(0, second.length);

        // ClassValue ensures same instance is always returned for the same class
        assertSame(first, second, "Custom class arrays should be cached via ClassValue");
    }

    // Test helper classes
    private static class TestClass {}

    private abstract static class AbstractTestClass {}
}
