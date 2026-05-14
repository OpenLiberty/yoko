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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.logging.Logger;

import static java.util.Objects.requireNonNull;

/**
 * A thread-safe lazily initialized field using atomic function pointer swapping.
 * <p>
 * This implementation uses a sophisticated lock-free pattern where:
 * <ol>
 *   <li>The atomic function pointer initially points to an initialization function</li>
 *   <li>The first thread to call get() atomically swaps to a "waiting" function</li>
 *   <li>If the swap succeeds, that thread performs initialization</li>
 *   <li>If the swap fails, other threads wait on a latch</li>
 *   <li>After initialization, the pointer is swapped to a simple getter function</li>
 *   <li>The latch is released, allowing waiting threads to proceed</li>
 * </ol>
 *
 * @param <T> the type of the lazily initialized value
 */
public class LazyInitializedField<T> {
    private static final Logger LOGGER = Logger.getLogger(LazyInitializedField.class.getName());

    /**
     * Exception thrown when lazy initialization fails and retry is not allowed.
     */
    public static class InitializationException extends RuntimeException {
        private InitializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Exception thrown when a thread is interrupted while waiting for initialization.
     */
    public static class InitializationInterruptedException extends RuntimeException {
        private InitializationInterruptedException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Marker interface for the getter function to enable instanceof checks.
     */
    private interface Getter<T> extends Supplier<T> {
    }

    /**
     * Waiting function that carries its own latch for coordinating threads.
     */
    private class Waiter implements Supplier<T> {
        public final CountDownLatch latch = new CountDownLatch(1);

        @Override
        public T get() {
            LOGGER.fine(() -> "Thread " + Thread.currentThread().getName() + " waiting for initialization");
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new InitializationInterruptedException("Interrupted while waiting for initialization", e);
            }
            LOGGER.fine(() -> "Thread " + Thread.currentThread().getName() + " resuming after initialization");
            return functionPointer.get().get();
        }
    }

    /**
     * Atomic reference to the current function pointer.
     * This will be swapped between initialization, waiting, and getter functions.
     */
    private final AtomicReference<Supplier<T>> functionPointer;

    /**
     * The supplier that performs the actual lazy initialization.
     */
    private final Supplier<T> initializer;

    /**
     * Whether to allow retry on initialization failure.
     */
    private final boolean allowRetry;

    /**
     * Reference to the initialization function for use in CAS operation.
     */
    private final Supplier<T> initializationFunctionRef = this::initializationFunction;

    /**
     * Creates a new lazily initialized field with retry disabled.
     *
     * @param initializer the supplier that will compute the value on first access
     */
    public LazyInitializedField(Supplier<T> initializer) {
        this(initializer, false);
    }

    /**
     * Creates a new lazily initialized field.
     *
     * @param initializer the supplier that will compute the value on first access
     * @param allowRetry whether to allow retry on initialization failure
     */
    public LazyInitializedField(Supplier<T> initializer, boolean allowRetry) {
        this.initializer = requireNonNull(initializer, "initializer must not be null");
        this.allowRetry = allowRetry;
        this.functionPointer = new AtomicReference<>(initializationFunctionRef);
    }

    /**
     * Gets the value, initializing it if necessary.
     * <p>
     * This method is thread-safe and ensures that initialization happens exactly once,
     * even under concurrent access.
     * <p>
     * If initialization fails and {@code allowRetry} is {@code false}, this method
     * will throw {@link InitializationException} wrapping the original cause. This
     * applies to both the initial call and all subsequent calls, without retrying
     * initialization.
     * <p>
     * If a thread is interrupted while waiting for initialization to complete by another
     * thread, this method will throw {@link InitializationInterruptedException} wrapping
     * the {@link InterruptedException}.
     *
     * @return the initialized value
     * @throws InitializationException iff initialization failed and retry is not allowed
     * @throws InitializationInterruptedException if the thread is interrupted while waiting for initialization
     */
    public T get() {
        return functionPointer.get().get();
    }

    /**
     * The initialization function - attempts to atomically swap to the waiting function.
     * <p>
     * If the swap succeeds, this thread becomes the initializer.
     * If the swap fails, another thread is already initializing, so wait.
     *
     * @return the initialized value
     */
    private T initializationFunction() {
        LOGGER.fine(() -> "Thread " + Thread.currentThread().getName() + " attempting to initialize");

        // Check if we're still in initialization state before allocating Waiter
        if (functionPointer.get() != initializationFunctionRef) {
            LOGGER.fine(() -> "Thread " + Thread.currentThread().getName() + " detected initialization already in progress, reinvoking");
            return functionPointer.get().get();
        }

        Waiter waiter = new Waiter();

        boolean swapSucceeded = functionPointer.compareAndSet(
                initializationFunctionRef,
                waiter
        );

        if (swapSucceeded) {
            LOGGER.fine(() -> "Thread " + Thread.currentThread().getName() + " won initialization race");
            return performInitialization(waiter);
        } else {
            LOGGER.fine(() -> "Thread " + Thread.currentThread().getName() + " lost initialization race, reinvoking");
            return functionPointer.get().get();
        }
    }



    /**
     * Performs the actual lazy initialization.
     * <p>
     * This method:
     * <ol>
     *   <li>Calls the initializer to compute the value</li>
     *   <li>On success: swaps the function pointer to the getter function</li>
     *   <li>On failure: resets the function pointer to allow retry</li>
     *   <li>Releases the latch to unblock waiting threads</li>
     * </ol>
     *
     * @param waiter the waiter function that holds the latch
     * @return the initialized value
     */
    private T performInitialization(Waiter waiter) {
        try {
            LOGGER.fine(() -> "Thread " + Thread.currentThread().getName() + " performing initialization");

            T result = initializer.get();
            Getter<T> getter = () -> result;
            functionPointer.set(getter);

            LOGGER.fine(() -> "Thread " + Thread.currentThread().getName() + " completed initialization");
            return result;
        } catch (Throwable e) {
            if (allowRetry) {
                LOGGER.warning(() -> "Thread " + Thread.currentThread().getName() + " failed initialization, resetting for retry");
                functionPointer.set(initializationFunctionRef);
                throw e;
            } else {
                LOGGER.warning(() -> "Thread " + Thread.currentThread().getName() + " failed initialization, setting permanent error state");
                // Store the original cause and create new exception on each call for accurate stack traces
                Getter<T> errorGetter = () -> {
                    throw new InitializationException("Initialization failed and retry is not allowed", e);
                };
                functionPointer.set(errorGetter);
                throw new InitializationException("Initialization failed and retry is not allowed", e);
            }
        } finally {
            waiter.latch.countDown();
            LOGGER.fine(() -> "Thread " + Thread.currentThread().getName() + " released initialization latch");
        }
    }

    /**
     * Checks if initialization has completed.
     * <p>
     * Returns {@code true} if initialization succeeded or failed with {@code allowRetry=false}
     * (permanent error state). Returns {@code false} if not yet attempted or failed with
     * {@code allowRetry=true} (retry allowed).
     *
     * @return true if initialization completed (success or permanent error), false otherwise
     */
    public boolean isCompleted() {
        return functionPointer.get() instanceof Getter;
    }
}
