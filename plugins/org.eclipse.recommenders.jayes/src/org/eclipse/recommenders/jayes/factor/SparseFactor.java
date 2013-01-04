/**
 * Copyright (c) 2012 Michael Kutschke. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * Michael Kutschke - initial API and implementation.
 */
package org.eclipse.recommenders.jayes.factor;

import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.recommenders.jayes.util.AddressCalc;
import org.eclipse.recommenders.jayes.util.ArrayUtils;
import org.eclipse.recommenders.jayes.util.MathUtils;
import org.eclipse.recommenders.jayes.util.arraywrapper.IArrayWrapper;

public class SparseFactor extends AbstractFactor {

    private static final int SIZE_OF_INT = 4;
    /**
     * blockSize values in the original value array correspond to one block pointer
     */
    private int blockSize;
    private int[] blockPointers;

    @Override
    public void setDimensions(int... dimensions) {
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
        int length = other.length() - offset;

        values.arrayCopy(other, offset, offset, length);
    }

    @Override
    public void multiplyCompatible(AbstractFactor compatible) {
        int[] positions = prepareMultiplication(compatible);
        multiplyPrepared(compatible.values, positions);
    }

    @Override
    public int[] prepareMultiplication(AbstractFactor compatible) {
        if (dimensions.length == 0) {
            // treat 0-dimensional factors specially
            return new int[] { 0, 0 };
        }

        int[] positions = new int[values.length()];
        int[] counter = new int[dimensions.length];
        int[] positionTransformation = AddressCalc.computeLinearMap(compatible, dimensionIDs);
        counter[counter.length - 1] = -1;
        for (int i = 0; i < blockPointers.length; i++) {
            for (int j = 0; j < blockSize; j++) {
                if (i * blockSize + j >= computeLength())
                    break;
                AddressCalc.incrementMultiDimensionalCounter(counter,
                        dimensions);
                if (blockPointers[i] != 0) {
                    int pos = MathUtils.scalarProduct(counter, positionTransformation);
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
     * Prepares the factor in the sense that it's internal structures are optimized according to the zero/non-zero
     * structure of the factors that will be multiplied in. This method should <strong>always</strong> and
     * <strong>only</strong> be called <strong>once before</strong> any call that modifies this factor's values
     * 
     * @param compatible
     *            a Factor with compatible dimensions
     */
    /*
     * can't put this in a constructor because we already need full information about the dimensions here
     */
    public void sparsify(AbstractFactor... compatible) {
        if (dimensions.length == 0) {
            //treat 0-dimensional factors specially (many methods break for them)
            blockSize = 1;
            blockPointers = new int[] { 1 };
            createSparseValueArray();
            return;
        }
        assert dimensions.length > 0;
        optimizeDimensionOrder(compatible);
        optimizeBlockSize(compatible);
        System.out.println("Sparseness: " + blockSize);

        int length = computeLength();
        blockPointers = new int[(int) Math.ceil((double) length / blockSize)];
        int[][] posTransformations = computePositionTransformations(compatible);
        int[] counter = new int[dimensions.length];
        counter[counter.length - 1] = -1;
        int numberOfNonzeroBlocks = 0;
        for (int i = 0; i < blockPointers.length; i++) {
            boolean isZero = checkIfPartitionIsZero(i, counter, compatible,
                    posTransformations, blockSize);
            if (isZero) {
                blockPointers[i] = 0;
            } else {
                numberOfNonzeroBlocks++;
                blockPointers[i] = numberOfNonzeroBlocks * blockSize;
            }
        }
        createSparseValueArray();
    }

    private void optimizeDimensionOrder(AbstractFactor[] compatible) {
        int[][] zerosByDimension = countZerosByDimension(compatible);
        final double[] infogain = computeInfoGain(zerosByDimension);

        setDimensionIDs(sortByKey(infogain, getDimensionIDs()));
        setDimensions(sortByKey(infogain, getDimensions()));
    }

    private int[] indexArray(int length) {
        int[] indices = new int[length];
        for (int i = 0; i < length; i++) {
            indices[i] = i;
        }
        return indices;
    }

    // sorts in descending order of the keys
    private int[] sortByKey(final double[] key, int[] array) {
        int[] permutation = sort(indexArray(key.length),
                new Comparator<Integer>() {

                    @Override
                    public int compare(Integer o1, Integer o2) {
                        return -Double.compare(key[o1], key[o2]);
                    }

                });
        return permute(array, permutation);
    }

    private int[] sort(int[] array, Comparator<Integer> comparator) {
        Integer[] boxed = ArrayUtils.boxArray(array);
        Arrays.sort(boxed, comparator);
        return (int[]) ArrayUtils.unboxArray(boxed);
    }

    private int[] permute(int[] array, int[] permutation) {
        int[] array2 = new int[array.length];
        for (int i = 0; i < array.length; i++) {
            array2[i] = array[permutation[i]];
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

    private int[][] countZerosByDimension(AbstractFactor[] compatible) {
        int[][] zeros = new int[dimensions.length][];
        for (int i = 0; i < zeros.length; i++) {
            zeros[i] = new int[dimensions[i]];
        }

        int[][] foreignIdToIndex = computePositionTransformations(compatible);
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

    private int[][] computePositionTransformations(AbstractFactor[] compatible) {
        int[][] localToForeignTransformations = new int[compatible.length][];
        for (int i = 0; i < compatible.length; i++) {
            localToForeignTransformations[i] = AddressCalc.computeLinearMap(compatible[i], dimensionIDs);
        }
        return localToForeignTransformations;
    }

    private boolean checkIfPartitionIsZero(final int partition, int[] counter, AbstractFactor[] compatible,
            int[][] localToForeignTransformations, final int blockSize) {
        boolean isPartitionZero = true;
        for (int j = 0; j < blockSize; j++) {
            if (partition * blockSize + j >= computeLength())
                break;
            AddressCalc.incrementMultiDimensionalCounter(counter, dimensions);
            if (isPartitionZero) {
                isPartitionZero &= checkIfPositionIsZero(counter, compatible, localToForeignTransformations);
            }
        }
        return isPartitionZero;
    }

    private boolean checkIfPositionIsZero(int[] position, AbstractFactor[] compatible,
            int[][] localToForeignTransformations) {
        for (int i = 0; i < compatible.length; i++) {
            AbstractFactor f = compatible[i];
            int pos = MathUtils.scalarProduct(position,
                    localToForeignTransformations[i]);
            if (f.getValue(pos) == 0) {
                return true;
            }
        }
        return false;
    }

    private void optimizeBlockSize(AbstractFactor[] compatible) {
        int blocksize = computeLocallyOptimalPowerOf2BlockSize(compatible);
        blocksize = refineBlockSizeByBinarySearch(blocksize, compatible);

        this.blockSize = blocksize;
    }

    private int computeLocallyOptimalPowerOf2BlockSize(AbstractFactor[] compatible) {
        //stage 1: greedy search restricted on powers of 2 
        int blocksize = 1;
        int arraySize;
        int overhead;
        int newOverhead;
        int newArraySize;
        newArraySize = predictLengthOfValueArray(blocksize, compatible) * values.sizeOfElement();
        newOverhead = (computeLength() / blocksize) * SIZE_OF_INT;
        do {
            blocksize *= 2;
            arraySize = newArraySize;
            overhead = newOverhead;
            newArraySize = predictLengthOfValueArray(blocksize, compatible) * values.sizeOfElement();
            newOverhead = (computeLength() / blocksize) * SIZE_OF_INT;
        } while (newArraySize + newOverhead <= arraySize + overhead);
        blocksize /= 2;
        return blocksize;
    }

    private int refineBlockSizeByBinarySearch(
            int blocksize, AbstractFactor[] compatible) {
        //stage 2: greedy binary search
        int upperBound = blocksize * 2;
        int lowerBound = blocksize;
        while (upperBound - lowerBound > 1) {
            //invariant: lowerBound is a better block size than upperBound
            int lowerArraySize = predictLengthOfValueArray(lowerBound, compatible) * values.sizeOfElement();
            int lowerOverhead = (computeLength() / lowerBound) * SIZE_OF_INT;
            int middle = (lowerBound + upperBound) / 2;
            int middleArraySize = predictLengthOfValueArray(middle, compatible) * values.sizeOfElement();
            int middleOverhead = (computeLength() / middle) * SIZE_OF_INT;
            if (middleArraySize + middleOverhead < lowerArraySize + lowerOverhead) {
                lowerBound = middle;
            } else {
                upperBound = middle;
            }
        }
        return lowerBound;
    }

    private int predictLengthOfValueArray(int blockSize, AbstractFactor[] compatible) {
        int[][] foreignIdToIndex = computePositionTransformations(compatible);
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
    public int getOverhead() {
        return blockPointers.length * SIZE_OF_INT;
    }

    @Override
    public SparseFactor clone() {
        return (SparseFactor) super.clone();
    }

    /**
     * approximates whether creating a sparse factor will save memory compared to a dense factor
     * 
     * @param futureLength
     *            the length that the new factor's value array would have in a dense factor
     * @param multiplicationCandidates
     * @return
     */
    public static boolean isSuitable(int futureLength, AbstractFactor... multiplicationCandidates) {
        if (multiplicationCandidates == null) {
            return false;
        }
        for (AbstractFactor f : multiplicationCandidates) {
            if (isSparseEnough(f, futureLength))
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
        for (Number d : values) {
            if (d.doubleValue() == 0) {
                result++;
            }
        }
        return result;
    }

}
