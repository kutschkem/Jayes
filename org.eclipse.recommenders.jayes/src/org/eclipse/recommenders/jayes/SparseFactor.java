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
import java.util.Map;

import org.eclipse.recommenders.jayes.util.AddressCalc;
import org.eclipse.recommenders.jayes.util.MathUtils;

public class SparseFactor extends Factor {

    private static final int SPARSENESS_EXPONENT = 5;
    public static final int SPARSENESS = 1 << SPARSENESS_EXPONENT;
    private boolean isSparse = false;
    private int[] trie;

    @Override
    public void setDimensions(int[] dimensions) {
        this.dimensions = Arrays.copyOf(dimensions, dimensions.length);
        selections = new int[dimensions.length];
        resetSelections();
        // dont set the value array!
        setDimensionIDs(Arrays.copyOf(getDimensionIDs(), dimensions.length));
        int length = computeLength();
        trie = new int[length / SPARSENESS + (length % SPARSENESS == 0 ? 0 : 1)];
        Arrays.fill(trie, Integer.MIN_VALUE);
    }

    @Override
    public void copyValues(double[] other) {
        System.arraycopy(other, 0, getValues(), 0, other.length);
    }

    public void multiplyCompatible(Factor compatible) {
        if (getValues().length == 1) {
            setValues(createSparseValueArray());
            fill(1);
            for (int i = 0; i < SPARSENESS; i++) {
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
            for (int j = 0; j < SPARSENESS; j++) {
                if (i * SPARSENESS + j >= computeLength())
                    break;
                AddressCalc.incrementMultiDimensionalCounter(counter, dimensions, dimensions.length - 1);
                if (trie[i] != 0) {
                    int pos = computeForeignPosition(compatible, counter, foreignIdToIndex);
                    positions[getSparsePosition(i * SPARSENESS + j)] = pos;
                }
            }
        }
        return positions;
    }

    private double[] createSparseValueArray() {
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
            return new double[(nonSparse + 1) * SPARSENESS];
        }
    }

    /**
     * this method should ONLY be called BEFORE any call that modifies this
     * factor's values
     * 
     * @param compatible
     *            a Factor with compatible dimensions
     */
    public void sparsify(Factor compatible) {
        int[] counter = new int[dimensions.length];
        Map<Integer, Integer> foreignIdToIndex = AddressCalc.computeIdToDimensionIndexMap(compatible);
        counter[counter.length - 1] = -1;
        int triesize = 0;
        for (int i = 0; i < trie.length; i++) {
            boolean isZero = true;
            for (int j = 0; j < SPARSENESS; j++) {
                if (i * SPARSENESS + j >= computeLength())
                    break;
                AddressCalc.incrementMultiDimensionalCounter(counter, dimensions, dimensions.length - 1);
                int pos = computeForeignPosition(compatible, counter, foreignIdToIndex);
                if (compatible.getValue(pos) != 0) {
                    isZero = false;
                }
            }
            if (isZero || trie[i] == 0) {
                trie[i] = 0;
            } else {
                triesize++;
                trie[i] = triesize * SPARSENESS;
            }
        }
    }

    private int getSparsePosition(int pos) {
        return trie[pos >> SPARSENESS_EXPONENT] + (pos & (SPARSENESS - 1));
    }

    private int computeForeignPosition(Factor compatible, int[] counter, Map<Integer, Integer> foreignIdToIndex) {
        // special case: zero-dimensional factor
        if (compatible.dimensions.length == 0) {
            return 0;
        }
        int[] foreignPos = transformLocalToForeignPosition(counter, foreignIdToIndex);

        return AddressCalc.realAddr(compatible.getDimensions(), foreignPos);

    }

    private int[] transformLocalToForeignPosition(int[] localPosition, Map<Integer, Integer> foreignIdToIndex) {
        int[] foreignPosition = new int[foreignIdToIndex.size()];
        for (int i = 0; i < dimensions.length; i++) {
            Integer foreignDim = foreignIdToIndex.get(getDimensionIDs()[i]);
            if (foreignDim != null) // dimension present in the other Factor?
                foreignPosition[foreignDim] = localPosition[i];
        }
        return foreignPosition;
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
            for (int i = 0; i < SPARSENESS; i++) {
                values[i] = 0;
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

}
