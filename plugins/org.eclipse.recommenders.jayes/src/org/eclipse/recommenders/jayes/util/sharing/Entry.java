/*******************************************************************************
 * Copyright (c) 2012 Michael Kutschke.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Michael Kutschke - initial API and implementation
 ******************************************************************************/
package org.eclipse.recommenders.jayes.util.sharing;

public abstract class Entry<T> {
    public T entry;
    private int hashcode;

    public Entry(final T entry) {
        this.entry = entry;
        this.hashcode = computeHash(entry);
    }

    protected abstract int computeHash(T entry2);

	@Override
    public int hashCode() {
        return hashcode;
    }

    @SuppressWarnings("unchecked")
	@Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof Entry)) {
            return false;
        }
        Object otherEntry = ((Entry<?>) obj).entry;
        if(entry.getClass() != otherEntry.getClass()) return false;
        return equals(entry, (T) otherEntry);
    }

	protected abstract boolean equals(T entry2, T entry3);

}
