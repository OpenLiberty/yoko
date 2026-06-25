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

import org.apache.yoko.io.SimplyCloseable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class YokoCleanerTest {

    private YokoCleaner cleaner;

    @BeforeEach
    public void setUp() {
        cleaner = YokoCleaner.create();
    }

    @AfterEach
    public void tearDown() {
        // No shutdown needed - singleton cleaner thread runs forever
    }

    @Test
    public void testManualCleanup() throws Exception {
        AtomicInteger cleanupCount = new AtomicInteger(0);
        Object obj = new Object();

        SimplyCloseable cleanable = cleaner.register(obj, cleanupCount::incrementAndGet);

        // Manually trigger cleanup
        cleanable.close();

        assertEquals(1, cleanupCount.get(), "Cleanup should have been called once");
    }

    @Test
    public void testIdempotentCleanup() throws Exception {
        AtomicInteger cleanupCount = new AtomicInteger(0);
        Object obj = new Object();

        SimplyCloseable cleanable = cleaner.register(obj, cleanupCount::incrementAndGet);

        // Call close multiple times
        cleanable.close();
        cleanable.close();
        cleanable.close();

        assertEquals(1, cleanupCount.get(), "Cleanup should have been called exactly once despite multiple close() calls");
    }

    @Test
    public void testConcurrentCleanup() throws Exception {
        AtomicInteger cleanupCount = new AtomicInteger(0);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(5);
        Object obj = new Object();

        SimplyCloseable cleanable = cleaner.register(obj, () -> {
            cleanupCount.incrementAndGet();
            try {
                Thread.sleep(100); // Simulate some work
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Start 5 threads that all try to close concurrently
        for (int i = 0; i < 5; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    cleanable.close();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown(); // Start all threads
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "All threads should complete");
        assertEquals(1, cleanupCount.get(), "Cleanup should have been called exactly once despite concurrent close() calls");
    }

    @Test
    public void testAutomaticCleanupOnGC() throws Exception {
        AtomicInteger cleanupCount = new AtomicInteger(0);
        CountDownLatch cleanupLatch = new CountDownLatch(1);

        // Create object in a separate scope so it can be GC'd
        createAndRegisterObject(cleanupCount, cleanupLatch);

        // Force garbage collection
        for (int i = 0; i < 10; i++) {
            System.gc();
            System.runFinalization();
            if (cleanupLatch.await(100, TimeUnit.MILLISECONDS)) {
                break;
            }
        }

        assertTrue(cleanupLatch.await(5, TimeUnit.SECONDS), "Cleanup should have been triggered by GC");
        assertEquals(1, cleanupCount.get(), "Cleanup should have been called once");
    }

    private void createAndRegisterObject(AtomicInteger cleanupCount, CountDownLatch cleanupLatch) {
        Object obj = new Object();
        cleaner.register(obj, () -> {
            cleanupCount.incrementAndGet();
            cleanupLatch.countDown();
        });
        // obj goes out of scope here and becomes eligible for GC
    }

    @Test
    public void testCleanupWithException() throws Exception {
        AtomicInteger cleanupCount = new AtomicInteger(0);
        Object obj = new Object();

        SimplyCloseable cleanable = cleaner.register(obj, () -> {
            cleanupCount.incrementAndGet();
            throw new RuntimeException("Test exception");
        });

        // Should not throw exception to caller
        assertDoesNotThrow(cleanable::close);
        assertEquals(1, cleanupCount.get(), "Cleanup should have been called despite exception");
    }

    @Test
    public void testNullObjectThrowsException() {
        assertThrows(NullPointerException.class, () -> {
            cleaner.register(null, () -> {});
        });
    }

    @Test
    public void testNullActionThrowsException() {
        assertThrows(NullPointerException.class, () -> {
            cleaner.register(new Object(), null);
        });
    }

    @Test
    public void testMultipleRegistrations() throws Exception {
        AtomicInteger cleanup1Count = new AtomicInteger(0);
        AtomicInteger cleanup2Count = new AtomicInteger(0);
        AtomicInteger cleanup3Count = new AtomicInteger(0);

        Object obj1 = new Object();
        Object obj2 = new Object();
        Object obj3 = new Object();

        SimplyCloseable cleanable1 = cleaner.register(obj1, cleanup1Count::incrementAndGet);
        SimplyCloseable cleanable2 = cleaner.register(obj2, cleanup2Count::incrementAndGet);
        SimplyCloseable cleanable3 = cleaner.register(obj3, cleanup3Count::incrementAndGet);

        cleanable1.close();
        cleanable2.close();
        cleanable3.close();

        assertEquals(1, cleanup1Count.get());
        assertEquals(1, cleanup2Count.get());
        assertEquals(1, cleanup3Count.get());
    }

    @Test
    public void testCleanupActionDoesNotReferenceObject() throws Exception {
        // This test verifies the pattern where cleanup action must not reference the monitored object
        AtomicInteger cleanupCount = new AtomicInteger(0);

        class Resource {
            private final int id;
            Resource(int id) { this.id = id; }
            int getId() { return id; }
        }

        Resource resource = new Resource(42);
        int capturedId = resource.getId(); // Capture value, not reference

        SimplyCloseable cleanable = cleaner.register(resource, () -> {
            // This is correct - uses captured primitive value, not object reference
            cleanupCount.addAndGet(capturedId);
        });

        cleanable.close();
        assertEquals(42, cleanupCount.get());
    }


}
