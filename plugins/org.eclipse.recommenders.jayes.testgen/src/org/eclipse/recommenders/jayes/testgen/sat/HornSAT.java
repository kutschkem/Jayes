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
package org.eclipse.recommenders.jayes.testgen.sat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class HornSAT {

    private final List<HornClause> clauses = new ArrayList<HornClause>();
    private final List<HornClause> tempClauses = new LinkedList<HornClause>();

    public void addClause(HornClause clause) {
        clauses.add(clause);
        tempClauses.add(clause);
    }

    /**
     * adds a temporary clause (used for inference, but will be reset if resetTempClauses is called)
     * 
     * @param clause
     */
    public void addTempClause(HornClause clause) {
        tempClauses.add(clause);
    }

    public void resetTempClauses() {
        tempClauses.clear();
        for (HornClause c : clauses) {
            tempClauses.add(c.clone());
        }
    }

    /**
     * computes a minimal model satisfying the Horn formulas
     * 
     * @return the variables that are true in the minimal model
     */
    public Collection<Integer> computeModel() {
        List<HornClause> singletons = getSingletons();
        Map<Integer, List<HornClause>> graph = buildClauseGraph();
        Collection<Integer> trueVars = new LinkedHashSet<Integer>();
        for (HornClause c : singletons)
            if (!propagate(graph, c.getPositiveLiteral(), trueVars))
                return null;
        return trueVars;
    }

    /**
     * 
     * @param var
     *            next true variable
     * @return false = unsatisfiable
     */
    private boolean propagate(Map<Integer, List<HornClause>> graph, Integer var, Collection<Integer> trueVars) {
        if (var == null) {
            return false;
        }
        if (!trueVars.contains(var)) {
            trueVars.add(var);
            for (HornClause ctick : graph.get(var)) {
                ctick.removeNegativeLiteral(var);
                if (!ctick.hasNegativeLiteral()) {
                    if (!propagate(graph, ctick.getPositiveLiteral(), trueVars)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private List<HornClause> getSingletons() {
        List<HornClause> singletons = new ArrayList<HornClause>();
        for (HornClause c : tempClauses)
            if (!c.hasNegativeLiteral())
                singletons.add(c);
        return singletons;
    }

    public List<HornClause> getClauses() {
        return clauses;
    }

    private Map<Integer, List<HornClause>> buildClauseGraph() {
        Map<Integer, List<HornClause>> clauseGraph = new HashMap<Integer, List<HornClause>>();
        for (HornClause c : tempClauses) {
            if (c.getPositiveLiteral() != null) {
                ensureExistence(clauseGraph, c.getPositiveLiteral());
            }
            for (Integer i : c.getNegativeLiterals()) {
                ensureExistence(clauseGraph, i);
                clauseGraph.get(i).add(c);
            }
        }
        return clauseGraph;
    }

    private void ensureExistence(Map<Integer, List<HornClause>> clauseGraph, Integer variable) {
        if (!clauseGraph.containsKey(variable))
            clauseGraph.put(variable, new LinkedList<HornClause>());
    }

}
