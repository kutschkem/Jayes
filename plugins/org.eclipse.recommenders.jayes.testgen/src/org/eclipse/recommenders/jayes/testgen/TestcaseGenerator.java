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
package org.eclipse.recommenders.jayes.testgen;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.eclipse.recommenders.jayes.BayesNet;
import org.eclipse.recommenders.jayes.BayesNode;
import org.eclipse.recommenders.jayes.sampling.BasicSampler;

public class TestcaseGenerator {

    private BasicSampler sampler = new BasicSampler();
    private double unsetRate = 0.2;
    private Set<Integer> hidden = new HashSet<Integer>();
    private Set<Integer> observed = new HashSet<Integer>();
    private Map<BayesNode, String> fixed = new HashMap<BayesNode, String>();
    private Random random = new Random();

    public void setBN(BayesNet bn) {
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
            if ((!observed.contains(n.getId())) && (hidden.contains(n.getId()) || random.nextDouble() < unsetRate)) {
                it.remove();
            }
        }
        sample.putAll(fixed);
        return sample;

    }

    public void addHidden(int node) {
        hidden.add(node);

    }

    public void addObserved(int node) {
        observed.add(node);

    }

    public void set(BayesNode node, String outcome) {
        fixed.put(node, outcome);

    }

    public void seed(long seed) {
        random.setSeed(seed);
        sampler.seed(seed);
    }

}
