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

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * A Java 8-compatible implementation of automatic resource cleanup similar to Java 9's {@code java.lang.ref.Cleaner}.
 *
 * <p>This class uses {@link PhantomReference} and {@link ReferenceQueue} to detect when objects become
 * phantom reachable (i.e., eligible for garbage collection) and automatically executes registered cleanup actions.
 *
 * <p>All YokoCleaner instances share a single daemon thread for processing cleanup actions, reducing
 * thread overhead in applications with multiple cleaner instances.
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * public class ResourceHolder {
 *     private static final YokoCleaner cleaner = YokoCleaner.create();
 *     private final SimplyCloseable cleanable;
 *
 *     public ResourceHolder() {
 *         // Register cleanup action that will run when this object is GC'd
 *         this.cleanable = cleaner.register(this, () -> {
 *             // Cleanup code here - must not reference 'this'
 *             closeNativeResource();
 *         });
 *     }
 *
 *     public void close() {
 *         // Manually trigger cleanup if needed
 *         cleanable.close();
 *     }
 * }
 * }</pre>
 *
 * <p><strong>Important Notes:</strong>
 * <ul>
 *   <li>The cleanup action must NOT hold a strong reference to the object being monitored</li>
 *   <li>Cleanup actions run on a shared daemon thread</li>
 *   <li>Exceptions thrown by cleanup actions are logged but do not stop the cleaner thread</li>
 *   <li>The returned {@link SimplyCloseable} is idempotent - multiple calls to {@code close()} are safe</li>
 *   <li>All threads calling {@code close()} will block until the cleanup action completes</li>
 * </ul>
 *
 * @see java.lang.ref.Cleaner
 * @see java.lang.ref.PhantomReference
 */
public final class YokoCleaner {
    private static final Logger logger = Logger.getLogger(YokoCleaner.class.getName());

    // Shared queue and cleaner thread for all YokoCleaner instances
    private static final ReferenceQueue<Object> SHARED_QUEUE = new ReferenceQueue<>();
    private static final LazyReference<Void> lazyCleanupThread = new LazyReference<>(YokoCleaner::startCleanupThread);

    private static Void startCleanupThread() {
        Thread thread = new Thread(YokoCleaner::processQueue, "YokoCleaner-Thread");
        thread.setDaemon(true);
        thread.start();
        return null;
    }

    private final Set<PhantomCleanable<?>> cleanables = ConcurrentHashMap.newKeySet();

    /**
     * Creates a new cleaner instance. All instances share a single daemon thread for processing cleanup actions.
     *
     * @return a new cleaner instance
     */
    public static YokoCleaner create() { return new YokoCleaner(); }

    private YokoCleaner() {
        // Nothing to initialize - uses shared static resources
    }

    /**
     * Registers an object and a cleanup action to be executed when the object becomes phantom reachable.
     *
     * <p><strong>CRITICAL:</strong> The cleanup action must NOT hold a strong reference to the object being
     * monitored, otherwise the object will never become phantom reachable and the cleanup will never run.
     *
     * <p>The returned {@link SimplyCloseable} can be used to manually trigger cleanup before garbage collection.
     * Calling {@code close()} is idempotent and thread-safe - all threads will block until cleanup completes.
     *
     * @param obj the object to monitor for garbage collection
     * @param action the cleanup action to execute (must not reference {@code obj})
     * @return a {@link SimplyCloseable} that can be used to manually trigger cleanup
     * @throws NullPointerException if obj or action is null
     */
    public SimplyCloseable register(Object obj, Runnable action) {
        Objects.requireNonNull(obj, "obj must not be null");
        Objects.requireNonNull(action, "action must not be null");

        // Ensure cleaner thread is started on first registration
        lazyCleanupThread.get();

        return PhantomCleanable.create(obj, action, cleanables);
    }

    private static void processQueue() {
        logger.info("YokoCleaner thread started");
        for (;;) {
            try {
                // Block until a reference is available
                PhantomCleanable<?> ref = (PhantomCleanable<?>) SHARED_QUEUE.remove();
                ref.close();
            } catch (InterruptedException e) {
                // Continue processing - daemon thread runs forever
            } catch (Throwable t) {
                // Log but don't stop the cleaner thread - catch Throwable to ensure thread continues
                logger.warning("Exception in YokoCleaner cleanup action: " + t);
            }
        }
    }

    private static final class PhantomCleanable<T> extends PhantomReference<T> implements SimplyCloseable {
        private final LazyReference<Void> lazyCleanup;

        static <T> PhantomCleanable<T> create(T referent, Runnable action, Set<PhantomCleanable<?>> cleanables) {
            PhantomCleanable<T> cleanable = new PhantomCleanable<>(referent, YokoCleaner.SHARED_QUEUE, action, cleanables);
            cleanables.add(cleanable);
            return cleanable;
        }

        private PhantomCleanable(T referent, ReferenceQueue<? super T> queue, Runnable action, Set<PhantomCleanable<?>> cleanables) {
            super(referent, queue);
            // Use LazyReference to ensure cleanup happens exactly once
            // and all threads wait for completion
            this.lazyCleanup = new LazyReference<>(() -> {
                try {
                    action.run();
                } catch (Throwable t) {
                    // Log but don't propagate exceptions from cleanup actions
                    logger.warning("Exception in cleanup action: " + t);
                } finally {
                    cleanables.remove(this);
                    clear();
                }
                return null;
            });
        }

        @Override
        public void close() { lazyCleanup.get(); }
    }
}
