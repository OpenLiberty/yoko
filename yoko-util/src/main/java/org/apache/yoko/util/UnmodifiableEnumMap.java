/*
 * Copyright 2017 IBM Corporation and others.
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
package org.apache.yoko.util;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public abstract class UnmodifiableEnumMap<K extends Enum<K>, V> extends EnumMap<K, V> {
    private static final long serialVersionUID = 1L;

    public UnmodifiableEnumMap(Class<K> keyType) {
        super(keyType);
        // initialise all values up front to avoid races later
        for(K key : keyType.getEnumConstants())
            super.put(key, computeValueFor(key));
    }

    protected abstract V computeValueFor(K key);

    @Override
    public final V remove(Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final V put(K key, V value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void putAll(Map<? extends K, ? extends V> m) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final Set<K> keySet() {
        return Collections.unmodifiableSet(super.keySet());
    }

    @Override
    public final Collection<V> values() {
        return Collections.unmodifiableCollection(super.values());
    }
}
