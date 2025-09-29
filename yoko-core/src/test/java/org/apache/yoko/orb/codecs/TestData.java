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
    static Stream<Object[]> asciiChars() { return rangeToTest(0x00,0x80).mapToObj(TestData::convertTo3Args); }

    static Stream<Object[]> isoLatinChars() { return rangeToTest(0x80, 0x100).mapToObj(TestData::convertTo3Args); }

    static Stream<Object[]> bmpChars() {
        return Stream.of(rangeToTest(0x100, 0xD800),
                        rangeToTest(0xE000, 0x10000))
                .flatMapToInt(s -> s)
                .mapToObj(TestData::convertTo3Args);
    }

    static Stream<Object[]> highSurrogates() {
        return  IntStream.of(0xD800, 0xD801, 0xDBFE, 0xDBFF).mapToObj(TestData::convertTo3Args);
    }

    static Stream<Object[]> lowSurrogates() {
        return  IntStream.of(0xDC00, 0xDC01, 0xDFFE, 0xDFFF).mapToObj(TestData::convertTo3Args);
    }

    static Stream<Object[]> wideChars() { return Stream.of(bmpChars(), highSurrogates(), lowSurrogates()).flatMap(s -> s); }

    static Object[] convertTo3Args(int i) { return new Object[]{String.format("0x%X", i), i, (char) i}; }

    static IntStream rangeToTest(int start, int finish) {
        final int STEP = 5;
        int count = (finish - start) / STEP - 2;
        assert count > 0;
        IntStream beginning = IntStream.range(start, start+STEP);
        IntStream middle = iterate(start, n -> n + STEP).limit(count);
        IntStream end = IntStream.range(finish-STEP, finish);
        return Stream.of(beginning, middle, end).flatMapToInt(s -> s);
    }
}
