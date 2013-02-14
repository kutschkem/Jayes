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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.recommenders.jayes.util.Graph;

/**
 * Graph elimination based on greedy minimum fill-in heuristic. Tie-breaking is done by using weights on the nodes. On
 * tie, the node is chosen that will result in the cluster with a minimal sum of node weights
 */
public class GraphElimination implements Iterable<List<Integer>> {

    private Graph graph;
    private double[] nodeWeights;
    private IEliminationHeuristic heuristic = new MinFillIn();

    public GraphElimination(Graph graph, double[] nodeWeights, IEliminationHeuristic heuristic) {
        this.nodeWeights = nodeWeights;
        this.graph = graph;
        this.heuristic = heuristic;
    }

    private List<Integer> getNodeList() {
        final List<Integer> moralNodes = new LinkedList<Integer>();
        for (int i = 0; i < graph.getAdjacency().size(); i++) {
            moralNodes.add(i);
        }
        return moralNodes;
    }

    @Override
    public Iterator<List<Integer>> iterator() {
        return new Iterator<List<Integer>>() {

            private List<Integer> nodes = getNodeList();
            private QuotientGraph graph = new QuotientGraph(GraphElimination.this.graph);

            @Override
            public boolean hasNext() {
                return !nodes.isEmpty();
            }

            @Override
            public List<Integer> next() {
                int next = nextTriangulationNode();
                nodes.remove(Integer.valueOf(next));
                List<Integer> result = createClique(next);
                graph.eliminate(next);
                return result;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();

            }

            private int nextTriangulationNode() {
                int minCost = Integer.MAX_VALUE;
                Double nextClusterWeight = Double.MAX_VALUE;
                int returnNode = 0;

                for (final int node : nodes) {
                    final int predictedCost = heuristic.getHeuristicValue(graph, node);
                    if (predictedCost <= minCost) {
                        final double clusterWeight = computeClusterWeight(node);
                        if ((predictedCost < minCost) || (clusterWeight < nextClusterWeight)) {
                            returnNode = node;
                            minCost = predictedCost;
                            nextClusterWeight = clusterWeight;
                        }
                    }
                }

                return returnNode;
            }

            private double computeClusterWeight(final int node) {
                double clSize = nodeWeights[node];
                for (final int neighbor : graph.getNeighbors(node)) {
                    clSize += nodeWeights[neighbor];
                }
                return clSize;
            }

            private List<Integer> createClique(final int centerNode) {
                final List<Integer> clique = new ArrayList<Integer>();
                clique.add(centerNode);
                for (final int neighbor : graph.getNeighbors(centerNode)) {
                    clique.add(neighbor);
                }
                return clique;
            }

        };
    }

}
