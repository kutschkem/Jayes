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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.recommenders.jayes.BayesNet;
import org.eclipse.recommenders.jayes.BayesNode;
import org.eclipse.recommenders.jayes.inference.IBayesInferer;
import org.eclipse.recommenders.jayes.transformation.IDecompositionStrategy;
import org.eclipse.recommenders.jayes.transformation.util.DecompositionFailedException;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.name.Named;

public class TransformationDecorator implements IBayesInferer {

    public static final String DELEGATE = "delegate";
    public static final String DECOMPOSED_NODES = "decomposedNodes";

    private IBayesInferer delegate;
    private IDecompositionStrategy decomp;
    private List<String> names;

    @Inject(optional = true)
    public void setDecomposedNodes(@Named(DECOMPOSED_NODES) List<String> names) {
        this.names = names;
    }

    @Inject
    public TransformationDecorator(@Named(DELEGATE) IBayesInferer delegate, IDecompositionStrategy decompositionStrat) {
        this.delegate = delegate;
        this.decomp = decompositionStrat;
    }

    @Override
    public void setNetwork(BayesNet bayesNet) {
        BayesNet net = copyNetwork(bayesNet);
        for (BayesNode node : Lists.newArrayList(net.getNodes())) {
            try {
                if (names == null || names.contains(node.getName())) {
                    decomp.decompose(net, node);
                }
            } catch (DecompositionFailedException e) {
                e.printStackTrace();
            }
        }
        delegate.setNetwork(net);
    }

    private BayesNet copyNetwork(BayesNet bayesNet) {
        BayesNet net = new BayesNet();
        net.setName(bayesNet.getName());
        for (BayesNode node : bayesNet.getNodes()) {
            copyNode(net, node);
        }
        return net;
    }

    private BayesNode copyNode(BayesNet net, BayesNode node) {
        BayesNode copy = net.createNode(node.getName());
        copy.addOutcomes(node.getOutcomes().toArray(new String[0]));
        List<BayesNode> newParents = new ArrayList<BayesNode>();
        for (BayesNode parent : node.getParents()) {
            newParents.add(net.getNode(parent.getName()));
        }
        copy.setParents(newParents);
        copy.setProbabilities(node.getFactor().getValues().toDoubleArray().clone());
        return copy;
    }

    @Override
    public void setEvidence(Map<BayesNode, String> evidence) {
        delegate.setEvidence(evidence);
    }

    @Override
    public void addEvidence(BayesNode node, String outcome) {
        delegate.addEvidence(node, outcome);
    }

    @Override
    public Map<BayesNode, String> getEvidence() {
        return delegate.getEvidence();
    }

    @Override
    public double[] getBeliefs(BayesNode node) {
        return delegate.getBeliefs(node);
    }

}
