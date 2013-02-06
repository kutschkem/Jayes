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
package org.eclipse.recommenders.tests.jayes;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.*;

import org.eclipse.recommenders.jayes.BayesNet;
import org.eclipse.recommenders.jayes.factor.AbstractFactor;
import org.eclipse.recommenders.jayes.factor.DenseFactor;
import org.eclipse.recommenders.jayes.factor.SparseFactor;
import org.eclipse.recommenders.jayes.factor.arraywrapper.DoubleArrayWrapper;
import org.eclipse.recommenders.jayes.factor.arraywrapper.FloatArrayWrapper;
import org.eclipse.recommenders.jayes.util.MathUtils;
import org.eclipse.recommenders.tests.jayes.util.NetExamples;
import org.junit.Test;

public class SparseFactorTest {

    @Test
    public void testIsSuitableNoFactors() {
        // no NullPointerException should be thrown
        assertFalse(SparseFactor.isSuitable(1, (AbstractFactor[]) null));
        assertFalse(SparseFactor.isSuitable(1, new AbstractFactor[0]));

    }

    @Test
    public void testIsSuitableSparseFactor() {
        BayesNet sparseNet = NetExamples.sparseNet();
        AbstractFactor sparse = sparseNet.getNode("c").getFactor();
        assertTrue(SparseFactor.isSuitable(MathUtils.product(sparse.getDimensions()), sparse));
    }

    @Test
    public void testIsSuitableNonSparseFactor() {
        AbstractFactor nonsparse = NetExamples.sparseNet().getNode("d").getFactor();
        assertFalse(SparseFactor.isSuitable(MathUtils.product(nonsparse.getDimensions()), nonsparse));
    }

    @Test
    public void testBlocksizeOne() {
        AbstractFactor dense = new DenseFactor();
        dense.setDimensionIDs(0);
        dense.setDimensions(12);
        dense.setValues(new DoubleArrayWrapper(0, 1, 0, 2, 0, 3, 0, 4, 0, 5, 0, 6));

        SparseFactor sparse = SparseFactor.fromFactor(dense);

        assertEquals(7, sparse.getValues().length());

        assertThat(sparse.getValues().toDoubleArray(), is(new double[] { 0, 1, 2, 3, 4, 5, 6 }));
    }

    @Test
    public void testEvenBlocksize() {
        AbstractFactor dense = new DenseFactor();
        dense.setDimensionIDs(0);
        dense.setDimensions(16);
        dense.setValues(new FloatArrayWrapper(0, 0, 0, 0, 1, 2, 3, 4, 0, 0, 0, 0, 5, 6, 7, 8));

        SparseFactor sparse = SparseFactor.fromFactor(dense);

        // is 12 if a blocksize of 4 is chosen (which is best here, because of lower overhead)
        //would be 13 if a blocksize of 3 was chosen
        //would be 10 in case of a blocksize of 2
        assertEquals(12, sparse.getValues().length());

        assertThat(sparse.getValues().toDoubleArray(), is(new double[] { 0, 0, 0, 0, 1, 2, 3, 4, 5, 6, 7, 8 }));
    }

    @Test
    public void testOddBlocksize() {
        AbstractFactor dense = new DenseFactor();
        dense.setDimensionIDs(0);
        dense.setDimensions(12);
        dense.setValues(new DoubleArrayWrapper(0, 0, 0, 1, 2, 3, 0, 0, 0, 4, 5, 6));

        SparseFactor sparse = SparseFactor.fromFactor(dense);

        //is 9 if a blocksize of 3 is chosen
        //would be 10 in case of a blocksize of 2
        assertEquals(9, sparse.getValues().length());

        assertThat(sparse.getValues().toDoubleArray(), is(new double[] { 0, 0, 0, 1, 2, 3, 4, 5, 6 }));
    }

    @Test
    public void testZeroDimensional() {
        AbstractFactor dense = new DenseFactor();
        dense.setDimensionIDs();
        dense.setDimensions();
        dense.setValues(new DoubleArrayWrapper(2));

        SparseFactor sparse = SparseFactor.fromFactor(dense);

        assertEquals(2, sparse.getValues().length());

        assertThat(sparse.getValues().toDoubleArray(), is(new double[] { 0, 2 }));
    }

    /*
     * zero-dimensional dense factor, but one-dimensional sparse factor
     */
    @Test
    public void testZeroDimensional2() {
        AbstractFactor dense = new DenseFactor();
        dense.setDimensionIDs();
        dense.setDimensions();
        dense.setValues(new DoubleArrayWrapper(2));

        SparseFactor sparse = new SparseFactor();
        sparse.setDimensionIDs(0);
        sparse.setDimensions(2);

        sparse.sparsify(dense);

        sparse.fill(1);
        sparse.multiplyCompatible(dense);

        assertThat(sparse.getValues().toDoubleArray(), is(new double[] { 0, 2, 2 }));
    }

    @Test
    public void testDimensionReordering() {
        AbstractFactor dense = new DenseFactor();
        dense.setDimensionIDs(0, 1);
        dense.setDimensions(2, 2);
        dense.setValues(new DoubleArrayWrapper(0, 1, 0, 2));

        SparseFactor sparse = SparseFactor.fromFactor(dense);

        int[] reorderedDimensionIds = new int[] { 1, 0 };
        assertThat(sparse.getDimensionIDs(), is(reorderedDimensionIds));
        assertEquals(4, sparse.getValues().length());

        assertThat(sparse.getValues().toDoubleArray(), is(new double[] { 0, 0, 1, 2 }));
    }

    @Test
    public void testSum() {
        AbstractFactor dense = new DenseFactor();
        dense.setDimensionIDs(0);
        dense.setDimensions(12);
        dense.setValues(new DoubleArrayWrapper(0, 1, 0, 2, 0, 4, 0, 8, 0, 16, 0, 32));

        SparseFactor sparse = SparseFactor.fromFactor(dense);

        assertThat(sparse.marginalizeAllBut(0), is(new double[] { 0, 1, 0, 2, 0, 4, 0, 8, 0, 16, 0, 32 }));
    }

    @Test
    public void testFromSparseFactor() {
        AbstractFactor dense = new DenseFactor();
        dense.setDimensionIDs(0);
        dense.setDimensions(12);
        dense.setValues(new DoubleArrayWrapper(0, 1, 0, 2, 0, 4, 0, 8, 0, 16, 0, 32));

        SparseFactor sparse = SparseFactor.fromFactor(dense);
        SparseFactor sparse2 = SparseFactor.fromFactor(sparse);

        assertThat(sparse2.getValues(), is(not(sameInstance(sparse.getValues()))));
        assertThat(sparse2.getValues().toDoubleArray(), is(sparse.getValues().toDoubleArray()));
    }

}
