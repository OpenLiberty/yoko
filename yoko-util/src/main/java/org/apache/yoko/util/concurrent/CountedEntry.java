/*==============================================================================
 * Copyright 2020 IBM Corporation and others.
 * 
 * Licensed under the Apache License, Version 2.0 (the License);
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
 *=============================================================================*/
package org.apache.yoko.util.concurrent;

import org.apache.yoko.util.Reference;
import org.apache.yoko.util.Sequential;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A thread-safe, reference-counting entry for use in a cache.
 * If threads race to call @link{#clear} and @link{#obtain},
 * one or other method will return <code>null</code>.
 * <br>
 * Entries with a reference count of zero will be put onto the
 * provided @link{Sequential} object, and removed on successful
 * calls to either @link{#clear} or @link{#obtain}.
 */
class CountedEntry<K, V> {
    private static final int CLEANED = Integer.MIN_VALUE;
    private static final int NOT_READY = -1;
    private static final int IDLE = -2;
    private final AtomicInteger refCount = new AtomicInteger(NOT_READY);
    private final Sequential<CountedEntry<K, V>> idleEntries;
    private Sequential.Place<?> idlePlace;
    private V value;
    final K key;

    /** Create a not-yet-ready CountedEntry - the next operation must be to call setValue() or abort() */
    CountedEntry(K key, Sequential<CountedEntry<K, V>> idleEntries) {
        this.key = key;
        this.idleEntries = idleEntries;
    }

    ValueReference setValue(V value) {
        this.value = Objects.requireNonNull(value);
        notifyReady(1);
        return new ValueReference();
    }

    void abort() {
        assert value == null;
        notifyReady(CLEANED);
    }

    private synchronized void notifyReady(int newCount) {
        boolean success = refCount.compareAndSet(NOT_READY, newCount);
        assert success;
        this.notifyAll();
    }

    private synchronized void blockWhileNotReady() {
        while (refCount.get() == NOT_READY) {
            try {
                this.wait();
            } catch (InterruptedException ignored) {
            }
        }
    }

    // Acquire a reference to this entry.
    private boolean acquire() {
        for (;;) {
            int oldCount = refCount.get();
            switch (oldCount) {
            case CLEANED:
                // terminal state - must fail
                return false;
            case NOT_READY:
                blockWhileNotReady();
                break;
            case IDLE:
                if (rouseFromIdleState()) return true;
                break;
            default:
                // increment the value retrieved or start again
                if (refCount.compareAndSet(oldCount, oldCount + 1)) return true;
                break;
            }
        }
    }

    private boolean rouseFromIdleState() {
        // grab the ref while it's idle or report failure
        if (!!!refCount.compareAndSet(IDLE, NOT_READY)) return false;
        // remove this entry from the idle queue
        Object self = idlePlace.relinquish();
        assert this == self;
        // mark that this entry is no longer in the queue
        idlePlace = null;
        // let other threads know this entry is accessible again
        notifyReady(1);
        return true;
    }

    /**
     * Release a reference to this entry.
     * Only the owner of the reference should drive this method.
     *
     * @return true to facilitate use in boolean expressions
     * @see ReferenceCloserTask#run()
     */
    private boolean release() {
        int newCount = refCount.decrementAndGet();
        if (newCount != 0) return true;

        // try to IDLE this entry
        if (!!!refCount.compareAndSet(0, NOT_READY))
            // some other thread revived or purged this entry, so no need to IDLE it now
            return true;

        idlePlace = idleEntries.put(this);
        notifyReady(IDLE);
        return true;
    }

    // Mark this entry unusable. Return value if entry is modified, null otherwise.
    V clear() {
        if (!!! refCount.compareAndSet(IDLE, CLEANED)) return null;
        // safe to read/update idlePlace since this is the only thread that has moved it from IDLE
        try {
            Object self = idlePlace.relinquish();
            assert self == this;
            return value;
        } finally {
            value = null;
            idlePlace = null;
        }
    }

    /**
     * Attempt to acquire a counted reference to a value.
     * If a non-null value is returned, the caller is responsible for
     * calling {@link ValueReference#close()} on the result
     * when the reference is no longer required.
     *
     * @return the reference, or <code>null</code> if no reference could be obtained
     */
    ValueReference obtain() {return acquire() ? new ValueReference() : null;}

    /**
     * Clear an entry that still has valid references.
     *
     * @return <code>true</code>> if this invocation was successful,
     * and <code>false</code> if the entry was already purged
     */
    private boolean purge() {
        for (;;) {
            int oldCount = refCount.get();
            if (oldCount == CLEANED) return false;
            if (oldCount < 1) throw new IllegalStateException();
            if (refCount.compareAndSet(oldCount, CLEANED)) return true;
        }
    }

    final class ValueReference implements Reference<V> {
        private final ReferenceCloserTask closer = new ReferenceCloserTask();
        public V get() {return value;}
        public void close() {closer.run();}
        CountedEntry<K, V> invalidateAndGetEntry() {return closer.purge() ? CountedEntry.this : null;}
        Runnable getCloserTask() {return closer;}
    }

    /**
     * In order to drive cleanup after a ValueReference becomes unreachable,
     * we need to store the clean up details in a separate object that holds
     * no strong reference back to the ValueReference object
     */
    private final class ReferenceCloserTask implements Runnable {
        boolean closed;
        public synchronized void run() {closed = closed || release();}
        synchronized boolean purge() {
            if (closed) return false;
            closed = true;
            return CountedEntry.this.purge();
        }
    }
}
