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
package org.eclipse.recommenders.eval.jayes.util;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.LinkedList;

import org.eclipse.recommenders.commons.bayesnet.BayesianNetwork;
import org.eclipse.recommenders.commons.bayesnet.Node;
import org.eclipse.recommenders.jayes.BayesNet;
import org.eclipse.recommenders.jayes.BayesNode;
import org.eclipse.recommenders.jayes.io.XMLBIFWriter;

public class RecommenderModelLoader {

    private BayesNet bayesNet;

    /**
     * @param args
     * @throws Exception
     * @throws FileNotFoundException
     */
    public static void main(String[] args) throws FileNotFoundException, Exception {
        String model = args[0];

        XMLBIFWriter writer = new XMLBIFWriter();
        writer.writeToFile(load(model), args[1]);

    }

    public static BayesNet load(String model) throws Exception,
            FileNotFoundException {
        BayesianNetwork net = BayesianNetwork.read(new BufferedInputStream(new FileInputStream(model)));

        RecommenderModelLoader modelTrans = new RecommenderModelLoader();
        modelTrans.initializeNetwork(net);
        return modelTrans.bayesNet;
    }

    private void initializeNetwork(final BayesianNetwork network) {
        bayesNet = new BayesNet();
        initializeNodes(network);
        initializeArcs(network);
        initializeProbabilities(network);

    }

    //uses the Jayes 1.0.0 API on purpose
    @SuppressWarnings("deprecation")
    private void initializeNodes(final BayesianNetwork network) {
        final Collection<Node> nodes = network.getNodes();
        for (final Node node : nodes) {
            final BayesNode bayesNode = new BayesNode(node.getIdentifier());
            final String[] states = node.getStates();
            for (int i = 0; i < states.length; i++) {
                bayesNode.addOutcome(states[i]);
            }
            bayesNet.addNode(bayesNode);

        }
    }

    private void initializeArcs(final BayesianNetwork network) {
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

    private void initializeProbabilities(final BayesianNetwork network) {
        final Collection<Node> nodes = network.getNodes();
        for (final Node node : nodes) {
            final BayesNode bayesNode = bayesNet.getNode(node.getIdentifier());
            bayesNode.setProbabilities(node.getProbabilities());
        }
    }

}
