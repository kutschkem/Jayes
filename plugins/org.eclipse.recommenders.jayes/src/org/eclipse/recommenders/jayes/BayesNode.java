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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.eclipse.recommenders.internal.jayes.util.BidirectionalMap;
import org.eclipse.recommenders.jayes.factor.AbstractFactor;
import org.eclipse.recommenders.jayes.factor.DenseFactor;
import org.eclipse.recommenders.jayes.factor.arraywrapper.DoubleArrayWrapper;
import org.eclipse.recommenders.jayes.util.MathUtils;

public class BayesNode {

    private final String name;
    private final List<BayesNode> children = new ArrayList<BayesNode>();
    private List<BayesNode> parents = new ArrayList<BayesNode>();
    private int outcomes = 0;
    private final BidirectionalMap<String, Integer> outcomeIndices = new BidirectionalMap<String, Integer>();
    private final AbstractFactor factor = new DenseFactor();
    private int id = -1;
    private final List<String> outcomesList = new ArrayList<String>();

    /**
     * @deprecated use {@link BayesNet#createNode(String) BayesNet.createNode} instead
     */
    @Deprecated
    public BayesNode(String name) {
        this.name = name;
    }

    /**
     * Must be called after the parents and outcomes, and the outcome of the parents are set.
     */
    public void setProbabilities(final double... probabilities) {
        adjustFactordimensions();
        if (probabilities.length != MathUtils.product(factor.getDimensions())) {
            throw new IllegalArgumentException("Probability table does not have expected size. Expected: "
                    + MathUtils.product(factor.getDimensions()) + "but got: " + probabilities.length);
        }
        factor.setValues(new DoubleArrayWrapper(probabilities));
    }

    public double[] getProbabilities() {
        return factor.getValues().toDoubleArray();
    }

    public List<BayesNode> getChildren() {
        return children;
    }

    public List<BayesNode> getParents() {
        return Collections.unmodifiableList(parents);
    }

    public void setParents(final List<BayesNode> parents) {
        for (BayesNode oldParent : this.parents) {
            oldParent.children.remove(this);
        }
        this.parents = parents;
        for (BayesNode p : parents) {
            p.children.add(this);
        }
        adjustFactordimensions();
    }

    private void adjustFactordimensions() {
        final int[] dimensions = new int[parents.size() + 1];
        final int[] dimensionIds = new int[parents.size() + 1];
        fillWithParentDimensions(dimensions, dimensionIds);
        insertSelf(dimensions, dimensionIds);
        factor.setDimensions(dimensions);
        factor.setDimensionIDs(dimensionIds);

    }

    private void insertSelf(final int[] dimensions, final int[] dimensionIds) {
        dimensions[dimensions.length - 1] = getOutcomeCount();
        dimensionIds[dimensionIds.length - 1] = getId();
    }

    private void fillWithParentDimensions(final int[] dimensions, final int[] dimensionIds) {
        for (ListIterator<BayesNode> it = parents.listIterator(); it.hasNext();) {
            final BayesNode p = it.next();
            dimensions[it.nextIndex() - 1] = p.getOutcomeCount();
            dimensionIds[it.nextIndex() - 1] = p.getId();
        }
    }

    public double[] marginalize(final Map<BayesNode, String> evidence) {
        for (final BayesNode p : parents) {
            if (evidence.containsKey(p)) {
                factor.select(p.getId(), p.getOutcomeIndex(evidence.get(p)));
            } else {
                factor.select(p.getId(), -1);
            }
        }
        final double[] result = MathUtils.normalize(factor.marginalizeAllBut(-1));
        factor.resetSelections();

        return result;
    }

    public int getId() {
        return id;
    }

    /**
     * @deprecated internal method, don't use. visibility might change to default
     */
    @Deprecated
    public void setId(final int id) {
        if (this.id != -1) {
            throw new IllegalStateException("Impossible to reset Id!");
        }
        if (id < 0) {
            throw new IllegalArgumentException("id has to be greater or equal to 0");
        }
        this.id = id;

    }

    public void addOutcomes(String... names) {
        if (!Collections.disjoint(outcomesList, Arrays.asList(names))) {
            throw new IllegalArgumentException("Outcome already exists");
        }
        for (String name : names) {
            outcomeIndices.put(name, outcomes);
            outcomes++;
            outcomesList.add(name);
        }
        adjustFactordimensions();
    }

    public int addOutcome(final String name) {
        addOutcomes(name);
        return outcomes - 1;
    }

    public int getOutcomeIndex(final String name) {
        try {
            return outcomeIndices.get(name);
        } catch (NullPointerException ex) {
            throw new IllegalArgumentException(name, ex);
        }
    }

    public String getOutcomeName(final int index) {
        return outcomeIndices.getKey(index);
    }

    public int getOutcomeCount() {
        return outcomes;
    }

    public AbstractFactor getFactor() {
        return factor;
    }

    public List<String> getOutcomes() {
        return Collections.unmodifiableList(outcomesList);
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

}
