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

import static org.junit.Assert.assertArrayEquals;
import org.eclipse.recommenders.jayes.Factor;
import org.eclipse.recommenders.jayes.util.ArrayUtils;
import org.eclipse.recommenders.jayes.util.DoubleArrayWrapper;
import org.eclipse.recommenders.jayes.util.MathUtils;
import org.junit.Test;

public class FactorTest {

    @Test
    public void sumTest() {
        Factor factor = new Factor();
        factor.setDimensionIDs(new int[] { 0, 1, 2 });
        factor.setDimensions(new int[] { 2, 2 });
        factor.setValues(new DoubleArrayWrapper(new double[] { 0.5, 0.5, 1.0, 0.0 }));
        double[] prob = MathUtils.normalize(factor.sum(-1));
        assertArrayEquals(prob, new double[] { 0.75, 0.25 }, 0.00001);
    }

    @Test
    public void selectAndSumTest() {
        Factor factor = new Factor();
        factor.setDimensionIDs(new int[] { 0, 1, 2 });
        factor.setDimensions(new int[] { 2, 2, 2 });
        factor.setValues(new DoubleArrayWrapper(ArrayUtils.flatten(new double[][][] { { { 0.5, 0.5 }, { 1.0, 0.0 } },
                { { 0.4, 0.6 }, { 0.3, 0.7 } } })));
        factor.select(0, 0);
        double[] prob = MathUtils.normalize(factor.sum(-1));
        assertArrayEquals(prob, new double[] { 0.75, 0.25 }, 0.00001);

        factor.select(0, -1);
        factor.select(1, 1);
        prob = MathUtils.normalize(factor.sum(-1));
        assertArrayEquals(prob, new double[] { 0.65, 0.35 }, 0.00001);
    }

    @Test
    public void sumMiddleTest1() {
        Factor factor = new Factor();
        factor.setDimensionIDs(new int[] { 0, 1, 2 });
        factor.setDimensions(new int[] { 2, 2, 2 });
        factor.setValues(new DoubleArrayWrapper(ArrayUtils.flatten(new double[][][] { { { 0.5, 0.5 }, { 1.0, 0.0 } },
                { { 0.4, 0.6 }, { 0.3, 0.7 } } })));
        factor.select(2, 0);
        double[] prob = MathUtils.normalize(factor.sum(1));
        assertArrayEquals(prob, new double[] { 0.9 / 2.2, 1.3 / 2.2 }, 0.00001);
    }

    @Test
    public void sumMiddleTest2() {
        Factor factor = new Factor();
        factor.setDimensionIDs(new int[] { 0, 1, 2 });
        factor.setDimensions(new int[] { 2, 2, 2 });
        factor.setValues(new DoubleArrayWrapper(ArrayUtils.flatten(new double[][][] { { { 0.5, 0.5 }, { 1.0, 0.0 } },
                { { 0.4, 0.6 }, { 0.3, 0.7 } } })));
        factor.select(0, 1);
        factor.select(2, 1);
        double[] prob = MathUtils.normalize(factor.sum(1));
        assertArrayEquals(prob, new double[] { 0.6 / 1.3, 0.7 / 1.3 }, 0.00001);
    }

    @Test
    public void multiplicationTest() {
        Factor f1 = new Factor();
        f1.setDimensions(new int[] { 2, 2, 2 });
        f1.setDimensionIDs(new int[] { 0, 1, 2 });
        f1.setValues(new DoubleArrayWrapper(new double[] { 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5 }));

        Factor f2 = new Factor();
        f2.setDimensions(new int[] { 2, 2 });
        f2.setDimensionIDs(new int[] { 2, 0 });
        f2.setValues(new DoubleArrayWrapper(new double[] { 1.0, 0.0, 0.0, 1.0 }));

        f1.multiplyCompatible(f2);
        assertArrayEquals(f1.getValues().getDouble(), new double[] { 0.5, 0.0, 0.5, 0.0, 0.0, 0.5, 0.0, 0.5 }, 0.0001);
    }

    @Test
    public void preparedSumTest() {
        Factor f = new Factor();
        f.setDimensions(new int[] { 4, 4, 4 });
        f.setDimensionIDs(new int[] { 0, 1, 2 });
        f.fill(1);

        Factor f2 = new Factor();
        f2.setDimensions(new int[] { 4 });
        f2.setDimensionIDs(new int[] { 2 });

        f.sumPrepared(f2.getValues(), f.prepareMultiplication(f2));
        
        assertArrayEquals(f.sum(-1),f2.getValues().getDouble(),0.0000001);
    }

}
