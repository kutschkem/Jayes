/**
 * Copyright (c) 2012 Michael Kutschke. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * Michael Kutschke - initial API and implementation.
 */
package org.eclipse.recommenders.jayes.factor;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.recommenders.jayes.util.AddressCalc;
import org.eclipse.recommenders.jayes.util.ArrayUtils;
import org.eclipse.recommenders.jayes.util.MathUtils;
import org.eclipse.recommenders.jayes.util.arraywrapper.IArrayWrapper;

public class SparseFactor extends AbstractFactor {

	/**
	 * blockSize values in the original value array correspond to one block pointer
	 */
	private int blockSize;
	private int[] blockPointers;

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
		validateCut();
		int offset = getRealPosition(cut.getIndex());
		// we don't know how many values need to be copied, thus copy everything until the end
		int length = other.length() - offset;;
	
		values.arrayCopy(other, offset, offset, length);
	}

	public void multiplyCompatible(AbstractFactor compatible) {
		int[] positions = prepareMultiplication(compatible);
		multiplyPrepared(compatible.values, positions);
	}

	@Override
	public int[] prepareMultiplication(AbstractFactor compatible) {
		int[] positions = new int[values.length()];
		int[] counter = new int[dimensions.length];
		Map<Integer, Integer> foreignIdToIndex = AddressCalc.computeIdToDimensionIndexMap(compatible);
		counter[counter.length - 1] = -1;
		for (int i = 0; i < blockPointers.length; i++) {
			for (int j = 0; j < blockSize; j++) {
				if (i * blockSize + j >= computeLength()) break;
				AddressCalc.incrementMultiDimensionalCounter(counter,
						dimensions, dimensions.length - 1);
				if (blockPointers[i] != 0) {
					int pos = computeForeignPosition(compatible, counter,
							foreignIdToIndex);
					positions[getRealPosition(i * blockSize + j)] = pos;
				}
			}
		}
		return positions;
	}

	private void createSparseValueArray() {
		int nonSparse = countNonzeroBlocks();
		values.newArray((nonSparse + 1) * blockSize);
	}

	private int countNonzeroBlocks() {
		int nonSparse = 0;
		for (int i = 0; i < blockPointers.length; i++) {
			if (blockPointers[i] != 0) {
				nonSparse++;
			}
		}
		return nonSparse;
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
	/*
	 * can't put this in a constructor because we already need full information about the dimensions here
	 */
	public void sparsify(List<AbstractFactor> compatible) {
		optimizeDimensionOrder(compatible);
		blockSize = computeOptimalSparseness(compatible);
		if (blockSize != -1) {
			System.out.println("Sparseness: " + blockSize);
		}
		int length = computeLength();
		if (blockSize == -1) {
			// this will result in a non-sparse factor with a minimum of overhead
			// still, avoid using a SparseFactor in this case
			blockSize = (int) Math.sqrt(length);
		}
		blockPointers = new int[(int) Math.ceil((double)length / blockSize)];
		Map<AbstractFactor, Map<Integer, Integer>> foreignIdToIndex = computeIDToIndexMaps(compatible);
		int[] counter = new int[dimensions.length];
		counter[counter.length - 1] = -1;
		int numberOfNonzeroBlocks = 0;
		for (int i = 0; i < blockPointers.length; i++) {
			boolean isZero = checkIfPartitionIsZero(i, counter, compatible,
					foreignIdToIndex, blockSize);
			if (isZero) {
				blockPointers[i] = 0;
			} else {
				numberOfNonzeroBlocks++;
				blockPointers[i] = numberOfNonzeroBlocks * blockSize;
			}
		}
		createSparseValueArray();
	}

	private void optimizeDimensionOrder(List<AbstractFactor> compatible) {
		if(dimensions.length < 2) {
			// countZerosByDimension breaks for 0-dimensional factors
			// and there is not point in optimizing the order if there is only one dimension
			return;
		}
		int[][] zerosByDimension = countZerosByDimension(compatible);
		final double[] infogain = computeInfoGain(zerosByDimension);

		setDimensionIDs(sortByKey(infogain,getDimensionIDs()));
		setDimensions(sortByKey(infogain,getDimensions()));
	}

	private int[] indexArray(int length) {
		int[] indices = new int[length];
		for (int i = 0; i < length; i++) {
			indices[i] = i;
		}
		return indices;
	}
	
	// sorts in descending order of the keys
	private int[] sortByKey(final double[] key, int[] array){
		int[] permutation = sort(indexArray(key.length), 
				new Comparator<Integer>() {

			@Override
			public int compare(Integer o1, Integer o2) {
				return -Double.compare(key[o1], key[o2]);
			}

		});
		return permute(array,permutation);
	}

	private int[] sort(int[] array, Comparator<Integer> comparator){
		Integer[] boxed = ArrayUtils.boxArray(array);
		Arrays.sort(boxed, comparator);
		return (int[]) ArrayUtils.unboxArray(boxed);
	}

	private int[] permute(int[] array, int[] permutation) {
		int[] array2 = new int[array.length];
		for (int i = 0; i < array.length; i++) {
			array2[i]=  array[permutation[i]];
		}
		
		return array2;
	}

	private double[] computeInfoGain(int[][] zerosByDimension) {
		int totalZeros = 0;
		for (int i = 0; i < zerosByDimension[0].length; i++) {
			totalZeros += zerosByDimension[0][i];
		}

		double entropy = entropy(totalZeros, computeLength() - totalZeros);
		double[] conditionalEntropy = conditionalEntropy(zerosByDimension);

		for (int i = 0; i < conditionalEntropy.length; i++) {
			conditionalEntropy[i] = entropy - conditionalEntropy[i];
		}

		return conditionalEntropy;
	}

	/**
	 * compute the entropy of a binary class distribution
	 */
	private double entropy(int classOne, int classTwo) {
	    double p = classOne / (double) (classOne + classTwo);
	    if (p == 0 || p == 1) {
	        return 0;
	    }
	    return -(p * Math.log(p) + (1 - p) * Math.log(1 - p));
	}

	private double[] conditionalEntropy(int[][] zerosByDimension) {
		double[] entropyValues = new double[dimensions.length];
		int length = computeLength();
		for (int i = 0; i < dimensions.length; i++) {
			int l = length / dimensions[i];
			for (int j = 0; j < dimensions[i]; j++) {
				int zeroes = zerosByDimension[i][j];
				entropyValues[i] += entropy(zeroes, l - zeroes);
			}
			entropyValues[i] /= dimensions[i];
			//for the ends of compression, use a uniform distribution for the outcomes
		}
		return entropyValues;
	}

	private int[][] countZerosByDimension(List<AbstractFactor> compatible) {
		int[][] zeros = new int[dimensions.length][];
		for (int i = 0; i < zeros.length; i++) {
			zeros[i] = new int[dimensions[i]];
		}

		Map<AbstractFactor, Map<Integer, Integer>> foreignIdToIndex = computeIDToIndexMaps(compatible);
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

	private Map<AbstractFactor, Map<Integer, Integer>> computeIDToIndexMaps(
			List<AbstractFactor> compatible) {
		Map<AbstractFactor, Map<Integer, Integer>> foreignIdToIndex = new HashMap<AbstractFactor, Map<Integer, Integer>>();
		for (AbstractFactor f : compatible) {
			foreignIdToIndex.put(f, AddressCalc.computeIdToDimensionIndexMap(f));
		}
		return foreignIdToIndex;
	}

	private boolean checkIfPartitionIsZero(final int partition, int[] counter,
			List<AbstractFactor> compatible,
			Map<AbstractFactor, Map<Integer, Integer>> foreignIdToIndex, final int blockSize) {
		boolean isZero = true;
		for (int j = 0; j < blockSize; j++) {
			if (partition * blockSize + j >= computeLength()) break;
			AddressCalc.incrementMultiDimensionalCounter(counter, dimensions,
					dimensions.length - 1);
			boolean isNotZero = true;
			for (AbstractFactor f : compatible) {
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

	private int computeOptimalSparseness(List<AbstractFactor> compatible) {
		if(dimensions.length == 0){
			// zero dimensions -> exactly one value, no need to optimize
			// (also predictLengthOfValueArray breaks for zero dimensions)
			return -1;
		}
		int arraySize;
		int overhead;
		int newOverhead;
		int newArraySize;
		int blocksize = 1;
		newArraySize = predictLengthOfValueArray(blocksize, compatible) * values.sizeOfElement();
		newOverhead = (computeLength() / blocksize) * 4; // integer is 4 byte TODO: make this a constant dependent on Integer.SIZE
		do { // greedy strategy
			//TODO enhance with second stage of searching by binary search
			blocksize*=2;
			arraySize = newArraySize;
			overhead = newOverhead;
			newArraySize = predictLengthOfValueArray(blocksize, compatible) * values.sizeOfElement();
			newOverhead = (computeLength() / blocksize) * 4;
		} while (newArraySize + newOverhead <= arraySize + overhead);
		
		int sparseMemory = arraySize + overhead;
		int denseMemory = computeLength() * values.sizeOfElement() + 2* (int) Math.sqrt(computeLength());
		if (sparseMemory >= denseMemory) return -1;
		return blocksize/2;
	}

	private int predictLengthOfValueArray(int blockSize, List<AbstractFactor> compatible) {
		Map<AbstractFactor, Map<Integer, Integer>> foreignIdToIndex = computeIDToIndexMaps(compatible);
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
		return blockPointers[virtualPosition / blockSize] + (virtualPosition % blockSize);
	}

	@Override
	protected int computeLength() {
		return MathUtils.product(dimensions);
	}

	@Override
	public double[] sum(int sumDimensionID) {
		throw new UnsupportedOperationException("Not implemented");
	}

	@Override
	public void fill(double d) {
		values.fill(d);
		for (int i = 0; i < blockSize; i++) {
			values.set(i, isLogScale() ? Double.NEGATIVE_INFINITY : 0);
		}
	}
	
	@Override
	public int getOverhead(){
		return blockPointers.length * 4;
	}
	
	@Override
	public SparseFactor clone(){
		return (SparseFactor) super.clone();
	}
	
	
	/**
	 * approximates whether creating a sparse factor will save memory compared to a
	 * dense factor
	 * @param futureLength the length that the new factor's value array would have in a dense factor
	 * @param multiplicationCandidates
	 * @return
	 */
	public static boolean isSuitable(int futureLength, List<AbstractFactor> multiplicationCandidates){
		if(multiplicationCandidates == null){
			return false;
		}
		for(AbstractFactor f: multiplicationCandidates){
			if(isSparseEnough(f,futureLength))
				return true;
		}
		return false;
	}

	/*
	 * the minimal overhead is 2*sqrt(length).
	 * The formula follows from (futureL. / f.length) * nbrOfZ > 2 * sqrt(futureL.)
	 */
	private static boolean isSparseEnough(AbstractFactor f, int futureLength) {
		return countZeros(f.getValues()) > 2 * f.computeLength() / Math.sqrt(futureLength);
	}

	private static double countZeros(IArrayWrapper values) {
		int result = 0;
		for(double d: values.toDoubleArray()){
			if(d == 0){
				result ++;
			}
		}
		return result;
	}

}
