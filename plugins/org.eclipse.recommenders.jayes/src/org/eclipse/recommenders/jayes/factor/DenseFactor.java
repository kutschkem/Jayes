/**
 * Copyright (c) 2011 Michael Kutschke. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * Michael Kutschke - initial API and implementation.
 */
package org.eclipse.recommenders.jayes.factor;

import java.util.Map;

import org.eclipse.recommenders.jayes.util.AddressCalc;
import org.eclipse.recommenders.jayes.util.IArrayWrapper;

public class DenseFactor extends AbstractFactor {

	@Override
	public void fill(double d) {
		values.fill(d);
	}

	/**
	 * @param sumDimensionID
	 *            -1 for last dimension (default)
	 * @return
	 */
	@Override
	public double[] sum(int sumDimensionID) {
		if (sumDimensionID == -1) {
			sumDimensionID = dimensionIDs[dimensionIDs.length - 1];
		}
		int sumDimension = getDimensionFromID(sumDimensionID);
		double[] result = new double[dimensions[sumDimension]];
		sumDim(sumDimension, result);
		return result;
	}

	private void sumDim(int sumDimension, double[] acc) {
		validateCut();
		int divisor = 1;
		for (int i = dimensions.length - 1; i > sumDimension; i--) {
			divisor *= dimensions[i];
		}
		sumToBucket(cut, 0, divisor, acc);

	}

	private void sumToBucket(Cut cut, int offset, int divisor, double[] result) {
		if (cut.getSubCut() == null) {
			int last = cut.getIndex() + offset + cut.getLength();
			for (int i = cut.getIndex() + offset; i < last; i += cut.getStepSize()) {
				result[(i / divisor) % result.length] += values.getDouble(i);
			}
		} else {
			Cut c = cut.getSubCut();
			for (int i = 0; i < cut.getLength(); i += cut.getSubtreeStepsize()) {
				sumToBucket(c, offset + i, divisor, result);
			}
		}
	}

	@Override
	protected int getRealPosition(int virtualPosition) {
		//for non-sparse factors, no address translation needs to be done
		return virtualPosition;
	}

	@Override
	protected int computeLength() {
		return values.length();
	}

	/**
	 * prepares multiplication by precomputing the corresponding array positions in the compatible
	 * Factor
	 * 
	 * @param compatible
	 *            a factor that has a subset of the dimensions of this factor
	 * @return
	 */
	@Override
	public int[] prepareMultiplication(AbstractFactor compatible) {
		int[] positions = new int[values.length()];
		int[] counter = new int[dimensions.length];
		Map<Integer, Integer> foreignIdToIndex = AddressCalc.computeIdToDimensionIndexMap(compatible);
		counter[counter.length - 1] = -1;
		for (int i = 0; i < values.length(); i++) {
			AddressCalc.incrementMultiDimensionalCounter(counter, dimensions,
					dimensions.length - 1);
			positions[i] = computeForeignPosition(compatible, counter,
					foreignIdToIndex);
		}
		return positions;
	}

	@Override
	public void copyValues(IArrayWrapper arrayWrapper) {
		validateCut();
		values.arrayCopy(arrayWrapper, cut.getIndex(), cut.getIndex(),
				cut.getLength());
	}

	@Override
	public int getOverhead() {
		return 0;
	}

}
