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
package org.eclipse.recommenders.jayes.benchmark;

import java.util.List;
import java.util.Map;

import org.eclipse.recommenders.jayes.BayesNet;
import org.eclipse.recommenders.jayes.BayesNode;
import org.eclipse.recommenders.jayes.benchmark.util.ModelLoader;
import org.eclipse.recommenders.jayes.inference.junctionTree.JunctionTreeAlgorithm;
import org.eclipse.recommenders.jayes.testgen.scenario.impl.SampledScenarioGenerator;
import org.eclipse.recommenders.jayes.transformation.IDecompositionStrategy;
import org.eclipse.recommenders.jayes.transformation.SmoothedFactorDecomposition;
import org.eclipse.recommenders.jayes.util.MathUtils;

import com.google.caliper.SimpleBenchmark;
import com.google.common.collect.Lists;

public class InferenceBenchmark extends SimpleBenchmark {

    private static final double EVIDENCE_RATE = 0.5;
    private static final int SEED = 1337;
    private BayesNet net;
    private SampledScenarioGenerator scenarioGen = new SampledScenarioGenerator();
    private JunctionTreeAlgorithm algo = new JunctionTreeAlgorithm();
    private JunctionTreeAlgorithm algoTransformed = new JunctionTreeAlgorithm();
    private int mergeNr = 100;

    public InferenceBenchmark() throws Exception {
        ModelLoader modelLoader = new ModelLoader("jre:jre:zip:call:1.0.0");
        List<BayesNet> networksToMerge = modelLoader.getJayesNetworks().subList(0, mergeNr);
        this.net = mergeNetworks(networksToMerge);
        scenarioGen.seed(SEED);
        scenarioGen.setEvidenceRate(EVIDENCE_RATE);
        scenarioGen.setNetwork(net);
        algo.setNetwork(net);
        BayesNet transformedNet = mergeNetworks(networksToMerge);
        IDecompositionStrategy decomp = new SmoothedFactorDecomposition();
        for (BayesNet network : networksToMerge) {
            BayesNode node = transformedNet.getNode(network.hashCode() + "contexts");
            if (node != null) {
                decomp.decompose(transformedNet, node);
            }
        }
        algoTransformed.setNetwork(transformedNet);

    }

    private BayesNet mergeNetworks(List<BayesNet> jayesNetworks) {
        BayesNet merged = new BayesNet();
        for (BayesNet net : jayesNetworks) {
            for (BayesNode node : net.getNodes()) {
                BayesNode clone = merged.createNode(net.hashCode() + node.getName());
                clone.addOutcomes(node.getOutcomes().toArray(new String[0]));
                List<BayesNode> parents = Lists.newArrayList();
                for (BayesNode origParent : node.getParents()) {
                    parents.add(merged.getNode(net.hashCode() + origParent.getName()));
                }
                clone.setParents(parents);
                clone.setProbabilities(node.getFactor().getValues().toDoubleArray());
            }
        }
        return merged;
    }

    public int timeInference(int repetitions) {
        int result = 0;
        for (Map<BayesNode, String> evidence : scenarioGen.generate(repetitions)) {
            algo.setEvidence(evidence);
            for (BayesNode node : net.getNodes()) {
                result += MathUtils.sum(algo.getBeliefs(node));
            }
        }
        return result;
    }

    public int timeInferenceTransformed(int repetitions) {
        int result = 0;
        for (Map<BayesNode, String> evidence : scenarioGen.generate(repetitions)) {
            algoTransformed.setEvidence(evidence);
            for (BayesNode node : net.getNodes()) {
                result += MathUtils.sum(algoTransformed.getBeliefs(node));
            }
        }
        return result;
    }

}
