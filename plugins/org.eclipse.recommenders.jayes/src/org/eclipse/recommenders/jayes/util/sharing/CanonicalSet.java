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

import java.util.AbstractSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Set to support managing canonical, shared instances. These instances should be
 * immutable while in the set. Call get(T) to get a canonical instance for the specified
 * key.
 *
 * @param <T>
 */
public abstract class CanonicalSet<T> extends AbstractSet<T> {

	private final HashMap<Entry<T>, T> set = new HashMap<Entry<T>, T>();

	public T get(final T example) {
	    final Entry<T> entry = createEntry(example);
	    final T inst = set.get(entry);
	    if (entry.equals(createEntry(inst))) {
	        return inst;
	    }
	    // Entry has changed - remove it
	    set.remove(entry);
	    return null;
	}

	protected abstract Entry<T> createEntry(T array);

	@Override
	public boolean add(final T e) {
		if(! contains(e)){
	    return set.put(createEntry(e), e) == null;
		}else
			return false;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean contains(final Object o) {
	    return hasProperType(o) ? set.containsKey(createEntry((T) o)) : false;
	}

	/**
	 * @return true iff o is of type T
	 */
	protected abstract boolean hasProperType(Object o);

	@Override
	public Iterator<T> iterator() {
	    return set.values().iterator();
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean remove(final Object o) {
	    if (!(hasProperType(o))) {
	        return false;
	    }
	    return set.remove(createEntry((T) o)) != null;
	}

	@Override
	public boolean retainAll(final Collection<?> c) {
	    throw new UnsupportedOperationException("Unimplemented");
	}

	@Override
	public int size() {
	    return set.size();
	}

}
