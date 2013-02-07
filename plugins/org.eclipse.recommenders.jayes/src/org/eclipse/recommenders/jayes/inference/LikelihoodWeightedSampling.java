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
package org.eclipse.recommenders.jayes.inference;

import java.util.Map;

import org.eclipse.recommenders.jayes.BayesNet;
import org.eclipse.recommenders.jayes.BayesNode;
import org.eclipse.recommenders.jayes.sampling.BasicSampler;
import org.eclipse.recommenders.jayes.util.MathUtils;

public class LikelihoodWeightedSampling extends AbstractInferer {

    private int sampleCount = 200;
    private BasicSampler sampler = new BasicSampler();

    @Override
    public void setNetwork(BayesNet bn) {
        super.setNetwork(bn);
        sampler.setNetwork(bn);

    }

    @Override
    protected void updateBeliefs() {
        sampler.setEvidence(evidence);
        for (int i = 0; i < sampleCount; i++) {
            Map<BayesNode, String> sample = sampler.sample();
            double weight = computeEvidenceProbability(sample);

            for (BayesNode e : sample.keySet()) {
                beliefs[e.getId()][e.getOutcomeIndex(sample.get(e))] += weight;
            }
        }

        normalizeBeliefs();

    }

    private void normalizeBeliefs() {
        for (int i = 0; i < beliefs.length; i++)
            beliefs[i] = MathUtils.normalize(beliefs[i]);
    }

    private double computeEvidenceProbability(Map<BayesNode, String> sample) {
        double factor = 1.0;
        for (BayesNode n : evidence.keySet()) {
            factor *= n.marginalize(sample)[n.getOutcomeIndex(evidence.get(n))];
        }
        return factor;
    }

    public void setSampleCount(int sampleCount) {
        this.sampleCount = sampleCount;
    }

    public void seed(long seed) {
        sampler.seed(seed);
    }

}
