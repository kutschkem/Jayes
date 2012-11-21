/**
 * Copyright (c) 2012 Michael Kutschke. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * Michael Kutschke - initial API and implementation.
 */
package org.eclipse.recommenders.jayes;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.recommenders.jayes.util.AddressCalc;
import org.eclipse.recommenders.jayes.util.ArrayUtils;
import org.eclipse.recommenders.jayes.util.IArrayWrapper;
import org.eclipse.recommenders.jayes.util.MathUtils;

public class SparseFactor extends Factor {

	/**
	 * blockSize values in the original array correspond to one block pointer
	 */
	private int blockSize;
	private int[] blockPointers;
	private boolean isSparse = false;

	@Override
	public void setDimensions(int[] dimensions) {
		this.dimensions = Arrays.copyOf(dimensions, dimensions.length);
		selections = new int[dimensions.length];
		resetSelections();
		setDimensionIDs(Arrays.copyOf(getDimensionIDs(), dimensions.length));
		// dont set the value array!
	}

	@Override
	public void copyValues(IArrayWrapper other) {
		if(isSparse){
			validateCut();
			int offset = getRealPosition(cut.getIndex());
			// we don't know how many values need to be copied, thus copy everything until the end
			int length = other.length() - offset;
		
			values.arrayCopy(other, offset, offset, length);
		}else{
			super.copyValues(other);
		}
	}

	public void multiplyCompatible(Factor compatible) {
		if (values.length() == 1) {
			createSparseValueArray();
			fill(1);
			for (int i = 0; i < getSparseness(); i++) {
				values.assign(i, 0);
			}
		}
		int[] positions = prepareMultiplication(compatible);
		multiplyPrepared(compatible.values, positions);
	}

	@Override
	public int[] prepareMultiplication(Factor compatible) {
		if (!isSparse) return super.prepareMultiplication(compatible);

		int[] positions = new int[values.length()];
		int[] counter = new int[dimensions.length];
		Map<Integer, Integer> foreignIdToIndex = AddressCalc.computeIdToDimensionIndexMap(compatible);
		counter[counter.length - 1] = -1;
		for (int i = 0; i < blockPointers.length; i++) {
			for (int j = 0; j < getSparseness(); j++) {
				if (i * getSparseness() + j >= computeLength()) break;
				AddressCalc.incrementMultiDimensionalCounter(counter,
						dimensions, dimensions.length - 1);
				if (blockPointers[i] != 0) {
					int pos = computeForeignPosition(compatible, counter,
							foreignIdToIndex);
					positions[getRealPosition(i * getSparseness() + j)] = pos;
				}
			}
		}
		return positions;
	}

	private void createSparseValueArray() {
		// need to treat 0-dimensional factors specially
		if (dimensions.length == 0) {
			blockPointers = new int[0];
			values.newArray(1);
			return;
		}

		if (blockPointers == null) {
			blockPointers = new int[0];
		}

		int nonSparse = 0;
		for (int i = 0; i < blockPointers.length; i++) {
			if (blockPointers[i] != 0) {
				nonSparse++;
			}
		}
		if (nonSparse == blockPointers.length) {
			blockPointers = new int[0];
			values.newArray(computeLength());
			return;
		} else {
			isSparse = true;
			values.newArray((nonSparse + 1) * getSparseness());
		}
	}

	/**
	 * Prepares the factor in the sense that it's internal structures are optimized according to the
	 * zero/non-zero structure of the factors that will be multiplied in. This method should
	 * <strong>always</strong> and <strong>only</strong> be called <strong>once before</strong> any
	 * call that modifies this factor's values
	 * 
	 * @param compatible
	 *            a Factor with compatible dimensions
	 */
	public void sparsify(List<Factor> compatible) {
		optimizeDimensionOrder(compatible);
		blockSize = computeOptimalSparseness(compatible);
		if (blockSize != -1) {
			System.out.println("Sparseness: " + blockSize);
		}
		if (blockSize == -1) {
			// HACKHACK cue for createSparseValueArray to make factor nonsparse
			blockPointers = new int[1];
			blockPointers[0] = 1;
			return;
		}
		int length = computeLength();
		blockPointers = new int[length / getSparseness()
				+ (length % getSparseness() == 0 ? 0 : 1)];
		Arrays.fill(blockPointers, Integer.MIN_VALUE);
		Map<Factor, Map<Integer, Integer>> foreignIdToIndex = computeIDToIndexMaps(compatible);
		int[] counter = new int[dimensions.length];
		counter[counter.length - 1] = -1;
		int triesize = 0;
		for (int i = 0; i < blockPointers.length; i++) {
			boolean isZero = checkIfPartitionIsZero(i, counter, compatible,
					foreignIdToIndex, getSparseness());
			if (isZero || blockPointers[i] == 0) {
				blockPointers[i] = 0;
			} else {
				triesize++;
				blockPointers[i] = triesize * getSparseness();
			}
		}
		System.out.println("Sparse entries: "
				+ ((blockPointers.length - triesize) * getSparseness()));
	}

	private void optimizeDimensionOrder(List<Factor> compatible) {
		int[][] zerosByDimension = countZerosByDimension(compatible);
		final double[] infogain = computeInfoGain(zerosByDimension);
		Integer[] indices = ArrayUtils.indexArray(infogain);
		Arrays.sort(indices, new Comparator<Integer>() {

			@Override
			public int compare(Integer o1, Integer o2) {
				return -Double.compare(infogain[o1], infogain[o2]);
			}

		});

		if (!Arrays.equals(indices, ArrayUtils.indexArray(infogain))) {
			System.out.println("Reordering variables...");
			System.out.println("Infogain: " + Arrays.toString(infogain));
		}

		setDimensionIDs(ArrayUtils.permute(getDimensionIDs(), indices));
		setDimensions(ArrayUtils.permute(getDimensions(), indices));
	}

	private double[] computeInfoGain(int[][] zerosByDimension) {
		int totalZeros = 0;
		for (int i = 0; i < zerosByDimension[0].length; i++) {
			totalZeros += zerosByDimension[0][i];
		}

		double entropy = MathUtils.entropy(totalZeros, computeLength());
		double[] conditionalEntropy = conditionalEntropy(zerosByDimension);

		for (int i = 0; i < conditionalEntropy.length; i++) {
			conditionalEntropy[i] = entropy - conditionalEntropy[i];
		}

		return conditionalEntropy;
	}

	private double[] conditionalEntropy(int[][] zerosByDimension) {
		double[] entropyValues = new double[zerosByDimension.length];
		int length = computeLength();
		for (int i = 0; i < zerosByDimension.length; i++) {
			int l = length / dimensions[i];
			for (int j = 0; j < zerosByDimension[i].length; j++) {
				entropyValues[i] += MathUtils.entropy(zerosByDimension[i][j], l);
			}
			entropyValues[i] /= zerosByDimension[i].length;
		}
		return entropyValues;
	}

	private int[][] countZerosByDimension(List<Factor> compatible) {
		int[][] zeros = new int[dimensions.length][];
		for (int i = 0; i < zeros.length; i++) {
			zeros[i] = new int[dimensions[i]];
		}

		Map<Factor, Map<Integer, Integer>> foreignIdToIndex = computeIDToIndexMaps(compatible);
		int[] counter = new int[dimensions.length];
		counter[counter.length - 1] = -1;
		int length = computeLength();
		for (int i = 0; i < length; i++) {
			boolean isZero = checkIfPartitionIsZero(i, counter, compatible,
					foreignIdToIndex, 1);
			if (isZero) {
				for (int j = 0; j < dimensions.length; j++) {
					zeros[j][counter[j]]++;
				}
			}
		}

		return zeros;
	}

	private Map<Factor, Map<Integer, Integer>> computeIDToIndexMaps(
			List<Factor> compatible) {
		Map<Factor, Map<Integer, Integer>> foreignIdToIndex = new HashMap<Factor, Map<Integer, Integer>>();
		for (Factor f : compatible) {
			foreignIdToIndex.put(f, AddressCalc.computeIdToDimensionIndexMap(f));
		}
		return foreignIdToIndex;
	}

	private boolean checkIfPartitionIsZero(final int partition, int[] counter,
			List<Factor> compatible,
			Map<Factor, Map<Integer, Integer>> foreignIdToIndex, final int blockSize) {
		boolean isZero = true;
		for (int j = 0; j < blockSize; j++) {
			if (partition * blockSize + j >= computeLength()) break;
			AddressCalc.incrementMultiDimensionalCounter(counter, dimensions,
					dimensions.length - 1);
			boolean isNotZero = true;
			for (Factor f : compatible) {
				int pos = computeForeignPosition(f, counter,
						foreignIdToIndex.get(f));
					if (f.getValue(pos) == 0) {
						isNotZero = false;
					}
			}
			if (isNotZero) {
				isZero = false;
			}
		}
		return isZero;
	}

	private int computeOptimalSparseness(List<Factor> factor) {
		int arraySize;
		int overhead;
		int newOverhead;
		int newArraySize;
		int blocksize = 1;
		newArraySize = predictLengthOfValueArray(blocksize, factor) * values.sizeOfElement();
		newOverhead = (computeLength() / blocksize) * 4; // integer is 4 byte TODO: make this a constant dependent on Integer.SIZE
		do { // greedy strategy
			blocksize*=2;
			arraySize = newArraySize;
			overhead = newOverhead;
			newArraySize = predictLengthOfValueArray(blocksize, factor) * values.sizeOfElement();
			newOverhead = (computeLength() / blocksize) * 4;
		} while (newArraySize + newOverhead <= arraySize + overhead);
		
		int sparseMemory = overhead + arraySize;
		int denseMemory = computeLength() * values.sizeOfElement();
		if (sparseMemory >= denseMemory) return -1;
		return blocksize/2;
	}

	private int predictLengthOfValueArray(int blockSize, List<Factor> compatible) {
		Map<Factor, Map<Integer, Integer>> foreignIdToIndex = computeIDToIndexMaps(compatible);
		int[] counter = new int[dimensions.length];
		counter[counter.length - 1] = -1;
		int length = computeLength();
		int futureLength = length + blockSize;
		for (int i = 0; i < length / blockSize; i++) {
			boolean isZero = checkIfPartitionIsZero(i, counter, compatible,
					foreignIdToIndex, blockSize);
			if (isZero) {
				futureLength -= blockSize;
			}
		}
		return futureLength;
	}

	@Override
	protected int getRealPosition(int virtualPosition) {
		if(! isSparse) {
			return super.getRealPosition(virtualPosition);
		}
		return blockPointers[virtualPosition / getSparseness()] + (virtualPosition % getSparseness());
	}

	@Override
	public int computeLength() {
		return MathUtils.multiply(dimensions);
	}

	@Override
	public double[] sum(int sumDimensionID) {
		if (!isSparse) return super.sum(sumDimensionID);
		throw new UnsupportedOperationException("Not implemented");
	}

	@Override
	public void multiplyCompatibleToLog(Factor compatible) {
		if (values.length() == 1) {
			createSparseValueArray();
			fill(1);
		}
		super.multiplyCompatibleToLog(compatible);
	}

	@Override
	public void fill(double d) {
		if (values.length() == 1) {
			createSparseValueArray();
		}
		super.fill(d);
		if (isSparse) for (int i = 0; i < getSparseness(); i++) {
			values.assign(i, isLogScale() ? Double.NEGATIVE_INFINITY : 0);
		}
	}

	public boolean isSparse() {
		return isSparse;
	}

	public int getSparseness() {
		return blockSize;
	}

}
