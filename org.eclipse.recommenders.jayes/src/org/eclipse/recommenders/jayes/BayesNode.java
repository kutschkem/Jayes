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
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.recommenders.jayes.util.BidirectionalMap;
import org.eclipse.recommenders.jayes.util.MathUtils;

public class BayesNode {

    private final String name;
    private final List<BayesNode> children = new ArrayList<BayesNode>();
    private List<BayesNode> parents = new ArrayList<BayesNode>();
    private int outcomes = 0;
    private final BidirectionalMap<String, Integer> outcomeIndices = new BidirectionalMap<String, Integer>();
    private final Factor factor = new Factor();
    private int id = -1;

    public BayesNode(String name) {
        this.name = name;
    }

    public void setProbabilities(final double[] probabilities) {
        factor.setValues(probabilities);
    }

    public List<BayesNode> getChildren() {
        return children;
    }

    public List<BayesNode> getParents() {
        return Collections.unmodifiableList(parents);
    }

    public void setParents(final List<BayesNode> parents) {
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
        final double[] result = MathUtils.normalize(factor.sum(-1));
        factor.resetSelections();

        return result;
    }

    public int getId() {
        return id;
    }

    public void setId(final int id) {
        if (this.id != -1 && this.id != id) {
            throw new IllegalStateException("Impossible to reset Id!");
        }
        this.id = id;
        adjustFactordimensions();

    }

    public int addOutcome(final String name) {
        if (!outcomeIndices.containsKey(name)) {
            outcomeIndices.put(name, outcomes);
            outcomes++;
            adjustFactordimensions();
            return outcomes - 1;
        }
        throw new IllegalArgumentException("Outcome already exists");
    }

    public int getOutcomeIndex(final String name) {
        try {
            return outcomeIndices.get(name);
        } catch (NullPointerException ex) {
            System.out.println("");
            throw ex;
        }
    }

    public String getOutcomeName(final int index) {
        return outcomeIndices.getKey(index);
    }

    public int getOutcomeCount() {
        return outcomes;
    }

    public Factor getFactor() {
        return factor;
    }

    public Set<String> getOutcomes() {
        return outcomeIndices.keySet();
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

}
