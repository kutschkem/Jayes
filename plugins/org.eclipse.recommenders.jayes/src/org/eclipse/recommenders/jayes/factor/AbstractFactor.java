/*******************************************************************************
 * Copyright (c) 2012 Michael Kutschke.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Michael Kutschke - initial API and implementation
 ******************************************************************************/
package org.eclipse.recommenders.jayes.factor;

import java.util.Arrays;

import org.eclipse.recommenders.jayes.factor.arraywrapper.DoubleArrayWrapper;
import org.eclipse.recommenders.jayes.factor.arraywrapper.IArrayWrapper;
import org.eclipse.recommenders.jayes.factor.opcache.DivisionCache;
import org.eclipse.recommenders.jayes.factor.opcache.IOperationCache;
import org.eclipse.recommenders.jayes.factor.opcache.ModuloCache;
import org.eclipse.recommenders.jayes.util.MathUtils;

public abstract class AbstractFactor implements Cloneable {

    public abstract void copyValues(IArrayWrapper arrayWrapper);

    public abstract int[] prepareMultiplication(AbstractFactor compatible);

    protected abstract int getRealPosition(int virtualPosition);

    public abstract void fill(double d);

    protected int[] dimensions = new int[0];
    protected int[] dimensionIDs = new int[0];
    protected IArrayWrapper values = new DoubleArrayWrapper(0.0);
    protected int[] selections = new int[0];
    protected Cut cut = new Cut(this);
    private boolean isCutValid = false;
    private boolean isLogScale = false;

    public AbstractFactor() {
        super();
    }

    public void setValues(IArrayWrapper values) {
        this.values = values;
        assert (MathUtils.product(dimensions) == values.length());
    }

    public IArrayWrapper getValues() {
        return values;
    }

    public double getValue(int i) {
        return values.getDouble(getRealPosition(i));
    }

    public void setDimensions(int... dimensions) {
        this.dimensions = Arrays.copyOf(dimensions, dimensions.length);
        selections = new int[dimensions.length];
        resetSelections();
        int length = MathUtils.product(dimensions);
        if (length > values.length())
            values.newArray(length);
        dimensionIDs = Arrays.copyOf(dimensionIDs, dimensions.length);
    }

    public int[] getDimensions() {
        return dimensions;
    }

    /**
     * tells the Factor which variables the dimensions correspond to. Uniqueness and consistency of size is assumed.
     */
    public void setDimensionIDs(int... ids) {
        dimensionIDs = ids.clone();
    }

    public int[] getDimensionIDs() {
        return dimensionIDs;
    }

    protected int getDimensionFromID(int id) {
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
     * marginalizes out all variables except the one with id sumDimensionID
     * 
     * @param sumDimensionID
     *            -1 for last dimension (default)
     * @return
     */
    public double[] marginalizeAllBut(int sumDimensionID) {
        validateCut();
        if (sumDimensionID == -1) {
            sumDimensionID = dimensionIDs[dimensionIDs.length - 1];
        }
        int sumDimension = getDimensionFromID(sumDimensionID);
        double[] result = new double[dimensions[sumDimension]];
        int divisor = MathUtils.productOfRange(dimensions, sumDimension + 1, dimensions.length);
        IOperationCache division = new DivisionCache(divisor);
        sumToBucket(cut, 0, division, new ModuloCache(result.length), result);
        return result;
    }

    private void sumToBucket(Cut cut, int offset, IOperationCache division, ModuloCache modulo, double[] result) {
        if (cut.getSubCut() == null) {
            int last = cut.getEnd() + offset;
            for (int i = cut.getStart() + offset; i < last; i += cut.getStepSize()) {
                int j = getRealPosition(i);
                int targetPos = modulo.apply(division.apply(i));
                result[targetPos] += values.getDouble(j);
            }
        } else {
            Cut c = cut.getSubCut();
            for (int i = 0; i < cut.getLength(); i += cut.getSubtreeStepsize()) {
                sumToBucket(c, offset + i, division, modulo, result);
            }
        }
    }

    /**
     * multiply the factors. Only compatible factors are allowed, meaning ones that have a subset of the variables of
     * this factor (assume consistent Dimension ID / size pairs)
     * 
     * @param compatible
     */
    public void multiplyCompatible(AbstractFactor compatible) {
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

    private void multiplyPrepared(Cut cut, int offset, IArrayWrapper compatibleValues, int[] positions) {
        if (cut.getSubCut() == null) {
            int last = cut.getEnd() + offset;
            for (int i = cut.getStart() + offset; i < last; i += cut.getStepSize()) {
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

    public void sumPrepared(IArrayWrapper compatibleFactorValues, int[] preparedOperation) {
        validateCut();

        compatibleFactorValues.fill(0);

        if (!isLogScale)
            sumPrepared(cut, 0, compatibleFactorValues, preparedOperation);
        else
            sumPreparedLog(compatibleFactorValues, preparedOperation);

    }

    private void sumPrepared(Cut cut, int offset, IArrayWrapper compatibleFactorValues, int[] positions) {
        if (cut.getSubCut() == null) {
            int last = cut.getEnd() + offset;
            for (int i = cut.getStart() + offset; i < last; i += cut.getStepSize()) {
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
            compatibleFactorValues.set(i, Math.log(compatibleFactorValues.getDouble(i)) + max);
        }
    }

    private double findMax(Cut cut, int offset, double max) {
        if (cut.getSubCut() == null) {
            int last = cut.getEnd() + offset;
            for (int i = cut.getStart() + offset; i < last; i += cut.getStepSize()) {
                int j = getRealPosition(i);
                if (values.getDouble(j) != Double.NEGATIVE_INFINITY && Math.abs(values.getDouble(j)) > Math.abs(max)) {
                    max = values.getDouble(j);
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

    private void sumPreparedLog(Cut cut, int offset, IArrayWrapper compatibleFactorValues, int[] positions, double max) {
        if (cut.getSubCut() == null) {
            int last = cut.getEnd() + offset;
            for (int i = cut.getStart() + offset; i < last; i += cut.getStepSize()) {
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

    private void multiplyPreparedLog(Cut cut, int offset, IArrayWrapper compatibleValues, int[] positions) {
        if (cut.getSubCut() == null) {
            int last = cut.getEnd() + offset;
            for (int i = cut.getStart() + offset; i < last; i += cut.getStepSize()) {
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

    @Override
    public AbstractFactor clone() {
        AbstractFactor f = null;
        try {
            f = (AbstractFactor) super.clone();
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

    public void multiplyCompatibleToLog(AbstractFactor factor) {
        int[] positions = prepareMultiplication(factor);
        for (int i = 0; i < values.length(); i++) {
            values.addAssign(i, Math.log(factor.values.getDouble(positions[i])));
        }

    }

    /**
     * 
     * @return approximated memory requirements on top of the value array
     */
    public abstract int getOverhead();

}
