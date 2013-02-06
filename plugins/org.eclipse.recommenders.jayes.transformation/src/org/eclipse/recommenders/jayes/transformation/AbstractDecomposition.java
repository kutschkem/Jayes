/*******************************************************************************
 * Copyright (c) 2012 Michael Kutschke. All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Michael Kutschke - initial API and implementation
 ******************************************************************************/
package org.eclipse.recommenders.jayes.transformation;

import static org.eclipse.recommenders.jayes.transformation.util.ArrayFlatten.flatten;
import static org.eclipse.recommenders.jayes.transformation.util.ArrayFlatten.unflatten;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.recommenders.jayes.BayesNet;
import org.eclipse.recommenders.jayes.BayesNode;
import org.eclipse.recommenders.jayes.factor.AbstractFactor;
import org.eclipse.recommenders.jayes.factor.DenseFactor;
import org.eclipse.recommenders.jayes.factor.arraywrapper.DoubleArrayWrapper;
import org.eclipse.recommenders.jayes.transformation.util.CanonicalDoubleArrayManager;
import org.eclipse.recommenders.jayes.transformation.util.DecompositionFailedException;
import org.eclipse.recommenders.jayes.util.MathUtils;

import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;

/**
 * Abstract base class for Matrix Decomposition classes used for probability distributions.
 */
public abstract class AbstractDecomposition implements IDecompositionStrategy {

    @Override
    public final void decompose(BayesNet net, BayesNode node) throws DecompositionFailedException {
        if (!net.getNodes().contains(node)) {
            throw new IllegalArgumentException("Node " + node + " is not part of the bayesnet " + net.getName());
        }

        AbstractFactor f = node.getFactor();
        if (f.getDimensions().length == 1) {
            // in a bayesian network, there are no 0-dimensional factors
            throw new DecompositionFailedException("Node " + node + " has no parents, impossible to decompose");
        }
        f = reorderFactor(f);
        int[] dimensions = f.getDimensions();
        List<double[]> basis;
        double[] latentProb;
        // TODO this line is one of those keeping this method from working with SparseFactor
        // (and consequently BayesNode from using SparseFactor as well)
        List<double[]> vectors = unflatten(f.getValues().toDoubleArray(), dimensions[dimensions.length - 1]);
        basis = getBasis(f, vectors);

        latentProb = getLatentProbabilities(vectors, basis);

        if (f == node.getFactor()) {
            // there was no reordering
            createLatentNodeInOriginalOrder(net, node, basis, latentProb);
        } else {
            createLatentNodeReordered(net, node, f, basis, latentProb);
        }
    }

    private AbstractFactor reorderFactor(AbstractFactor f) {
        int[] dimensions = f.getDimensions();
        int min = Ints.min(dimensions);
        int minIndex = Ints.lastIndexOf(dimensions, min);

        if (minIndex == dimensions.length - 1)
            return f;
        int[] nDim = rotateRight(dimensions, dimensions.length - 1 - minIndex);
        int[] nIDs = rotateRight(f.getDimensionIDs(), dimensions.length - 1 - minIndex);

        AbstractFactor f2 = new DenseFactor();
        f2.setDimensionIDs(nIDs);
        f2.setDimensions(nDim);
        f2.fill(1);
        f2.multiplyCompatible(f);

        return f2;
    }

    protected abstract List<double[]> getBasis(AbstractFactor f, List<double[]> vectors)
            throws DecompositionFailedException;

    private double[] getLatentProbabilities(List<double[]> vectors, List<double[]> best)
            throws DecompositionFailedException {
        CanonicalDoubleArrayManager canon = new CanonicalDoubleArrayManager(); // to make sure equals will work
        best = Lists.transform(best, canon);
        vectors = Lists.transform(vectors, canon);

        List<double[]> newVectors = toLatentSpace(vectors, best);
        return flatten(newVectors.toArray(new double[0][]));
    }

    private List<double[]> toLatentSpace(List<double[]> vectors, List<double[]> best)
            throws DecompositionFailedException {
        List<double[]> latent = new ArrayList<double[]>();
        for (double[] v : vectors) {
            latent.add(toLatentSpace(v, best));
        }
        return latent;
    }

    protected abstract double[] toLatentSpace(double[] v, List<double[]> best) throws DecompositionFailedException;

    /**
     * Example: assume C as the least outcomes, A and B are parents of C, C is decomposed
     * 
     * <pre>
     * A -> C      =>    A -> latent-C -> C
     * B /               B /
     * </pre>
     **/
    private void createLatentNodeInOriginalOrder(BayesNet net, BayesNode node, List<double[]> basis, double[] latentProb) {
        BayesNode newNode = net.createNode("latent-" + node.getName());
        addOutcomes(newNode, basis.size());
        newNode.setParents(node.getParents());
        newNode.setProbabilities(latentProb);

        node.setParents(Arrays.asList(newNode));
        node.setProbabilities(flatten(basis.toArray(new double[0][])));
    }

    /**
     * Example: assume B is the least outcomes, A and B are parents of C, C is decomposed
     * 
     * <pre>
     * A -> C                   =>   A -------------> C
     * B /                           B -> latent-C /
     * </pre>
     **/
    private void createLatentNodeReordered(BayesNet net, BayesNode node, AbstractFactor f, List<double[]> basis,
            double[] latentProb) {
        BayesNode newNode = net.createNode("latent-" + node.getName());
        addOutcomes(newNode, basis.size());
        int[] dimensions = f.getDimensions();
        BayesNode parentNode = net.getNode(f.getDimensionIDs()[dimensions.length - 1]);
        newNode.setParents(Arrays.asList(parentNode));
        newNode.setProbabilities(flatten(transpose(basis).toArray(new double[0][])));

        List<BayesNode> parents = new ArrayList<BayesNode>(node.getParents());
        int index = parents.indexOf(parentNode);
        parents.remove(parentNode);
        parents.add(index, newNode);
        node.setParents(parents);
        double[] nodeProbs = undoReordering(latentProb, node.getFactor(), f, newNode.getId());
        node.setProbabilities(nodeProbs);
    }

    private void addOutcomes(BayesNode newNode, int d) {
        for (int i = 0; i < d; i++) {
            newNode.addOutcome("outcome" + i);
        }

    }

    private double[] undoReordering(double[] latentProb, AbstractFactor originalFactor, AbstractFactor newFactor,
            int originalId) {
        AbstractFactor o2 = originalFactor.clone();
        AbstractFactor n2 = newFactor.clone();
        n2.getDimensionIDs()[n2.getDimensionIDs().length - 1] = originalId;
        int oInd = Ints.indexOf(o2.getDimensionIDs(), originalId);
        n2.getDimensions()[n2.getDimensions().length - 1] = o2.getDimensions()[oInd];
        n2.setValues(new DoubleArrayWrapper(latentProb));
        o2.setValues(new DoubleArrayWrapper(new double[MathUtils.product(o2.getDimensions())]));
        o2.fill(1);
        o2.multiplyCompatible(n2);
        return o2.getValues().toDoubleArray();
    }

    protected final List<double[]> transpose(List<double[]> best) {
        List<double[]> result = new ArrayList<double[]>();
        for (int i = 0; i < best.get(0).length; i++) {
            result.add(new double[best.size()]);
        }

        for (int i = 0; i < best.size(); i++) {
            double[] arr = best.get(i);
            for (int j = 0; j < arr.length; j++) {
                result.get(j)[i] = arr[j];
            }
        }
        return result;
    }

    private int[] rotateRight(int[] array, int amount) {
        int[] result = new int[array.length];
        System.arraycopy(array, 0, result, amount, array.length - amount);
        System.arraycopy(array, array.length - amount, result, 0, amount);
        return result;
    }

}
