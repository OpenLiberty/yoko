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
 * distributed under the License is distributed on an \"AS IS\" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.apache.yoko.util.rofl;

import org.apache.yoko.io.SimplyCloseable;

import java.util.logging.Logger;

/**
 * Provides a stack-based approach to track the ROFL settings in effect for the current request.
 */
public final class RoflThreadLocal {
    private static final Logger LOGGER = Logger.getLogger(RoflThreadLocal.class.getName());
    private static final ThreadLocal<Stack> threadLocalStack = ThreadLocal.withInitial(Stack::new);

    private RoflThreadLocal() {}

    private static final class Stack {
        public Frame head = Frame.DEFAULT;
        public boolean override = false;
    }

    private static final class Frame {
        private static final Frame DEFAULT = new Frame();
        final Rofl value;
        final Frame prev;

        private Frame() {
            this.value = Rofl.NONE;
            this.prev = this;
        }

        Frame(Rofl value, Frame prev) {
            this.value = value;
            this.prev = prev;
        }
    }

    public static SimplyCloseable override() {
        Stack info = threadLocalStack.get();
        info.override = true;
        return () -> info.override = false;
    }

    public static SimplyCloseable push(Rofl rofl) {
        final Stack info = threadLocalStack.get();
        LOGGER.finer(() -> String.format("ROFL thread local version pushed onto stack: %s", rofl));
        info.head = new Frame(rofl, info.head);
        return RoflThreadLocal::pop;
    }

    public static Rofl get() {
        final Stack info = threadLocalStack.get();
        final boolean override = info.override;
        final Rofl rofl = (override) ? null : info.head.value;
        LOGGER.finer(() -> String.format("ROFL thread local version retrieved: %s, override is %b", rofl, override));
        return rofl;
    }

    public static Rofl pop() {
        final Stack info = threadLocalStack.get();
        final Rofl rofl = info.head.value;
        LOGGER.finer(() -> String.format("ROFL thread local version popped from stack: %s", rofl));
        info.head = info.head.prev;
        return rofl;
    }

    public static void reset() {
        LOGGER.finer(() -> "ROFL thread local stack reset");
        threadLocalStack.remove();
    }

    /**
     * Returns the current stack depth for testing purposes only.
     * This method is package-private and should not be used in production code.
     *
     * @return the number of frames currently on the stack (0 means empty/clean state)
     */
    static int getStackDepth() {
        final Stack info = threadLocalStack.get();
        int depth = 0;
        Frame current = info.head;
        while (current != Frame.DEFAULT) {
            depth++;
            current = current.prev;
        }
        return depth;
    }
}
