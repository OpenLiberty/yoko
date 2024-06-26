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
package testify.annotation.logging;

import testify.annotation.Logging;
import testify.bus.key.TypeKey;
import testify.io.Stringifiable;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

final class LogSetting implements Stringifiable {
    enum Key implements TypeKey<LogSetting> { SEND_SETTING }

    private final Level level;
    private final String name;
    private transient Logger logger;
    private transient Level oldLevel;

    LogSetting(Logging annotation) { this(annotation.value(), annotation.level().level); }

    LogSetting(String name, Level level) {
        this.level = level;
        this.name = name;
    }

    @Override
    public String stringify() {return level.getName() + " " + name; }
    @Unstringify
    LogSetting(String s) { this(s.replaceAll("[^ ]* ", ""), Level.parse(s.replaceAll(" .*", "")));}

    void apply() {
        logger = Logger.getLogger(name);
        oldLevel = logger.getLevel();
        System.out.println(">>> applying log setting: " + this + " (was " + oldLevel + ") <<<");
        logger.setLevel(level);
    }

    void undo() {
        System.out.printf(">>> Reverting log setting \"%s\" from %s to %s <<<%n", this.name, this.level, this.oldLevel);
        this.logger.setLevel(oldLevel);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        LogSetting that = (LogSetting) other;
        return Objects.equals(level, that.level) && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() { return Objects.hash(level, name); }

    @Override
    public String toString() { return String.format("%s=%s", name, level); }
}
