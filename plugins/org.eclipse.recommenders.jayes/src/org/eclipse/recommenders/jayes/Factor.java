/**
 * Copyright (c) 2011 Michael Kutschke. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * Michael Kutschke - initial API and implementation.
 */
package org.eclipse.recommenders.jayes;

import java.util.Arrays;
import java.util.Map;

import org.eclipse.recommenders.jayes.util.AddressCalc;
import org.eclipse.recommenders.jayes.util.IArrayWrapper;
import org.eclipse.recommenders.jayes.util.DoubleArrayWrapper;
import org.eclipse.recommenders.jayes.util.MathUtils;

public class Factor implements Cloneable {

	protected int[] dimensions = new int[0];
	private int[] dimensionIDs = new int[0];
	protected IArrayWrapper values = new DoubleArrayWrapper(new double[1]);
	protected int[] selections = new int[0];

	protected Cut cut = new Cut(this);
	private boolean isCutValid = false;

	private boolean isLogScale = false;

	public void setValues(IArrayWrapper values) {
		this.values = values;
		assert (MathUtils.multiply(dimensions) == values.length());
	}

	public IArrayWrapper getValues() {
		return values;
	}

	public double getValue(int i) {
		return values.getDouble(getRealPosition(i));
	}

	public void fill(double d) {
		values.fill(d);
	}

	public void setDimensions(int[] dimensions) {
		this.dimensions = Arrays.copyOf(dimensions, dimensions.length);
		selections = new int[dimensions.length];
		resetSelections();
		int length = MathUtils.multiply(dimensions);
		if (length > values.length()) values.set(new double[length]);
		dimensionIDs = Arrays.copyOf(dimensionIDs, dimensions.length);
	}

	public int[] getDimensions() {
		return dimensions;
	}

	/**
	 * tells the Factor which variables the dimensions correspond to. Uniqueness and consistency of
	 * size is assumed.
	 */
	public void setDimensionIDs(int[] ids) {
		dimensionIDs = ids.clone();
	}

	public int[] getDimensionIDs() {
		return dimensionIDs;
	}

	private int getDimensionFromID(int id) {
		for (int i = 0; i < dimensionIDs.length; i++)
			if (dimensionIDs[i] == id) return i;
		return -1;
	}

	public void select(int dimensionID, int index) {
		int dim = getDimensionFromID(dimensionID);
		if (selections[dim] != index) {
			selections[dim] = index;
			isCutValid = false;
		}
	}

	public void resetSelections() {
		Arrays.fill(selections, -1);
		isCutValid = false;
	}

	public void setLogScale(boolean isLogScale) {
		this.isLogScale = isLogScale;
	}

	public boolean isLogScale() {
		return isLogScale;
	}

	/**
	 * @param sumDimensionID
	 *            -1 for last dimension (default)
	 * @return
	 */
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

	/**
	 * multiply the factors. Only compatible factors are allowed, meaning ones that have a subset of
	 * the variables of this factor (assume consistent Dimension ID / size pairs
	 * 
	 * @param compatible
	 */
	public void multiplyCompatible(Factor compatible) {
		int[] positions = prepareMultiplication(compatible);
		multiplyPrepared(compatible.values, positions);
	}

	public void multiplyPrepared(IArrayWrapper compatibleValues, int[] positions) {
		validateCut();
		if (!isLogScale)
			multiplyPrepared(cut, 0, compatibleValues, positions);
		else
			multiplyPreparedLog(cut, 0, compatibleValues, positions);
	}

	private void multiplyPrepared(Cut cut, int offset,
			IArrayWrapper compatibleValues, int[] positions) {
		if (cut.getSubCut() == null) {
			int last = Math.min(computeLength(), cut.getLength() + cut.getIndex()
					+ offset);
			for (int i = cut.getIndex() + offset; i < last; i += cut.getStepSize()){
				int j = getRealPosition(i);
				values.mulAssign(j, compatibleValues, positions[j]);
			}
		} else {
			Cut c = cut.getSubCut();
			for (int i = 0; i < cut.getLength(); i += cut.getSubtreeStepsize()) {
				multiplyPrepared(c, offset + i, compatibleValues, positions);
			}
		}
	}

	public void sumPrepared(IArrayWrapper compatibleFactorValues,
			int[] preparedOperation) {
		validateCut();

		compatibleFactorValues.fill(0);

		if (!isLogScale)
			sumPrepared(cut, 0, compatibleFactorValues, preparedOperation);
		else
			sumPreparedLog(compatibleFactorValues, preparedOperation);

	}

	private void sumPrepared(Cut cut, int offset, IArrayWrapper compatibleFactorValues,
			int[] positions) {
		if (cut.getSubCut() == null) {
			int last = Math.min(computeLength(), cut.getLength() + cut.getIndex()
					+ offset);
			for (int i = cut.getIndex() + offset; i < last; i += cut.getStepSize()){
				int j = getRealPosition(i);
				compatibleFactorValues.addAssign(positions[j], values, j);
			}
		} else {
			Cut c = cut.getSubCut();
			for (int i = 0; i < cut.getLength(); i += cut.getSubtreeStepsize()) {
				sumPrepared(c, offset + i, compatibleFactorValues, positions);
			}
		}
	}

	private void sumPreparedLog(IArrayWrapper compatibleFactorValues, int[] positions) {
		double max = findMax(cut, 0, 0);
		sumPreparedLog(cut, 0, compatibleFactorValues, positions, max);
		for (int i = 0; i < compatibleFactorValues.length(); i++) {
			compatibleFactorValues.assign(i, Math.log(compatibleFactorValues.getDouble(i)) + max);
		}
	}

	private double findMax(Cut cut, int offset, double max) {
		if (cut.getSubCut() == null) {
			int last = Math.min(computeLength(), cut.getLength() + cut.getIndex()
					+ offset);
			for (int i = cut.getIndex() + offset; i < last; i += cut.getStepSize()) {
				int j = getRealPosition(i);
				if (values.getDouble(j) != Double.NEGATIVE_INFINITY
						&& Math.abs(values.getDouble(j)) > Math.abs(max)) {
					max = values.getDouble(j);
				}
			}
		} else {
			Cut c = cut.getSubCut();
			for (int i = 0; i < cut.getLength(); i += cut.getSubtreeStepsize()) {
				double pot = findMax(c, offset + i, max);
				if (pot != Double.NEGATIVE_INFINITY
						&& Math.abs(pot) > Math.abs(max)) {
					max = pot;
				}
			}
		}
		return max;
	}

	private void sumPreparedLog(Cut cut, int offset, IArrayWrapper compatibleFactorValues,
			int[] positions, double max) {
		if (cut.getSubCut() == null) {
			int last = Math.min(computeLength(), cut.getLength() + cut.getIndex()
					+ offset);
			for (int i = cut.getIndex() + offset; i < last; i += cut.getStepSize()){
				int j = getRealPosition(i);
				compatibleFactorValues.addAssign(positions[j], Math.exp(values.getDouble(j) - max));
			}
		} else {
			Cut c = cut.getSubCut();
			for (int i = 0; i < cut.getLength(); i += cut.getSubtreeStepsize()) {
				sumPreparedLog(c, offset + i, compatibleFactorValues, positions, max);
			}
		}
	}

	private void multiplyPreparedLog(Cut cut, int offset,
			IArrayWrapper compatibleValues, int[] positions) {
		if (cut.getSubCut() == null) {
			int last = Math.min(computeLength(), cut.getLength() + cut.getIndex()
					+ offset);
			for (int i = cut.getIndex() + offset; i < last; i += cut.getStepSize()){
				int j = getRealPosition(i);
				values.addAssign(j, compatibleValues, positions[j]);
			}
		} else {
			Cut c = cut.getSubCut();
			for (int i = 0; i < cut.getLength(); i += cut.getSubtreeStepsize()) {
				multiplyPreparedLog(c, offset + i, compatibleValues, positions);
			}
		}
	}

	protected void validateCut() {
		if (!isCutValid) {
			cut.initialize();
			isCutValid = true;
		}

	}

	protected int getRealPosition(int virtualPosition) {
		//for non-sparse factors, no address translation needs to be done
		return virtualPosition;
	}

	public int computeLength() {
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
	public int[] prepareMultiplication(Factor compatible) {
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

	protected int computeForeignPosition(Factor compatible, int[] counter,
			Map<Integer, Integer> foreignIdToIndex) {
		// special case: zero-dimensional factor
		if (compatible.dimensions.length == 0) {
			return 0;
		}
		int[] foreignPos = transformLocalToForeignPosition(counter,
				foreignIdToIndex);

		return AddressCalc.realAddr(compatible.getDimensions(), foreignPos);

	}

	private int[] transformLocalToForeignPosition(int[] localPosition,
			Map<Integer, Integer> foreignIdToIndex) {
		int[] foreignPosition = new int[foreignIdToIndex.size()];
		for (int i = 0; i < localPosition.length; i++) {
			Integer foreignDim = foreignIdToIndex.get(dimensionIDs[i]);
			if (foreignDim != null) // dimension present in the other Factor?
				foreignPosition[foreignDim] = localPosition[i];
		}
		return foreignPosition;
	}

	@Override
	public Factor clone() {
		Factor f = null;
		try {
			f = (Factor) super.clone();
		} catch (CloneNotSupportedException exception) {
			// should not be possible to happen
			exception.printStackTrace();
		}
		f.values = values.clone();
		f.selections = selections.clone();
		f.cut = new Cut(f);
		f.isCutValid = false;
		return f;
	}

	public void multiplyCompatibleToLog(Factor factor) {
		int[] positions = prepareMultiplication(factor);
		for (int i = 0; i < values.length(); i++) {
			values.addAssign(i, Math.log(factor.values.getDouble(positions[i])));
		}

	}

	public void copyValues(IArrayWrapper arrayWrapper) {
		validateCut();
		values.arrayCopy(arrayWrapper, cut.getIndex(), cut.getIndex(),
				cut.getLength());
	}

}
