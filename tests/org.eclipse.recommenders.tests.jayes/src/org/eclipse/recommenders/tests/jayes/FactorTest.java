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
package org.eclipse.recommenders.tests.jayes;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.eclipse.recommenders.jayes.BayesNet;
import org.eclipse.recommenders.jayes.factor.AbstractFactor;
import org.eclipse.recommenders.jayes.factor.DenseFactor;
import org.eclipse.recommenders.jayes.factor.SparseFactor;
import org.eclipse.recommenders.jayes.util.ArrayUtils;
import org.eclipse.recommenders.jayes.util.MathUtils;
import org.eclipse.recommenders.jayes.util.arraywrapper.DoubleArrayWrapper;
import org.eclipse.recommenders.tests.jayes.util.NetExamples;
import org.junit.Test;

public class FactorTest {

	private static final double[] DISTRIBUTION_2x2x2 = ArrayUtils.flatten(new double[][][] { 
			{ { 0.5, 0.5 }, { 1.0, 0.0 } }, 
			{ { 0.4, 0.6 }, { 0.3, 0.7 } } 
			});
	private static final double TOLERANCE = 0.00001;

	@Test
	public void testSum() {
		AbstractFactor factor = new DenseFactor();
		factor.setDimensionIDs(0, 1);
		factor.setDimensions(2, 2);
		factor.setValues(new DoubleArrayWrapper(0.5, 0.5, 1.0, 0.0));
		double[] prob = MathUtils.normalize(factor.sum(-1));
		assertArrayEquals(prob, new double[] { 0.75, 0.25 }, TOLERANCE);
	}

	@Test
	public void testSelectAndSum() {
		AbstractFactor factor = create2x2x2Factor();
		factor.setValues(new DoubleArrayWrapper(DISTRIBUTION_2x2x2));
		factor.select(0, 0);
		double[] prob = MathUtils.normalize(factor.sum(-1));
		assertArrayEquals(prob, new double[] { 0.75, 0.25 }, TOLERANCE);

		factor.select(0, -1);
		factor.select(1, 1);
		prob = MathUtils.normalize(factor.sum(-1));
		assertArrayEquals(prob, new double[] { 0.65, 0.35 }, TOLERANCE);
	}

	private AbstractFactor create2x2x2Factor() {
		AbstractFactor factor = new DenseFactor();
		factor.setDimensionIDs(0, 1, 2);
		factor.setDimensions(2, 2, 2);
		return factor;
	}

	@Test
	public void testSumMiddle1() {
		AbstractFactor factor = create2x2x2Factor();
		factor.setValues(new DoubleArrayWrapper(DISTRIBUTION_2x2x2));
		factor.select(2, 0);
		double[] prob = MathUtils.normalize(factor.sum(1));
		assertArrayEquals(prob, new double[] { 0.9 / 2.2, 1.3 / 2.2 }, TOLERANCE);
	}

	@Test
	public void testSumMiddle2() {
		AbstractFactor factor = create2x2x2Factor();
		factor.setValues(new DoubleArrayWrapper(DISTRIBUTION_2x2x2));
		factor.select(0, 1);
		factor.select(2, 1);
		double[] prob = MathUtils.normalize(factor.sum(1));
		assertArrayEquals(prob, new double[] { 0.6 / 1.3, 0.7 / 1.3 }, TOLERANCE);
	}

	@Test
	public void testMultiplication() {
		AbstractFactor f1 = create2x2x2Factor();
		f1.setValues(new DoubleArrayWrapper(ArrayUtils.flatten(new double[][][] { 
				{ {0.5, 0.5}, {0.5, 0.5}}, 
				{ {0.5, 0.5}, {0.5, 0.5}} 
				})));

		AbstractFactor f2 = new DenseFactor();
		f2.setDimensionIDs(2, 0);
		f2.setDimensions(2, 2);
		f2.setValues(new DoubleArrayWrapper(1.0, 0.0, 0.0, 1.0));

		f1.multiplyCompatible(f2);
		assertArrayEquals(f1.getValues().toDoubleArray(), new double[] { 0.5, 0.0, 0.5, 0.0, 0.0, 0.5, 0.0, 0.5 }, TOLERANCE);
	}

	@Test
	public void testPreparedSum() {
		AbstractFactor f = new DenseFactor();
		f.setDimensionIDs(0, 1, 2);
		f.setDimensions(4, 4, 4);
		f.fill(1);

		AbstractFactor f2 = new DenseFactor();
		f2.setDimensionIDs(2);
		f2.setDimensions(4);

		f.sumPrepared(f2.getValues(), f.prepareMultiplication(f2));

		assertArrayEquals(f.sum(-1), f2.getValues().toDoubleArray(), TOLERANCE);
	}

	@Test
	public void testCopy() {
		AbstractFactor f = create2x2x2Factor();

		f.select(2, 1);
		
		// no ArrayIndexOutOfBoundsException should be thrown
		f.copyValues(new DoubleArrayWrapper(1, 1, 1, 1, 1, 1, 1, 1));

		for (int oddIndex = 1; oddIndex < f.getValues().length(); oddIndex += 2) {
			assertThat(f.getValue(oddIndex), is(1.0));
		}
	}

	@Test
	public void testIsSuitable() {
		// no NullPointerException should be thrown
		assertFalse(SparseFactor.isSuitable(1, null));
		assertFalse(SparseFactor.isSuitable(1, Collections.<AbstractFactor> emptyList()));

		BayesNet sparseNet = NetExamples.sparseNet();
		AbstractFactor sparse = sparseNet.getNode("c").getFactor();
		assertTrue(SparseFactor.isSuitable(MathUtils.product(sparse.getDimensions()), Arrays.asList(sparse)));

		AbstractFactor nonsparse = sparseNet.getNode("d").getFactor();
		assertFalse(SparseFactor.isSuitable(MathUtils.product(nonsparse.getDimensions()), Arrays.asList(nonsparse)));
	}

}
