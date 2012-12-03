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
package org.eclipse.recommenders.jayes.util.sharing;

import java.util.Arrays;

/**
 * This class uses the Arrays.hashCode and Arrays.equals methods as basis for the
 * equality relation between it's members.
 * @author michael
 *
 */
public final class CanonicalIntArraySet extends CanonicalSet<int[]> {


    @Override
	protected Entry<int[]> createEntry(int[] array) {
		return new IntArrayEntry(array);
	}

	@Override
	protected boolean hasProperType(Object o) {
		return o instanceof int[];
	}

	private static class IntArrayEntry extends Entry<int[]>  {
	
		public IntArrayEntry(int[] entry) {
			super(entry);
		}
	
		@Override
		protected int computeHash(int[] entry2) {
			return Arrays.hashCode(entry2);
		}
	
		@Override
		protected boolean equals(int[] entry2, int[] entry3) {
			return Arrays.equals(entry2,entry3);
		}
	
	
	}

}
