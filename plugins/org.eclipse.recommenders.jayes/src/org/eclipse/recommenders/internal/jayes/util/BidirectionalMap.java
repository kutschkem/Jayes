/**
 * Copyright (c) 2011 Michael Kutschke.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Michael Kutschke - initial API and implementation.
 */
package org.eclipse.recommenders.internal.jayes.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class BidirectionalMap<K, V> implements Map<K, V> {

    private Map<K, V> keyValue = new HashMap<K, V>();
    private Map<V, K> valueKey = new HashMap<V, K>();

    public BidirectionalMap() {
    }

    @Override
    public int size() {
        return keyValue.size();
    }

    @Override
    public boolean isEmpty() {
        return keyValue.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return keyValue.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return valueKey.containsKey(value);
    }

    @Override
    public V get(Object key) {
        return keyValue.get(key);
    }

    public K getKey(V value) {
        return valueKey.get(value);
    }

    @Override
    public V put(K key, V value) {
        if (containsValue(value) && valueKey.get(value) != key)
            throw new IllegalArgumentException(value + " has already been assigned an other key, violating uniqueness");
        V val = keyValue.put(key, value);
        valueKey.remove(val);
        valueKey.put(value, key);
        return val;
    }

    @Override
    public V remove(Object key) {
        V v = keyValue.remove(key);
        valueKey.remove(v);
        return v;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        for (Entry<? extends K, ? extends V> ent : m.entrySet()) {
            put(ent.getKey(), ent.getValue());
        }

    }

    @Override
    public void clear() {
        keyValue.clear();
        valueKey.clear();

    }

    @Override
    public Set<K> keySet() {
        return keyValue.keySet();
    }

    @Override
    public Collection<V> values() {
        return valueKey.keySet();
    }

    @Override
    public Set<java.util.Map.Entry<K, V>> entrySet() {
        return keyValue.entrySet();
    }

}
