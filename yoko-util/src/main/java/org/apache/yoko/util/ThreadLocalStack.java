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

import org.apache.yoko.io.SimplyCloseable;
import org.apache.yoko.util.cmsf.Cmsf;
import org.apache.yoko.util.rofl.Rofl;
import org.apache.yoko.util.yasf.Yasf;

import java.util.Set;
import java.util.logging.Logger;

/**
 * Generic stack-backed thread-local state holder with optional override behaviour.
 *
 * @param <T> the value type stored on the per-thread stack
 */
public final class ThreadLocalStack<T> {
    public static final ThreadLocalStack<Set<Yasf>> YASF_THREAD_LOCAL = new ThreadLocalStack<>("YASF", null, null);
    public static final ThreadLocalStack<Cmsf> CMSF_THREAD_LOCAL = new ThreadLocalStack<>("CMSF", Cmsf.CMSFv1, Cmsf.CMSFv1);
    public static final ThreadLocalStack<Rofl> ROFL_THREAD_LOCAL = new ThreadLocalStack<>("ROFL", Rofl.NONE, null);

    private final Logger logger;
    private final String name;
    private final ThreadLocal<State<T>> threadLocal;
    private final T overrideValue;
    private final SimplyCloseable popper = this::pop;
    private final SimplyCloseable resetOverride = this::resetOverride;

    public ThreadLocalStack(String name, T defaultValue, T overrideValue) {
        this.logger = Logger.getLogger(ThreadLocalStack.class.getName());
        this.name = name;
        this.overrideValue = overrideValue;
        this.threadLocal = ThreadLocal.withInitial(() -> new State<>(defaultValue));
    }

    public SimplyCloseable push(T value) {
        logger.finer(() -> String.format("%s thread local value pushed onto stack: %s", name, value));
        State<T> state = threadLocal.get();
        state.head = new Frame<>(value, state.head);
        return popper;
    }

    public T get() {
        State<T> state = threadLocal.get();
        T value = state.override ? overrideValue : state.head.value;
        logger.finer(() -> String.format("%s thread local value retrieved: %s", name, value));
        return value;
    }

    public T pop() {
        State<T> state = threadLocal.get();
        T value = state.head.value;
        state.head = state.head.prev;
        logger.finer(() -> String.format("%s thread local value popped from stack: %s", name, value));
        return value;
    }

    public void reset() {
        logger.finer(() -> String.format("%s thread local stack reset", name));
        threadLocal.remove();
    }

    public int depth() {
        State<T> state = threadLocal.get();
        int depth = 0;
        for (Frame<T> current = state.head; current != state.defaultFrame; current = current.prev) depth++;
        return depth;
    }

    public SimplyCloseable overrideForInterceptors() {
        threadLocal.get().override = true;
        logger.finer(() -> String.format("%s thread local override enabled", name));
        return resetOverride;
    }

    private void resetOverride() {
        threadLocal.get().override = false;
        logger.finer(() -> String.format("%s thread local override disabled", name));
    }

    private static final class State<T> {
        private final Frame<T> defaultFrame;
        private Frame<T> head;
        private boolean override;

        private State(T defaultValue) {
            this.defaultFrame = Frame.defaultFrame(defaultValue);
            this.head = defaultFrame;
        }
    }

    private static final class Frame<T> {
        private final T value;
        private final Frame<T> prev;

        private Frame(T value) {
            this.value = value;
            this.prev = this;
        }

        private Frame(T value, Frame<T> prev) {
            this.value = value;
            this.prev = prev;
        }

        private static <T> Frame<T> defaultFrame(T defaultValue) {
            return new Frame<>(defaultValue);
        }
    }
}
