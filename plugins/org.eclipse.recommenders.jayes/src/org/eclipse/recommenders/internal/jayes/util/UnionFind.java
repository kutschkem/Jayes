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

/**
 * Disjoint Set Datastructure, uses Union by Rank and Path Compression heuristics <br/>
 * <br/>
 * See "Efficiency of a Good But Not Linear Set Union Algorithm" (Tarjan, 1975)
 * 
 */
public class UnionFind {

    private UnionFind parent = this;
    private int rank = 0;

    /**
     * @return the set that contains this element
     */
    public UnionFind find() {
        if (parent == this) {
            return this;
        }
        parent = parent.find();
        return parent;
    }

    /**
     * Unites the sets. In equal rank case, the other set is chosen as the new root.
     */
    public void merge(final UnionFind other) {
        final UnionFind root = find();
        final UnionFind root2 = other.find();
        if (root == root2) {
            return;
        }
        if (root.rank == root2.rank) {
            root2.rank++;
        }
        if (root.rank > root2.rank) {
            root2.parent = root;
        } else {
            root.parent = root2;
        }
    }

    /**
     * @return upper bound for the height of the tree rooted at this element
     */
    public int rank() {
        return rank;
    }

    public static UnionFind[] createArray(int size) {
        final UnionFind[] sets = new UnionFind[size];
        for (int i = 0; i < sets.length; i++) {
            sets[i] = new UnionFind();
        }
        return sets;
    }

}
