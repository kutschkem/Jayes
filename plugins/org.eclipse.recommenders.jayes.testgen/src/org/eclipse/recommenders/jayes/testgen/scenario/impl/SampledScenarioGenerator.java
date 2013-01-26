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
package org.eclipse.recommenders.jayes.testgen.scenario.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.eclipse.recommenders.jayes.BayesNet;
import org.eclipse.recommenders.jayes.BayesNode;
import org.eclipse.recommenders.jayes.sampling.BasicSampler;
import org.eclipse.recommenders.jayes.testgen.scenario.ScenarioGenerator;

/**
 * generates scenarios by sampling the probability distribution. This scenario generator allows setting an evidence
 * rate, which means that any variable will be observed with that probability.
 */
public class SampledScenarioGenerator implements ScenarioGenerator {

    private BasicSampler sampler;
    private double unsetRate = 0.2;
    private Set<BayesNode> hidden = new HashSet<BayesNode>();
    private Set<BayesNode> observed = new HashSet<BayesNode>();
    private Map<BayesNode, String> fixed = new HashMap<BayesNode, String>();
    private Random random = new Random();

    public SampledScenarioGenerator(BasicSampler sampler) {
        this.sampler = sampler;
    }

    public SampledScenarioGenerator() {
        this(new BasicSampler());
    }

    @SuppressWarnings("deprecation")
    // use Jayes 1.0.0 API here
    public void setNetwork(BayesNet bn) {
        sampler.setBN(bn);
    }

    public void setEvidenceRate(double rate) {
        if (rate > 1 || rate < 0) {
            throw new IllegalArgumentException("Rate must be between 0.0 and 1.0, but was: " + rate);
        }
        unsetRate = 1 - rate;
    }

    public Map<BayesNode, String> testcase() {
        Map<BayesNode, String> sample = sampler.sample();

        Iterator<BayesNode> it = sample.keySet().iterator();
        while (it.hasNext()) {
            BayesNode n = it.next();
            if ((!observed.contains(n)) && (hidden.contains(n) || random.nextDouble() < unsetRate)) {
                it.remove();
            }
        }
        sample.putAll(fixed);
        return sample;

    }

    @Override
    public void addHidden(BayesNode node) {
        hidden.add(node);

    }

    @Override
    public void addObserved(BayesNode node) {
        observed.add(node);

    }

    @Override
    public void setFixed(BayesNode node, String outcome) {
        fixed.put(node, outcome);

    }

    public void seed(long seed) {
        random.setSeed(seed);
        sampler.seed(seed);
    }

    @Override
    public Collection<Map<BayesNode, String>> generate(int number) {
        List<Map<BayesNode, String>> scenarios = new ArrayList<Map<BayesNode, String>>();
        for (int i = 0; i < number; i++) {
            scenarios.add(testcase());
        }
        return scenarios;
    }

}
