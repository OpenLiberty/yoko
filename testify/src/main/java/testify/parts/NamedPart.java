/*
 * Copyright 2023 IBM Corporation and others.
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
package testify.parts;

import testify.bus.Bus;
import testify.bus.key.TypeKey;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

final class NamedPart implements Part {
    private enum Event implements TypeKey<Throwable> {STARTED, ENDED}
    private static final ConcurrentMap<String, AtomicInteger> uids = new ConcurrentHashMap<>();
    final String name;
    private final Part part;
    private final String uid;

    NamedPart(String name, Part part) {
        this.name = name;
        this.part = part;
        int instance = uids.computeIfAbsent(name, s -> new AtomicInteger()).incrementAndGet();
        this.uid = NamedPart.class.getSimpleName() + '[' + name + '#' + instance + ']';
    }

    public void run(Bus bus) {
        try {
            bus.log("part started");
            bus.forUser(uid).put(Event.STARTED);
            part.run(bus);
            bus.log("part ended normally");
            // normal completion — test passed
            bus.forUser(uid).put(Event.ENDED);
        } catch (Throwable e) {
            bus.log("part ended abnormally");
            System.err.printf("Test part '%s' failed with exception: %s%n", name, e);
            e.printStackTrace();
            bus.forUser(uid).put(Event.ENDED, e);
        } finally {
            System.out.flush();
            System.err.flush();
        }
    }

    public void waitForStart(Bus bus) { bus.forUser(uid).get(Event.STARTED); }
    public void waitForEnd(Bus bus) { bus.forUser(uid).get(Event.ENDED); }
}
