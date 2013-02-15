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
package org.eclipse.recommenders.jayes.sampling;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.eclipse.recommenders.jayes.BayesNet;
import org.eclipse.recommenders.jayes.BayesNode;

public class BasicSampler implements ISampler {

    private List<BayesNode> topologicallySortedNodes;
    private Map<BayesNode, String> evidence = Collections.emptyMap();
    private Random random = new Random();

    @Override
    public Map<BayesNode, String> sample() {
        Map<BayesNode, String> result = new HashMap<BayesNode, String>();
        result.putAll(evidence);
        for (BayesNode n : topologicallySortedNodes) {
            if (!evidence.containsKey(n)) {
                int newEvidence = sampleOutcome(n, result);
                result.put(n, n.getOutcomeName(newEvidence));
            }
        }
        return result;

    }

    private int sampleOutcome(BayesNode node, Map<BayesNode, String> currentSample) {
        double[] probs = node.marginalize(currentSample);
        double currentProb = 0;
        int newEvidence = 0;
        double rand = random.nextDouble();
        for (double prob : probs) {
            currentProb += prob;
            if (rand < currentProb) {
                break;
            }
            newEvidence++;
        }
        return Math.min(newEvidence, node.getOutcomeCount() - 1);
    }

    @Override
    public void setNetwork(BayesNet net) {
        topologicallySortedNodes = topsort(net.getNodes());
    }

    private List<BayesNode> topsort(List<BayesNode> list) {
        List<BayesNode> result = new LinkedList<BayesNode>();
        Set<BayesNode> visited = new HashSet<BayesNode>();
        for (BayesNode n : list)
            depthFirstSearch(n, visited, result);
        Collections.reverse(result);
        return result;
    }

    private void depthFirstSearch(BayesNode n, Set<BayesNode> visited, List<BayesNode> finished) {
        if (visited.contains(n))
            return;
        visited.add(n);
        for (BayesNode c : n.getChildren())
            depthFirstSearch(c, visited, finished);
        finished.add(n);
    }

    @Override
    public void setEvidence(Map<BayesNode, String> evidence) {
        this.evidence = evidence;

    }

    @Override
    public void seed(long seed) {
        random.setSeed(seed);
    }

    @Override
    @Deprecated
    public void setBN(BayesNet net) {
        setNetwork(net);

    }

}
