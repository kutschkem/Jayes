/**
 * Copyright (c) 2012 Michael Kutschke.
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

import org.eclipse.recommenders.jayes.factor.arraywrapper.IArrayWrapper;

public class CanonicalArrayWrapperManager {
	
	
	private CanonicalSet<IArrayWrapper> canonicals = new CanonicalSet<IArrayWrapper>(){

		@Override
		protected Entry<IArrayWrapper> createEntry(IArrayWrapper array) {
			return new Entry<IArrayWrapper>(array){

				@Override
				protected int computeHash(IArrayWrapper array) {
					return Arrays.hashCode(array.toDoubleArray());
				}

				@Override
				protected boolean equals(IArrayWrapper array,
						IArrayWrapper array2) {
					return Arrays.equals(array.toDoubleArray(), array2.toDoubleArray());
				}
				
			};
		}

		@Override
		protected boolean hasProperType(Object o) {
			return o instanceof IArrayWrapper;
		}
		
	};

	public IArrayWrapper getInstance(IArrayWrapper array){
		if(! canonicals.contains(array)){
			canonicals.add(array);
		}
		return canonicals.get(array);
	}
	
}
