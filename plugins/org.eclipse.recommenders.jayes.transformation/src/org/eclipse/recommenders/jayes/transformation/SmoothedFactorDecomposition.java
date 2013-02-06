/*******************************************************************************
 * Copyright (c) 2012 Michael Kutschke. All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Michael Kutschke - initial API and implementation
 ******************************************************************************/
package org.eclipse.recommenders.jayes.transformation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.math.linear.MatrixUtils;
import org.eclipse.recommenders.jayes.factor.AbstractFactor;
import org.eclipse.recommenders.jayes.util.MathUtils;

import com.google.common.primitives.Doubles;

/**
 * Applicable if the conditional probability distribution is, in essence, sparse, but was smoothed (which makes it
 * non-sparse). The decomposition makes this explicit in the model.<br/>
 * 
 * <pre>
 * Example:
 *  {
 *    { 0.99 0.01 }
 *    { 0.01 0.99 }
 *    { 0.01 0.99 }
 *  }
 * 
 *  would decompose to the new basis
 *  
 *  {
 *    { 1 1 }
 *    { 0 1 }
 *    { 1 0 }
 *  }
 *  
 *  and 
 *  {
 *    { 0.01 0 1 }
 *    { 0.01 1 0 }
 *    { 0.01 1 0 }
 *  }
 * </pre>
 * 
 * <br/>
 * This works better than {@link LatentDeterministicDecomposition} in cases where the distribution is less close to
 * being deterministic. A good indicator for when to use it is the size of the dimensions. With large dimensions, it
 * becomes unlikely that the distributions can be represented by just one basis vector. In that case,
 * {@link SmoothedFactorDecomposition} works better.
 * 
 */
public class SmoothedFactorDecomposition extends AbstractDecomposition {

    @Override
    protected double[] toLatentSpace(double[] v, List<double[]> best) {
        double min = Doubles.min(v);
        double[] latent = new double[v.length + 1];
        latent[0] = min;
        double[] h = new double[v.length];
        Arrays.fill(h, min);
        MathUtils.secureSubtract(v, h, h);
        System.arraycopy(h, 0, latent, 1, h.length);
        return latent;
    }

    @Override
    protected List<double[]> getBasis(AbstractFactor f, List<double[]> vectors) {
        List<double[]> basis = new ArrayList<double[]>();
        int d = MathUtils.product(f.getDimensions()) / vectors.size();
        double[] ones = new double[d];
        Arrays.fill(ones, 1);
        basis.add(ones);

        for (double[] e : MatrixUtils.createRealIdentityMatrix(d).getData()) {
            basis.add(e);
        }
        return basis;
    }

}
