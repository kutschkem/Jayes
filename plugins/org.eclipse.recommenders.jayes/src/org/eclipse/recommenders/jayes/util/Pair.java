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

/**
 * simple pair class
 * 
 * @author Michael
 * 
 * @param <T1>
 * @param <T2>
 */
public class Pair<T1, T2> {
    private final T1 o1;
    private final T2 o2;

    public Pair(final T1 o1, final T2 o2) {
        this.o1 = o1;
        this.o2 = o2;
    }

    public T1 getFirst() {
        return o1;
    }

    public T2 getSecond() {
        return o2;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof Pair)) {
            return false;
        }
        final Pair<?, ?> p = (Pair<?, ?>) obj;
        return bothNullOrEqual(p.o1, this.o1) && bothNullOrEqual(p.o2, this.o2);
    }

    protected static boolean bothNullOrEqual(final Object o1, final Object o2) {
        return o1 == null ? o2 == null : o1.equals(o2);
    }

    @Override
    public String toString() {
        return "Pair{" + o1 + ", " + o2 + "}";
    }

    @Override
    public int hashCode() {
        return o1.hashCode() + 67 * o2.hashCode();
    }

    public static <S1, S2> Pair<S1, S2> newPair(S1 o1, S2 o2) {
        return new Pair<S1, S2>(o1, o2);
    }

}
