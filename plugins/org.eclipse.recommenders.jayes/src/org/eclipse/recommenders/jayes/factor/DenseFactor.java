/**
 * Copyright (c) 2011 Michael Kutschke. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * Michael Kutschke - initial API and implementation.
 */
package org.eclipse.recommenders.jayes.factor;

import org.eclipse.recommenders.internal.jayes.util.AddressCalc;
import org.eclipse.recommenders.jayes.factor.arraywrapper.IArrayWrapper;
import org.eclipse.recommenders.jayes.util.MathUtils;

public class DenseFactor extends AbstractFactor {

    @Override
    public void fill(double d) {
        values.fill(d);
    }

    @Override
    protected int getRealPosition(int virtualPosition) {
        //for non-sparse factors, no address translation needs to be done
        return virtualPosition;
    }

    /**
     * prepares multiplication by precomputing the corresponding array positions in the compatible Factor
     * 
     * @param compatible
     *            a factor that has a subset of the dimensions of this factor
     * @return
     */
    @Override
    public int[] prepareMultiplication(AbstractFactor compatible) {
        if (dimensions.length == 0) { //treat 0-dimensional factors specially
            return new int[] { 0 };
        }
        int[] positions = new int[values.length()];
        int[] counter = new int[dimensions.length];
        int[] localToForeignPosition = AddressCalc.computeLinearMap(compatible, dimensionIDs);
        counter[counter.length - 1] = -1;
        for (int i = 0; i < values.length(); i++) {
            AddressCalc.incrementMultiDimensionalCounter(counter, dimensions);
            positions[i] = compatible.getRealPosition(MathUtils.scalarProduct(counter,
                    localToForeignPosition));
        }
        return positions;
    }

    @Override
    public void copyValues(IArrayWrapper arrayWrapper) {
        validateCut();
        int index = cut.getStart();
        int length = Math.min(cut.getLength(), values.length() - index);
        values.arrayCopy(arrayWrapper, index, index,
                length);
    }

    @Override
    public int getOverhead() {
        return 0;
    }

}
