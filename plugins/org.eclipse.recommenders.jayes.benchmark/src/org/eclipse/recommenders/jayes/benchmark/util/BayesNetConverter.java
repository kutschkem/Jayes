/*******************************************************************************
 * Copyright (c) 2013 Michael Kutschke.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Michael Kutschke - initial API and implementation
 ******************************************************************************/
package org.eclipse.recommenders.jayes.benchmark.util;

import java.util.Collection;
import java.util.LinkedList;

import org.eclipse.recommenders.commons.bayesnet.BayesianNetwork;
import org.eclipse.recommenders.commons.bayesnet.Node;
import org.eclipse.recommenders.jayes.BayesNet;
import org.eclipse.recommenders.jayes.BayesNode;

import com.google.common.base.Function;

public class BayesNetConverter implements Function<BayesianNetwork, BayesNet> {
    /*
     * most of this code comes directly from org.eclipse.recommenders.internal.completion.rcp.calls.net.BayesNetWrapper
     */
    public BayesNet transform(BayesianNetwork network) {
        BayesNet bayesNet = new BayesNet();
        initializeNodes(network, bayesNet);
        initializeArcs(network, bayesNet);
        initializeProbabilities(network, bayesNet);
        return bayesNet;
    }

    private void initializeNodes(final BayesianNetwork network, BayesNet bayesNet) {
        final Collection<Node> nodes = network.getNodes();
        for (final Node node : nodes) {
            final BayesNode bayesNode = bayesNet.createNode(node.getIdentifier());
            final String[] states = node.getStates();
            for (int i = 0; i < states.length; i++) {
                bayesNode.addOutcome(states[i]);
            }

        }
    }

    private void initializeArcs(final BayesianNetwork network, BayesNet bayesNet) {
        final Collection<Node> nodes = network.getNodes();
        for (final Node node : nodes) {
            final Node[] parents = node.getParents();
            final BayesNode children = bayesNet.getNode(node.getIdentifier());
            final LinkedList<BayesNode> bnParents = new LinkedList<BayesNode>();
            for (int i = 0; i < parents.length; i++) {
                bnParents.add(bayesNet.getNode(parents[i].getIdentifier()));
            }
            children.setParents(bnParents);
        }
    }

    private void initializeProbabilities(final BayesianNetwork network, BayesNet bayesNet) {
        final Collection<Node> nodes = network.getNodes();
        for (final Node node : nodes) {
            final BayesNode bayesNode = bayesNet.getNode(node.getIdentifier());
            bayesNode.setProbabilities(node.getProbabilities());
        }
    }

    @Override
    public BayesNet apply(BayesianNetwork net) {
        return transform(net);
    }

}
