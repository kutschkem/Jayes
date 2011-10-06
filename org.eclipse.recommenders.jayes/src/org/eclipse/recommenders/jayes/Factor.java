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
package org.eclipse.recommenders.jayes;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.recommenders.jayes.util.AddressCalc;

public class Factor implements Cloneable {

    protected int[] dimensions = new int[0];
    private int[] dimensionIDs = new int[0];
    private double[] values = new double[1];
    protected int[] selections = new int[0];

    private Cut cut = new Cut();
    private boolean isCutValid = false;

    private boolean isLogScale = false;

    public void setValues(double[] values) {
        this.values = values;
    }

    public double[] getValues() {
        return values;
    }

    public void fill(double d) {
        Arrays.fill(values, d);
    }

    public void setDimensions(int[] dimensions) {
        this.dimensions = Arrays.copyOf(dimensions, dimensions.length);
        selections = new int[dimensions.length];
        resetSelections();
        int length = 1;
        for (int i : dimensions)
            length *= i;
        if (length > values.length)
            values = new double[length];
        dimensionIDs = Arrays.copyOf(dimensionIDs, dimensions.length);
    }

    public int[] getDimensions() {
        return dimensions;
    }

    /**
     * tells the Factor which variables the dimensions correspond to. Uniqueness
     * and consistency of size is assumed.
     */
    public void setDimensionIDs(int[] ids) {
        dimensionIDs = ids.clone();
    }

    public int[] getDimensionIDs() {
        return dimensionIDs;
    }

    private int getDimensionFromID(int id) {
        for (int i = 0; i < dimensionIDs.length; i++)
            if (dimensionIDs[i] == id)
                return i;
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
        if (cut.subCut == null) {
            int last = cut.index + offset + cut.length;
            double[] val = values;
            for (int i = cut.index + offset; i < last; i += cut.stepSize) {
                result[(i / divisor) % result.length] += val[i];
            }
        } else {
            Cut c = cut.subCut;
            for (int i = 0; i < cut.length; i += cut.subtreeStepsize) {
                sumToBucket(c, offset + i, divisor, result);
            }
        }
    }

    /**
     * multiply the factors. Only compatible factors are allowed, meaning ones
     * that have a subset of the variables of this factor (assume consistent
     * Dimension ID / size pairs
     * 
     * @param compatible
     */
    public void multiplyCompatible(Factor compatible) {
        int[] positions = prepareMultiplication(compatible);
        multiplyPrepared(compatible.values, positions);
    }

    public void multiplyPrepared(double[] compatibleValues, int[] positions) {
        validateCut();
        if (!isLogScale)
            multiplyPrepared(cut, 0, compatibleValues, positions);
        else
            multiplyPreparedLog(cut, 0, compatibleValues, positions);
    }

    private void multiplyPrepared(Cut cut, int offset, double[] compatibleValues, int[] positions) {
        if (cut.subCut == null) {
            int last = Math.min(values.length, cut.length + cut.index + offset);
            for (int i = cut.index + offset; i < last; i += cut.stepSize)
                values[i] *= compatibleValues[positions[i]];
        } else {
            Cut c = cut.subCut;
            for (int i = 0; i < cut.length; i += cut.subtreeStepsize) {
                multiplyPrepared(c, offset + i, compatibleValues, positions);
            }
        }
    }

    public void sumPrepared(double[] compatibleFactorValues, int[] preparedOperation) {
        Arrays.fill(compatibleFactorValues, 0);

        validateCut();

        if (!isLogScale)
            sumPrepared(cut, 0, compatibleFactorValues, preparedOperation);
        else
            sumPreparedLog(compatibleFactorValues, preparedOperation);

    }

    private void sumPrepared(Cut cut, int offset, double[] compatibleValues, int[] positions) {
        if (cut.subCut == null) {
            int last = Math.min(values.length, cut.length + cut.index + offset);
            for (int i = cut.index + offset; i < last; i += cut.stepSize)
                compatibleValues[positions[i]] += values[i];
        } else {
            Cut c = cut.subCut;
            for (int i = 0; i < cut.length; i += cut.subtreeStepsize) {
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
        if (cut.subCut == null) {
            int last = Math.min(values.length, cut.length + cut.index + offset);
            for (int i = cut.index + offset; i < last; i += cut.stepSize) {
                if (values[i] != Double.NEGATIVE_INFINITY && Math.abs(values[i]) > Math.abs(max)) {
                    max = values[i];
                }
            }
        } else {
            Cut c = cut.subCut;
            for (int i = 0; i < cut.length; i += cut.subtreeStepsize) {
                double pot = findMax(c, offset + i, max);
                if (pot != Double.NEGATIVE_INFINITY && Math.abs(pot) > Math.abs(max)) {
                    max = pot;
                }
            }
        }
        return max;
    }

    private void sumPreparedLog(Cut cut, int offset, double[] compatibleValues, int[] positions, double max) {
        if (cut.subCut == null) {
            int last = Math.min(values.length, cut.length + cut.index + offset);
            for (int i = cut.index + offset; i < last; i += cut.stepSize)
                compatibleValues[positions[i]] += Math.exp(values[i] - max);
        } else {
            Cut c = cut.subCut;
            for (int i = 0; i < cut.length; i += cut.subtreeStepsize) {
                sumPreparedLog(c, offset + i, compatibleValues, positions, max);
            }
        }
    }

    private void multiplyPreparedLog(Cut cut, int offset, double[] compatibleValues, int[] positions) {
        if (cut.subCut == null) {
            int last = Math.min(values.length, cut.length + cut.index + offset);
            for (int i = cut.index + offset; i < last; i += cut.stepSize)
                values[i] += compatibleValues[positions[i]];
        } else {
            Cut c = cut.subCut;
            for (int i = 0; i < cut.length; i += cut.subtreeStepsize) {
                multiplyPreparedLog(c, offset + i, compatibleValues, positions);
            }
        }
    }

    private void validateCut() {
        if (!isCutValid) {
            cut.initialize();
            isCutValid = true;
        }

    }

    /**
     * prepares multiplication by precomputing the corresponding array positions
     * in the compatible Factor
     * 
     * @param compatible
     *            a factor that has a subset of the dimensions of this factor
     * @return
     */
    public int[] prepareMultiplication(Factor compatible) {
        int[] positions = new int[values.length];
        int[] counter = new int[dimensions.length];
        Map<Integer, Integer> foreignIdToIndex = computeIdToDimensionIndexMap(compatible);
        counter[counter.length - 1] = -1;
        for (int i = 0; i < values.length; i++) {
            AddressCalc.incrementMultiDimensionalCounter(counter, dimensions, dimensions.length - 1);
            positions[i] = computeForeignPosition(compatible, counter, foreignIdToIndex);
        }
        return positions;
    }

    private Map<Integer, Integer> computeIdToDimensionIndexMap(Factor factor) {
        Map<Integer, Integer> foreignIds = new HashMap<Integer, Integer>();
        for (int i = 0; i < factor.dimensionIDs.length; i++) {
            foreignIds.put(factor.dimensionIDs[i], i);
        }
        return foreignIds;
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
        f.cut = f.new Cut();
        f.isCutValid = false;
        return f;
    }

    public void multiplyCompatibleToLog(Factor factor) {
        int[] positions = prepareMultiplication(factor);
        for (int i = 0; i < values.length; i++) {
            values[i] += Math.log(factor.values[positions[i]]);
        }

    }

    private class Cut implements Cloneable {
        private int index;
        private int stepSize;
        private int length;

        private int subtreeStepsize;

        private int rootDimension;
        private int leafDimension;

        // the subtree(s); only one because of the inherent regularities of the
        // decision tree
        private Cut subCut;

        public Cut() {
        }

        public void initialize() {
            length = values.length;
            index = 0;
            stepSize = 1;
            subtreeStepsize = values.length / dimensions[0];
            rootDimension = 0;
            leafDimension = dimensions.length - 1;
            subCut = null;
            leafCut();
            rootCut();
            createSubcut();
        }

        @Override
        public Cut clone() {
            try {
                return (Cut) super.clone();
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
                return null;
            }
        }

        private void rootCut() {
            while (rootDimension < leafDimension && selections[rootDimension] != -1) {
                descendSelectedDimension();
            }
            while (rootDimension < leafDimension && selections[rootDimension + 1] == -1) {
                descendUnselectedDimension();
            }
        }

        private void descendSelectedDimension() {
            length /= dimensions[rootDimension];
            index += subtreeStepsize * selections[rootDimension];
            descendUnselectedDimension();
        }

        private void descendUnselectedDimension() {
            rootDimension++;
            subtreeStepsize /= dimensions[rootDimension];
        }

        private void leafCut() {
            while (leafDimension >= 0 && selections[leafDimension] != -1) {
                ascendSelectedDimension();
            }
        }

        private void ascendSelectedDimension() {
            index += selections[leafDimension] * stepSize;
            length -= selections[leafDimension] * stepSize;
            stepSize *= dimensions[leafDimension];
            leafDimension--;
        }

        private void createSubcut() {
            if (needsSplit()) {
                subCut = null; // avoid circularity in object graph
                subCut = clone();
                subCut.descendUnselectedDimension();
                subCut.length = subtreeStepsize;
                subCut.rootCut(); // no leaf cut, no zero compression
                subCut.createSubcut();
            }
        }

        /**
         * the Cut needs to further split if and only if there is an additional
         * selection between root and leaf
         * 
         * @return
         */
        private boolean needsSplit() {
            if (length < subtreeStepsize)
                return false;
            for (int i = rootDimension; i < leafDimension; i++)
                if (selections[i] != -1)
                    return true;
            return false;
        }

    }

}
