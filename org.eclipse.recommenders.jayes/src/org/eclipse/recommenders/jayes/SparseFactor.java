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
package org.eclipse.recommenders.jayes;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.recommenders.jayes.util.AddressCalc;
import org.eclipse.recommenders.jayes.util.ArrayUtils;
import org.eclipse.recommenders.jayes.util.MathUtils;

public class SparseFactor extends Factor {

    /**
     * 2^sparsenessExponent values in the original array correspond to one trie
     * entry
     */
    private int sparsenessExponent;
    private int[] trie;
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
    public void copyValues(double[] other) {
        System.arraycopy(other, 0, getValues(), 0, other.length);
    }

    public void multiplyCompatible(Factor compatible) {
        if (getValues().length == 1) {
            setValues(createSparseValueArray());
            fill(1);
            for (int i = 0; i < getSparseness(); i++) {
                values[i] = 0;
            }
        }
        int[] positions = prepareMultiplication(compatible);
        multiplyPrepared(compatible.getValues(), positions);
    }

    @Override
    public int[] prepareMultiplication(Factor compatible) {
        if (!isSparse)
            return super.prepareMultiplication(compatible);

        int[] positions = new int[getValues().length];
        int[] counter = new int[dimensions.length];
        Map<Integer, Integer> foreignIdToIndex = AddressCalc.computeIdToDimensionIndexMap(compatible);
        counter[counter.length - 1] = -1;
        for (int i = 0; i < trie.length; i++) {
            for (int j = 0; j < getSparseness(); j++) {
                if (i * getSparseness() + j >= computeLength())
                    break;
                AddressCalc.incrementMultiDimensionalCounter(counter, dimensions, dimensions.length - 1);
                if (trie[i] != 0) {
                    int pos = computeForeignPosition(compatible, counter, foreignIdToIndex);
                    positions[getSparsePosition(i * getSparseness() + j)] = pos;
                }
            }
        }
        return positions;
    }

    private double[] createSparseValueArray() {
        // need to treat 0-dimensional factors specially
        if (dimensions.length == 0) {
            trie = new int[0];
            return new double[1];
        }

        if (trie == null) {
            trie = new int[0];
        }

        int nonSparse = 0;
        for (int i = 0; i < trie.length; i++) {
            if (trie[i] != 0) {
                nonSparse++;
            }
        }
        if (nonSparse == trie.length) {
            trie = new int[0];
            return new double[computeLength()];
        } else {
            isSparse = true;
            return new double[(nonSparse + 1) * getSparseness()];
        }
    }

    /**
     * Prepares the factor in the sense that it's internal structures are
     * optimized according to the zero/non-zero structure of the factors that
     * will be multiplied in. This method should <strong>always</strong> and
     * <strong>only</strong> be called <strong>once before</strong> any call
     * that modifies this factor's values
     * 
     * @param compatible
     *            a Factor with compatible dimensions
     */
    public void sparsify(List<Factor> compatible) {
        optimizeDimensionOrder(compatible);
        sparsenessExponent = computeOptimalSparsenessExponent(1, compatible);
        if (sparsenessExponent != -1) {
            System.out.println("Sparseness: " + sparsenessExponent);
        }
        if (sparsenessExponent == -1) {
            // HACKHACK cue for createSparseValueArray to make factor nonsparse
            trie = new int[1];
            trie[0] = 1;
            return;
        }
        int length = computeLength();
        trie = new int[length / getSparseness() + (length % getSparseness() == 0 ? 0 : 1)];
        Arrays.fill(trie, Integer.MIN_VALUE);
        Map<Factor, Map<Integer, Integer>> foreignIdToIndex = computeIDToIndexMaps(compatible);
        int[] counter = new int[dimensions.length];
        counter[counter.length - 1] = -1;
        int triesize = 0;
        for (int i = 0; i < trie.length; i++) {
            boolean isZero = checkIfPartitionIsZero(i, counter, compatible, foreignIdToIndex, getSparseness());
            if (isZero || trie[i] == 0) {
                trie[i] = 0;
            } else {
                triesize++;
                trie[i] = triesize * getSparseness();
            }
        }
        System.out.println("Sparse entries: " + ((trie.length - triesize) * getSparseness()));
    }

    private void optimizeDimensionOrder(List<Factor> compatible) {
        int[][] zerosByDimension = countZerosByDimension(compatible);
        final double[] infogain = calcInfoGain(zerosByDimension);
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

    private double[] calcInfoGain(int[][] zerosByDimension) {
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
            boolean isZero = checkIfPartitionIsZero(i, counter, compatible, foreignIdToIndex, 1);
            if (isZero) {
                for (int j = 0; j < dimensions.length; j++) {
                    zeros[j][counter[j]]++;
                }
            }
        }

        return zeros;
    }

    private Map<Factor, Map<Integer, Integer>> computeIDToIndexMaps(List<Factor> compatible) {
        Map<Factor, Map<Integer, Integer>> foreignIdToIndex = new HashMap<Factor, Map<Integer, Integer>>();
        for (Factor f : compatible) {
            foreignIdToIndex.put(f, AddressCalc.computeIdToDimensionIndexMap(f));
        }
        return foreignIdToIndex;
    }

    private boolean checkIfPartitionIsZero(int partition, int[] counter, List<Factor> compatible,
            Map<Factor, Map<Integer, Integer>> foreignIdToIndex, int sparseness) {
        boolean isZero = true;
        for (int j = 0; j < sparseness; j++) {
            if (partition * sparseness + j >= computeLength())
                break;
            AddressCalc.incrementMultiDimensionalCounter(counter, dimensions, dimensions.length - 1);
            boolean isNotZero = true;
            for (Factor f : compatible) {
                int pos = computeForeignPosition(f, counter, foreignIdToIndex.get(f));
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

    private int computeOptimalSparsenessExponent(int start, List<Factor> factor) {
        int savings;
        int overhead;
        int newOverhead;
        int newSavings;
        newSavings = computeSavings(start, factor);
        newOverhead = (computeLength() >> start) + (1 << start);
        do { // greedy strategy
            savings = newSavings;
            overhead = newOverhead;
            start++;
            newSavings = computeSavings(start, factor);
            newOverhead = (computeLength() >> start) + (1 << start);
        } while (overhead - savings > newOverhead - newSavings);
        if (overhead - savings >= 0)
            return -1;
        return start - 1;
    }

    private int computeSavings(int exponent, List<Factor> compatible) {
        Map<Factor, Map<Integer, Integer>> foreignIdToIndex = computeIDToIndexMaps(compatible);
        int[] counter = new int[dimensions.length];
        counter[counter.length - 1] = -1;
        int length = computeLength();
        int saving = 0;
        for (int i = 0; i < length / (1 << exponent); i++) {
            boolean isZero = checkIfPartitionIsZero(i, counter, compatible, foreignIdToIndex, 1 << exponent);
            if (isZero) {
                saving += 1 << exponent;
            }
        }
        return saving;
    }

    private int getSparsePosition(int pos) {
        return trie[pos >> sparsenessExponent] + (pos & (getSparseness() - 1));
    }

    public int computeLength() {
        return MathUtils.multiply(dimensions);
    }

    @Override
    public double[] sum(int sumDimensionID) {
        if (!isSparse)
            return super.sum(sumDimensionID);
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void multiplyPrepared(double[] compatibleValues, int[] positions) {
        if (!isSparse) {
            super.multiplyPrepared(compatibleValues, positions);
            return;
        }
        validateCut();
        if (!isLogScale())
            multiplyPrepared(cut, 0, compatibleValues, positions);
        else
            multiplyPreparedLog(cut, 0, compatibleValues, positions);
    }

    private void multiplyPrepared(Cut cut, int offset, double[] compatibleValues, int[] positions) {
        if (cut.getSubCut() == null) {
            int last = Math.min(computeLength(), cut.getLength() + cut.getIndex() + offset);
            for (int i = cut.getIndex() + offset; i < last; i += cut.getStepSize()) {
                int j = getSparsePosition(i);
                values[j] *= compatibleValues[positions[j]];
            }
        } else {
            Cut c = cut.getSubCut();
            for (int i = 0; i < cut.getLength(); i += cut.getSubtreeStepsize()) {
                multiplyPrepared(c, offset + i, compatibleValues, positions);
            }
        }
    }

    private void multiplyPreparedLog(Cut cut, int offset, double[] compatibleValues, int[] positions) {
        if (cut.getSubCut() == null) {
            int last = Math.min(computeLength(), cut.getLength() + cut.getIndex() + offset);
            for (int i = cut.getIndex() + offset; i < last; i += cut.getStepSize()) {
                int j = getSparsePosition(i);
                values[j] += compatibleValues[positions[j]];
            }
        } else {
            Cut c = cut.getSubCut();
            for (int i = 0; i < cut.getLength(); i += cut.getSubtreeStepsize()) {
                multiplyPreparedLog(c, offset + i, compatibleValues, positions);
            }
        }
    }

    @Override
    public void sumPrepared(double[] compatibleFactorValues, int[] preparedOperation) {
        if (!isSparse) {
            super.sumPrepared(compatibleFactorValues, preparedOperation);
            return;
        }
        validateCut();

        Arrays.fill(compatibleFactorValues, 0);

        if (!isLogScale())
            sumPrepared(cut, 0, compatibleFactorValues, preparedOperation);
        else
            sumPreparedLog(compatibleFactorValues, preparedOperation);

    }

    private void sumPrepared(Cut cut, int offset, double[] compatibleValues, int[] positions) {
        if (cut.getSubCut() == null) {
            int last = Math.min(computeLength(), cut.getLength() + cut.getIndex() + offset);
            for (int i = cut.getIndex() + offset; i < last; i += cut.getStepSize()) {
                int j = getSparsePosition(i);
                compatibleValues[positions[j]] += values[j];
            }
        } else {
            Cut c = cut.getSubCut();
            for (int i = 0; i < cut.getLength(); i += cut.getSubtreeStepsize()) {
                sumPrepared(c, offset + i, compatibleValues, positions);
            }
        }
    }

    private void sumPreparedLog(double[] compatible, int[] positions) {
        double max = findMax(cut, 0, 0);
        sumPreparedLog(cut, 0, compatible, positions, max);
        for (int i = 0; i < compatible.length; i++) {
            compatible[i] = Math.log(compatible[i]) + max;
        }
    }

    private double findMax(Cut cut, int offset, double max) {
        if (cut.getSubCut() == null) {
            int last = Math.min(computeLength(), cut.getLength() + cut.getIndex() + offset);
            for (int i = cut.getIndex() + offset; i < last; i += cut.getStepSize()) {
                int j = getSparsePosition(i);
                if (values[j] != Double.NEGATIVE_INFINITY && Math.abs(values[j]) > Math.abs(max)) {
                    max = values[j];
                }
            }
        } else {
            Cut c = cut.getSubCut();
            for (int i = 0; i < cut.getLength(); i += cut.getSubtreeStepsize()) {
                double pot = findMax(c, offset + i, max);
                if (pot != Double.NEGATIVE_INFINITY && Math.abs(pot) > Math.abs(max)) {
                    max = pot;
                }
            }
        }
        return max;
    }

    private void sumPreparedLog(Cut cut, int offset, double[] compatibleValues, int[] positions, double max) {
        if (cut.getSubCut() == null) {
            int last = Math.min(computeLength(), cut.getLength() + cut.getIndex() + offset);
            for (int i = cut.getIndex() + offset; i < last; i += cut.getStepSize()) {
                int j = getSparsePosition(i);
                compatibleValues[positions[j]] += Math.exp(values[j] - max);
            }
        } else {
            Cut c = cut.getSubCut();
            for (int i = 0; i < cut.getLength(); i += cut.getSubtreeStepsize()) {
                sumPreparedLog(c, offset + i, compatibleValues, positions, max);
            }
        }
    }

    @Override
    public void multiplyCompatibleToLog(Factor compatible) {
        if (getValues().length == 1) {
            setValues(createSparseValueArray());
            fill(1);
        }
        int[] positions = prepareMultiplication(compatible);
        for (int i = 0; i < getValues().length; i++) {
            getValues()[i] += Math.log(compatible.getValues()[positions[i]]);
        }
    }

    @Override
    public void fill(double d) {
        if (getValues().length == 1) {
            setValues(createSparseValueArray());
        }
        super.fill(d);
        if (isSparse)
            for (int i = 0; i < getSparseness(); i++) {
                values[i] = isLogScale() ? Double.NEGATIVE_INFINITY : 0;
            }
    }

    @Override
    public double getValue(int i) {
        if (!isSparse)
            return values[i];
        return values[getSparsePosition(i)];
    }

    public boolean isSparse() {
        return isSparse;
    }

    public int getSparseness() {
        return 1 << sparsenessExponent;
    }

}
