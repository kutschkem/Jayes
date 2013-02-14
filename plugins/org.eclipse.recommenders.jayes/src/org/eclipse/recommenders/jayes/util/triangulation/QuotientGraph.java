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
package org.eclipse.recommenders.jayes.util.triangulation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.recommenders.jayes.util.Graph;
import org.eclipse.recommenders.jayes.util.Graph.Edge;

/**
 * Quotient graphs are special data structures for the perfect elimination order problem. Their size stays in O(|E|)
 * where E is the set of edges. Using plain graphs would result in a storage complexity of O(|E*|), where E* is the set
 * of Edges united with the set of "fill-in" edges generated during elimination. <br/>
 * <br/>
 * See "An Approximate Minimum Degree Ordering Algorithm" (Amestoy et al. 1996)
 */
public class QuotientGraph {

    private Graph variables;
    private Graph variablesToElements;

    private final Map<Integer, Set<Integer>> neighborCache = new HashMap<Integer, Set<Integer>>();

    public QuotientGraph(Graph graph) {
        this.variables = graph.clone();
        this.variablesToElements = new Graph();
        variablesToElements.initialize(variables.getAdjacency().size());
    }

    public Set<Integer> getNeighbors(int variable) {
        if (neighborCache.containsKey(variable)) {
            return neighborCache.get(variable);
        }
        Set<Integer> neighbors = new HashSet<Integer>();
        neighbors.addAll(getNeighbors(variables, variable));
        for (Edge e : variablesToElements.getIncidentEdges(variable)) {
            neighbors.addAll(getNeighbors(variablesToElements, e.getSecond()));
        }
        neighbors.remove(variable);
        neighborCache.put(variable, Collections.unmodifiableSet(neighbors));
        return Collections.unmodifiableSet(neighbors);
    }

    public void eliminate(int variable) {
        for (int elementNeighbor : getNeighbors(variablesToElements, variable)) { // merge eliminated nodes
            merge(variablesToElements, variable, elementNeighbor);
        }
        for (Edge e : variables.getIncidentEdges(variable)) { // interconnect neigbors
            variablesToElements.addEdge(variable, e.getSecond());
        }
        virtualRemoveNode(variables, variable);
        neighborCache.clear();
    }

    private List<Integer> getNeighbors(Graph graph, int var) {
        List<Integer> elementNeighbors = new ArrayList<Integer>();
        for (Edge e : graph.getIncidentEdges(var)) {
            elementNeighbors.add(e.getSecond());
        }
        return elementNeighbors;
    }

    public void merge(Graph graph, int v1, int v2) {
        for (int e2 : getNeighbors(graph, v2)) {
            if (v1 != e2) {
                graph.addEdge(v1, e2);
            }
        }
        virtualRemoveNode(graph, v2);
    }

    // isolating node = virtually removing it
    private void virtualRemoveNode(Graph graph, final int node) {
        while (!graph.getIncidentEdges(node).isEmpty()) {
            graph.removeEdge(graph.getIncidentEdges(node).iterator().next());
        }
    }

    // TODO indistinguishable variables and external degree

}
