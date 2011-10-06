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
package org.eclipse.recommenders.jayes.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public final class DeepIntArrayHashSet implements Set<int[]> {

    private final HashMap<Entry, int[]> set = new HashMap<Entry, int[]>();

    public DeepIntArrayHashSet() {
    }

    public DeepIntArrayHashSet(final List<int[]> l) {
        for (final int[] le : l) {
            add(le);
        }
    }

    public int[] get(final int[] example) {
        final Entry entry = new Entry(example);
        final int[] inst = set.get(entry);
        if (Arrays.equals(example, inst)) {
            return inst;
        }
        // Array has changed - remove it
        set.remove(entry);
        return null;
    }

    @Override
    public boolean add(final int[] e) {
        return set.put(new Entry(e), e) == null;
    }

    @Override
    public boolean addAll(final Collection<? extends int[]> c) {
        boolean changed = false;
        for (final int[] i : c) {
            changed = add(i) || changed; // beware of shortcut
        }
        return changed;
    }

    @Override
    public void clear() {
        set.clear();

    }

    @Override
    public boolean contains(final Object o) {
        return o instanceof int[] ? set.containsKey(new Entry((int[]) o)) : false;
    }

    @Override
    public boolean containsAll(final Collection<?> c) {
        for (final Object o : c) {
            if (!contains(o)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isEmpty() {
        return set.isEmpty();
    }

    @Override
    public Iterator<int[]> iterator() {
        return set.values().iterator();
    }

    @Override
    public boolean remove(final Object o) {
        if (!(o instanceof int[])) {
            return false;
        }
        return set.remove(new Entry((int[]) o)) != null;
    }

    @Override
    public boolean removeAll(final Collection<?> c) {
        return set.values().removeAll(c);
    }

    @Override
    public boolean retainAll(final Collection<?> c) {
        return set.values().retainAll(c);
    }

    @Override
    public int size() {
        return set.size();
    }

    @Override
    public Object[] toArray() {
        return set.values().toArray();
    }

    @Override
    public <T> T[] toArray(final T[] a) {
        return set.values().toArray(a);
    }

    private static class Entry {
        public int[] entry;
        private int hashcode;

        public Entry(final int[] entry) {
            this.entry = entry;
            this.hashcode = Arrays.hashCode(entry);
        }

        @Override
        public int hashCode() {
            return hashcode;
        }

        @Override
        public boolean equals(final Object obj) {
            if (!(obj instanceof Entry)) {
                return false;
            }
            return Arrays.equals(entry, ((Entry) obj).entry);
        }

    }

}
