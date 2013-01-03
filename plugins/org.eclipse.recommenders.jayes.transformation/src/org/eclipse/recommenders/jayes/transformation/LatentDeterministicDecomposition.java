/*******************************************************************************
 * Copyright (c) 2012 Michael Kutschke. All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Michael Kutschke - initial API and implementation
 ******************************************************************************/
package org.eclipse.recommenders.jayes.transformation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;

import org.apache.commons.math.optimization.GoalType;
import org.apache.commons.math.optimization.OptimizationException;
import org.apache.commons.math.optimization.RealPointValuePair;
import org.apache.commons.math.optimization.linear.LinearConstraint;
import org.apache.commons.math.optimization.linear.LinearObjectiveFunction;
import org.apache.commons.math.optimization.linear.Relationship;
import org.apache.commons.math.optimization.linear.SimplexSolver;
import org.eclipse.recommenders.jayes.factor.AbstractFactor;
import org.eclipse.recommenders.jayes.transformation.util.CanonicalDoubleArrayManager;
import org.eclipse.recommenders.jayes.transformation.util.DecompositionFailedException;

import com.google.common.base.Functions;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

/**
 * To be used when a conditional probability density consists of mostly the same few distributions, which is often the
 * case when a deterministic distribution is smoothed. <br/>
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
 *    { 0.01 0.99 }
 *    { 0.99 0.01 }
 *  }
 *  
 *  and 
 *  {
 *    { 0 1 }
 *    { 1 0 }
 *    { 1 0 }
 *  }
 * </pre>
 * 
 * <br/>
 * The decomposition is still applicable when there are some parts of the distribution that can not be directly
 * expressed by one of the basis vectors. In that case, that particular distribution will be represented as non-negative
 * linear combination of the basis vectors. Note that because of that, it is possible that the decomposition fails.
 * 
 */
public class LatentDeterministicDecomposition extends AbstractDecomposition {

    @Override
    protected List<double[]> getBasis(AbstractFactor f, List<double[]> vectors) throws DecompositionFailedException {
        Map<double[], Integer> counts = count(vectors);
        List<double[]> basis = getBest(counts, f.getValues().length() / vectors.size(), vectors.size() / 2);
        if (basis == null)
            throw new DecompositionFailedException("Could not find a good enough basis");
        return basis;
    }

    private Map<double[], Integer> count(List<double[]> vectors) {
        Map<double[], Integer> counts = new HashMap<double[], Integer>();
        for (double[] vector : Lists.transform(vectors, new CanonicalDoubleArrayManager())) {
            if (!counts.containsKey(vector)) {
                counts.put(vector, 0);
            }
            counts.put(vector, counts.get(vector) + 1);
        }

        return counts;
    }

    private List<double[]> getBest(final Map<double[], Integer> counts, int basisSize,
            int minTotalCounts) {
        PriorityQueue<double[]> q = new PriorityQueue<double[]>(basisSize,
                Ordering.natural().onResultOf(Functions.forMap(counts)));

        for (Entry<double[], Integer> e : counts.entrySet()) {
            if (q.isEmpty() || q.size() < basisSize) {
                q.add(e.getKey());
            } else {
                double[] head = q.peek();
                if (counts.get(head) < counts.get(e.getKey())) {
                    q.remove();
                    q.add(e.getKey());
                }
            }
        }

        int totalcounts = 0;
        for (double[] v : q) {
            totalcounts += counts.get(v);
        }
        if (totalcounts < minTotalCounts)
            return null;

        return new ArrayList<double[]>(q);
    }

    @Override
    protected double[] toLatentSpace(double[] v, List<double[]> basis)
            throws DecompositionFailedException {
        // we can assume here that equals works, we canonized everything before!
        int ind = basis.indexOf(v);
        if (ind != -1) {
            double[] l = new double[v.length];
            l[ind] = 1;
            return l;
        }
        // have to figure out a suitable non-negative linear combination of the base vectors
        // -> use simplex
        List<double[]> transposedBasis = transpose(basis);

        List<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        for (int i = 0; i < v.length; i++) {
            LinearConstraint c = new LinearConstraint(transposedBasis.get(i),
                    Relationship.EQ, v[i]);
            constraints.add(c);
        }

        LinearObjectiveFunction obj = new LinearObjectiveFunction(
                new double[v.length], 0);

        RealPointValuePair result;
        try {
            result = new SimplexSolver().optimize(obj,
                    constraints, GoalType.MINIMIZE, true);
        } catch (OptimizationException e) {
            throw new DecompositionFailedException(e);
        }

        return result.getPoint();
    }

}
