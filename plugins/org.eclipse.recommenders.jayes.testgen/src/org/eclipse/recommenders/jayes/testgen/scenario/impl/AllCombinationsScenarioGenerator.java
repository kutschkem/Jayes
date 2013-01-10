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
package org.eclipse.recommenders.jayes.testgen.scenario.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.recommenders.jayes.BayesNet;
import org.eclipse.recommenders.jayes.BayesNode;
import org.eclipse.recommenders.jayes.testgen.sat.HornClause;
import org.eclipse.recommenders.jayes.testgen.sat.HornSAT;
import org.eclipse.recommenders.jayes.testgen.sat.Learner;
import org.eclipse.recommenders.jayes.testgen.scenario.ScenarioGenerator;
import org.eclipse.recommenders.jayes.util.BidirectionalMap;
import org.eclipse.recommenders.jayes.util.Pair;

/**
 * generates all possible combinations of outcomes of the observed variables. Any variable that is not set as observed
 * is not going to be observed.
 */
public class AllCombinationsScenarioGenerator implements ScenarioGenerator {

    private List<BayesNode> observed = new ArrayList<BayesNode>();
    Map<BayesNode, String> fixed = new HashMap<BayesNode, String>();
    BayesNet net;
    Learner learner = new Learner();

    public AllCombinationsScenarioGenerator(BayesNet net) {
        this.net = net;
        for (BayesNode n : net.getNodes()) {
            learner.learnZeros(n.getFactor());
            learner.learnOnes(n.getFactor());
        }
    }

    @Override
    public Collection<Map<BayesNode, String>> generate(int number) {
        List<Map<BayesNode, String>> scen = new ArrayList<Map<BayesNode, String>>();
        Map<BayesNode, String> _case = new HashMap<BayesNode, String>(fixed);
        scen.add(_case);
        for (BayesNode node : observed) {
            if (fixed.containsKey(node)) {
                continue;
            }
            List<Map<BayesNode, String>> newscens = new ArrayList<Map<BayesNode, String>>();
            for (Map<BayesNode, String> sceninst : scen) {
                for (int j = 0; j < node.getOutcomeCount(); j++) {
                    Map<BayesNode, String> newlyGen = new HashMap<BayesNode, String>(sceninst);
                    newlyGen.put(node, node.getOutcomeName(j));
                    if (check(newlyGen)) { // only generate valid combinations,
                                           // inference algorithms do not like impossible scenarios
                        newscens.add(newlyGen);
                    }
                }
            }
            scen = newscens;
        }
        return scen;
    }

    private boolean check(Map<BayesNode, String> evidence) {
        HornSAT sat = learner.getSat();
        BidirectionalMap<Pair<Integer, Integer>, Integer> vars = learner.getTranslation();
        sat.resetTempClauses();
        boolean applicable = false;
        for (Entry<BayesNode, String> entry : evidence.entrySet()) {
            Pair<Integer, Integer> p = new Pair<Integer, Integer>(entry.getKey().getId(), entry.getKey()
                    .getOutcomeIndex(entry.getValue()));
            if (vars.containsKey(p)) {
                sat.addTempClause(new HornClause(vars.get(p), Arrays.<Integer> asList()));
                applicable = true;
            }
        }
        if (!applicable)
            return true;
        Collection<Integer> newEv = sat.computeModel();
        if (newEv == null)
            return false;
        return true;

    }

    @Override
    public void addObserved(BayesNode node) {
        observed.add(node);

    }

    @Override
    public void addHidden(BayesNode node) {
        // useless method for this kind of ScenarioGenerator

    }

    @Override
    public void setFixed(BayesNode node, String outcome) {
        fixed.put(node, outcome);
    }

    public void setObserved(List<BayesNode> observed) {
        this.observed = observed;
    }

    public List<BayesNode> getObserved() {
        return observed;
    }

}
