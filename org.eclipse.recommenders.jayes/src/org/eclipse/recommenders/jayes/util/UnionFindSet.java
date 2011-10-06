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
 * Disjoint Set Datastructure, uses Union by Rank and Path Compression
 * heuristics
 * 
 * @author Michael
 * 
 */
public class UnionFindSet {

    private UnionFindSet parent = this;
    private int rank = 0;

    /**
     * returns the set that contains this element
     * 
     * @return
     */
    public UnionFindSet find() {
        if (parent == this) {
            return this;
        }
        parent = parent.find();
        return parent;
    }

    /**
     * Unites the sets. In equal rank case, the other set is chosen as the new
     * root.
     * 
     * @param other
     */
    public void merge(final UnionFindSet other) {
        final UnionFindSet root = find();
        final UnionFindSet root2 = other.find();
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
     * upper bound for the height of the tree rooted at this element
     * 
     * @return
     */
    public int rank() {
        return rank;
    }

    public static UnionFindSet[] createArray(int size) {
        final UnionFindSet[] sets = new UnionFindSet[size];
        for (int i = 0; i < sets.length; i++) {
            sets[i] = new UnionFindSet();
        }
        return sets;
    }

}
