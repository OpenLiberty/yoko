/*
 * Copyright 2025 IBM Corporation and others.
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
package org.apache.yoko.orb.codecs;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.stream.IntStream.iterate;

interface TestData {
    static Stream<Object[]> asciiChars() { return testRange(0x00,0x80).mapToObj(TestData::toHexIntAndChar); }

    static Stream<Object[]> isoLatinChars() { return testRange(0x80, 0x100).mapToObj(TestData::toHexIntAndChar); }

    static Stream<Object[]> bmpChars() {
        return Stream.of(testRange(0x100, 0xD800),
                        testRange(0xE000, 0x10000))
                .flatMapToInt(s -> s)
                .mapToObj(TestData::toHexIntAndChar);
    }

    static Stream<Object[]> highSurrogates() {
        return  IntStream.of(0xD800, 0xD801, 0xDBFE, 0xDBFF).mapToObj(TestData::toHexIntAndChar);
    }

    static Stream<Object[]> lowSurrogates() {
        return  IntStream.of(0xDC00, 0xDC01, 0xDFFE, 0xDFFF).mapToObj(TestData::toHexIntAndChar);
    }

    static Stream<Object[]> wideChars() { return Stream.of(bmpChars(), highSurrogates(), lowSurrogates()).flatMap(s -> s); }

    static Object[] toHexIntAndChar(int i) { return new Object[]{describe(i), i, (char) i}; }

    static Object[] toHexIntAndString(int i) { return new Object[]{describe(i), i, toString(i)};}

    static String describe(int codepoint) { return String.format("0x%04X %s", codepoint, Character.getName(codepoint)); }

    static String toString(int codepoint) { return new String(Character.toChars(codepoint)); }

    static IntStream testRange(int start, int finish) {
        final int STEP = 5;
        int count = (finish - start) / STEP - 2;
        assert count > 0;
        IntStream beginning = IntStream.range(start, start+STEP);
        IntStream middle = iterate(start + STEP, n -> n + STEP).limit(count);
        IntStream end = IntStream.range(finish-STEP, finish);
        return Stream.of(beginning, middle, end).flatMapToInt(s -> s);
    }
}
