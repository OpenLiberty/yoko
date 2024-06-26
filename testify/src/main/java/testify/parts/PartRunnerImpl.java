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

import org.opentest4j.AssertionFailedError;
import testify.bus.Bus;
import testify.bus.InterProcessBus;
import testify.bus.TestLogLevel;
import testify.io.EasyCloseable;
import testify.util.ObjectUtil;

import java.util.Deque;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.EnumSet.complementOf;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.SECONDS;
import static testify.bus.TestLogLevel.DEBUG;
import static testify.bus.TestLogLevel.ERROR;
import static testify.bus.TestLogLevel.WARN;
import static testify.parts.PartRunner.State.COMPLETED;
import static testify.parts.PartRunner.State.CONFIGURING;
import static testify.parts.PartRunnerImpl.JoinHookType.JOIN;
import static testify.parts.PartRunnerImpl.JoinHookType.POST_JOIN;
import static testify.parts.PartRunnerImpl.JoinHookType.PRE_JOIN;

class PartRunnerImpl implements PartRunner {
    private static final EnumSet<TestLogLevel> URGENT_LEVELS = EnumSet.of(ERROR, WARN);
    private final String label = ObjectUtil.getNextObjectLabel(PartRunnerImpl.class);
    private final InterProcessBus centralBus = InterProcessBus.createParent();
    private final Map<String, Bus> knownBuses = new ConcurrentHashMap<>();

    enum JoinHookType {
        PRE_JOIN,
        JOIN,
        POST_JOIN;
    }
    private final EnumMap<JoinHookType, Deque<EasyCloseable>> hooks = new EnumMap<>(JoinHookType.class);
    { for (JoinHookType ht: JoinHookType.values()) hooks.put(ht, new ConcurrentLinkedDeque<>()); }
    private final Map<EasyCloseable, String> hookNames = new HashMap<>();
    private Function<String, Bus> busFactory = ((Function<String, Bus>)centralBus::forUser)
            .andThen(b -> b.logToSysErr(URGENT_LEVELS))
            .andThen(b -> b.logToSysOut(complementOf(URGENT_LEVELS)));
    private boolean useProcesses = false;

    private Part jvmInitHook = Part.NO_OP;
    private String[] jvmArgs;
    private State state = CONFIGURING;
    @Override
    public State getState() { return state; }

    @Override
    public void addJVMStartupHook(Part hook) throws IllegalStateException {
        if (CONFIGURING != this.state) throw new IllegalStateException("PartRunner cannot be configured after first use");
        jvmInitHook = jvmInitHook.andThen(hook);
    }

    @Override
    public PartRunner enableTestLogging(TestLogLevel level, String pattern) {
        // decorate the bus factory to enable the requested logging on any new buses
        busFactory = busFactory.andThen(b -> b.enableLogging(level, pattern));
        // Add the new log setting to existing buses.
        // There is a race condition if new buses are created concurrently
        // but we do this step second because duplicated log settings won't matter.
        knownBuses.values().forEach(bus -> bus.enableLogging(level, pattern));
        return this;
    }

    @Override
    public Bus bus(String partName) {
        return knownBuses.computeIfAbsent(partName, busFactory);
    }

    private Bus privateBus() { return bus(label); }

    @Override
    public PartRunner useNewJVMWhenForking(String... jvmArgs) {
        if (state == COMPLETED) throw new IllegalStateException("Runner already completed");
        this.useProcesses = true;
        this.jvmArgs = jvmArgs;
        return this;
    }

    @Override
    public PartRunner useNewThreadWhenForking() {
        if (state == COMPLETED) throw new IllegalStateException("Runner already completed");
        this.useProcesses = false;
        this.jvmArgs = null;
        return this;
    }

    @Override
    public ForkedPart fork(String partName, Part part) {
        if (state == COMPLETED) throw new IllegalStateException("Runner already completed");
        final Runner<?> runner;
        if (useProcesses) {
            runner = new ProcessRunner(jvmArgs);
            part = part.butFirst(jvmInitHook);
        } else {
            runner = ThreadRunner.SINGLETON;
        }

        return fork(runner, partName, part);
    }

    private <J> ForkedPart fork(Runner<J> runner, String partName, Part part) {
        state = State.IN_USE;
        try {
            final NamedPart namedPart = new NamedPart(partName, part);
            J job = runner.fork(centralBus, namedPart);
            namedPart.waitForStart(bus(partName));
            registerForJoin(runner, job, partName);
            // need to return a ForkedPart
            return endAction -> addHook(PRE_JOIN, partName, () -> endAction.accept(bus(partName)));
        } catch (Throwable throwable) {
            throw fatalError(throwable);
        }
    }

    private void addHook(JoinHookType hookType, String partName, EasyCloseable hook) {
        hookNames.put(hook, partName);
        Deque<EasyCloseable> hookQueue = hooks.get(hookType);
        // PRE_JOIN hooks are run in order of addition;
        // other hooks are run in reverse order of addition.
        if (requireNonNull(hookType) == PRE_JOIN) hookQueue.add(hook);
        else hookQueue.addFirst(hook);
    }

    private Error fatalError(Throwable t) {
        try {
            try {
                throw t;
            } catch (RuntimeException | Error runtimeExceptionOrError) {
                throw runtimeExceptionOrError;
            } catch (Throwable e) {
                t = new AssertionFailedError("Unexpected exception: " + t).initCause(t);
                throw (Error) t;
            }
        } finally {
            join(); // clean up anything already launched
        }
    }

    // recursively ensure close using try-with-resources and finally
    private void close(JoinHookType type) {
        Deque<EasyCloseable> closeables = this.hooks.get(type);
        if (closeables.isEmpty()) return; // end recursion when all
        String name = "unknown";
        // Use try-with-resources to drive close on the first hook.
        // Get hook using poll() so the queue shrinks.
        try (EasyCloseable hook = closeables.poll()) {
            final String partName = name = hookNames.get(hook);
            privateBus().log(DEBUG, () -> "Running " + partName + " " + type + " hook.");
        } finally {
            final String partName = name;
            privateBus().log(DEBUG, () -> "Stopped running " + partName + " " + type + " hook.");
            // recurse in finally to close next hook
            close(type);
        }
    }

    @Override
    public void join() {
        // close down the main bus
        try (EasyCloseable close = centralBus) {
            runHooks(PRE_JOIN);
            runHooks(JOIN);
            runHooks(POST_JOIN);
            privateBus().log("Completed all join actions.");
        } finally {
            state = COMPLETED;
        }
    }

    private void runHooks(JoinHookType type) {
        privateBus().log(() -> "Running " + type + " hooks: " + hooks.get(type).stream().map(hookNames::get).collect(Collectors.joining()));
        close(type);
    }

    private <J> void registerForJoin(Runner<J> runner, J job, String name) {
        addHook(JOIN, name, () -> {
            try {
                if (runner.join(job, 5, SECONDS)) return;
                privateBus().log(ERROR, "The test part '" + name + "' did not complete. Trying to force it to stop.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        addHook(POST_JOIN, name, () -> {
            try {
                if (runner.stop(job, 5, SECONDS)) return;
                privateBus().log(ERROR, "The test part '" + name + "' did not complete when forced. Giving up.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    @Override
    public String toString() { return label; }

    @Override
    public void dumpBuses() {
        System.out.println("== Dumping buses ==");
        knownBuses.forEach((name, bus) -> {
            System.out.printf("===%s===%n", name);
            bus.biStream().forEach((key, value) -> System.out.printf("\t%s=%s%n", key, value));
        });
        System.out.println("== Bus dump ended ==");
    }
}
