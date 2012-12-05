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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.recommenders.jayes.factor.AbstractFactor;

public final class AddressCalc {

	private AddressCalc(){
		
	}
	
    public static void incrementMultiDimensionalCounter(final int[] counter, final int[] dimensions, final int dim) {
        counter[dim]++;
        if (counter[dim] == dimensions[dim]) {
            // overflow, assume the counter was valid before and less than the
            // maximal counter
            counter[dim] = 0;
            incrementMultiDimensionalCounter(counter, dimensions, dim - 1);
        }
    }

    public static Map<Integer, Integer> computeIdToDimensionIndexMap(AbstractFactor factor) {
        Map<Integer, Integer> foreignIds = new HashMap<Integer, Integer>();
        for (int i = 0; i < factor.getDimensionIDs().length; i++) {
            foreignIds.put(factor.getDimensionIDs()[i], i);
        }
        return foreignIds;
    }

}