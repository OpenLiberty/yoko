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

import java.util.function.BiConsumer;

import static java.util.stream.IntStream.range;

/**
 * Utility class for array operations.
 */
public enum Arrays {
    ;

    /**
     * Functional interface for iterating over paired elements from two arrays.
     *
     * @param <A> the type of elements in the first array
     * @param <B> the type of elements in the second array
     */
    public interface Zipper<A, B> {
        /**
         * Performs the given action for each pair of elements.
         *
         * @param action the action to perform on each pair
         */
        void forEach(BiConsumer<A, B> action);
    }

    /**
     * Zips two arrays together, pairing elements at corresponding indices.
     * The arrays must have the same length.
     *
     * <p>Example usage:
     * <pre>{@code
     * String[] names = {"Alice", "Bob", "Charlie"};
     * Integer[] ages = {25, 30, 35};
     *
     * zip(names, ages).forEach((name, age) ->
     *     System.out.println(name + " is " + age + " years old")
     * );
     * }</pre>
     *
     * @param <A> the type of elements in the first array
     * @param <B> the type of elements in the second array
     * @param arr1 the first array
     * @param arr2 the second array
     * @return a Zipper that can iterate over paired elements
     * @throws IllegalArgumentException if the arrays have different lengths
     */
    public static <A, B> Zipper<A, B> zip(A[] arr1, B[] arr2) {
        if (arr1.length != arr2.length) throw new IllegalArgumentException("Arrays must have the same length: " + arr1.length + " != " + arr2.length);
        return action -> range(0, arr1.length).forEach(i -> action.accept(arr1[i], arr2[i]));
    }
}
