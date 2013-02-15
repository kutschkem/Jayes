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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.recommenders.jayes.factor.AbstractFactor;
import org.eclipse.recommenders.jayes.util.MathUtils;

public final class AddressCalc {

    private AddressCalc() {

    }

    public static void incrementMultiDimensionalCounter(final int[] counter, final int[] dimensions) {
        int dimension = counter.length - 1;

        counter[dimension]++;
        while (counter[dimension] == dimensions[dimension]) {
            // overflow, assume the counter was valid before and less than the
            // maximal counter
            counter[dimension] = 0;
            dimension--;
            counter[dimension]++;
        }
    }

    /**
     * computes a mapping from the factors' addresses to the corresponding index of the flat value array of a factor
     * consisting of the dimensions with the given dimensionIDs. dimensionIDs is expected to be a superset of the
     * dimension ids of the factor
     */
    public static int[] computeLinearMap(AbstractFactor factor, int... dimensionIDs) {
        return computeLinearMap(computeIdToDimensionIndexMap(factor), factor.getDimensions(),
                dimensionIDs);
    }

    private static int[] computeLinearMap(Map<Integer, Integer> foreignIdToIndex, int[] foreignDimensions,
            int[] dimensionIds) {
        int[] kernel = new int[dimensionIds.length];

        for (int i = 0; i < kernel.length; i++) {
            int dimensionId = dimensionIds[i];
            if (foreignIdToIndex.containsKey(dimensionId)) {
                kernel[i] = MathUtils.productOfRange(foreignDimensions, foreignIdToIndex.get(dimensionId) + 1,
                        foreignDimensions.length);
            }
        }

        return kernel;
    }

    private static Map<Integer, Integer> computeIdToDimensionIndexMap(AbstractFactor factor) {
        Map<Integer, Integer> foreignIds = new HashMap<Integer, Integer>();
        for (int i = 0; i < factor.getDimensionIDs().length; i++) {
            foreignIds.put(factor.getDimensionIDs()[i], i);
        }
        return foreignIds;
    }

}
