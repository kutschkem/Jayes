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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.recommenders.jayes.util.Graph;
import org.eclipse.recommenders.jayes.util.Graph.Edge;

/**
 * Graph elimination based on greedy minimum fill-in heuristic. Tie-breaking is done by using weights on the nodes. On
 * tie, the node is chosen that will result in the cluster with a minimal sum of node weights
 */
public class MinFillIn implements Iterable<List<Integer>> {

    private Graph graph;
    private double[] nodeWeights;

    public MinFillIn(Graph graph, double[] nodeWeights) {
        this.nodeWeights = nodeWeights;
        this.graph = graph;
    }

    private List<Integer> getNodeList() {
        final List<Integer> moralNodes = new ArrayList<Integer>();
        for (int i = 0; i < graph.getAdjacency().size(); i++) {
            moralNodes.add(i);
        }
        return moralNodes;
    }

    @Override
    public Iterator<List<Integer>> iterator() {
        return new Iterator<List<Integer>>() {

            private List<Integer> nodes = getNodeList();
            private Graph graph = MinFillIn.this.graph.clone();

            @Override
            public boolean hasNext() {
                return !nodes.isEmpty();
            }

            @Override
            public List<Integer> next() {
                int next = nextTriangulationNode();
                nodes.remove(Integer.valueOf(next));
                List<Integer> result = createClique(next);
                virtualRemoveNode(next);
                return result;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();

            }

            private int nextTriangulationNode() {
                int minFillIn = Integer.MAX_VALUE;
                Double nextClusterSize = Double.MAX_VALUE;
                int returnNode = 0;

                for (final int node : nodes) {
                    final Set<Integer> neighbors = getNeighbors(node);
                    final int predictedFillIn = predictFillIn(node, neighbors);
                    if (predictedFillIn <= minFillIn) {
                        final double clusterSize = computeClusterWeight(node, neighbors);
                        if ((predictedFillIn < minFillIn) || (clusterSize < nextClusterSize)) {
                            returnNode = node;
                            minFillIn = predictedFillIn;
                            nextClusterSize = clusterSize;
                        }
                    }
                }

                return returnNode;
            }

            private int predictFillIn(final int node, final Set<Integer> neighborsOfNode) {
                int fillIn = 0;
                for (final Edge e : graph.getIncidentEdges(node)) {
                    final Set<Integer> neighbors2 = getNeighbors(e.getSecond());

                    neighborsOfNode.remove(e.getSecond());
                    neighbors2.retainAll(neighborsOfNode);
                    fillIn += neighborsOfNode.size() - neighbors2.size();
                    // Edges are counted twice, but this is okay, since the
                    // ordering is maintained

                    neighborsOfNode.add(e.getSecond());
                }
                return fillIn;
            }

            private Set<Integer> getNeighbors(final int node) {
                final Set<Integer> neighbors = new HashSet<Integer>();
                for (final Edge e : graph.getIncidentEdges(node)) {
                    neighbors.add(e.getSecond());
                }
                return neighbors;
            }

            private double computeClusterWeight(final int node, final Set<Integer> neighborsOfNode) {
                double clSize = nodeWeights[node];
                for (final int neighbor : neighborsOfNode) {
                    clSize += nodeWeights[neighbor];
                }
                return clSize;
            }

            private List<Integer> createClique(final int centerNode) {
                final List<Integer> clique = new ArrayList<Integer>();
                clique.add(centerNode);
                for (final Edge e : graph.getIncidentEdges(centerNode)) {
                    connectToAll(e.getSecond(), clique);
                    clique.add(e.getSecond());
                }
                return clique;
            }

            private void connectToAll(final Integer node, final List<Integer> others) {
                for (final int other : others) {
                    final Edge newEdge = new Edge(node, other);
                    if (!graph.getIncidentEdges(node).contains(newEdge)) {
                        graph.addEdge(node, other);
                    }
                }
            }

            // isolating node = virtually removing it
            private void virtualRemoveNode(final int node) {
                while (!graph.getIncidentEdges(node).isEmpty()) {
                    graph.removeEdge(graph.getIncidentEdges(node).get(0));
                }
            }

        };
    }

}
