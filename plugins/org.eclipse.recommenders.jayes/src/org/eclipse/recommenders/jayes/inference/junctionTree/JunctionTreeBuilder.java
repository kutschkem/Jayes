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
package org.eclipse.recommenders.jayes.inference.junctionTree;

import static org.eclipse.recommenders.jayes.util.Pair.newPair;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

import org.eclipse.recommenders.internal.jayes.util.UnionFind;
import org.eclipse.recommenders.jayes.BayesNet;
import org.eclipse.recommenders.jayes.BayesNode;
import org.eclipse.recommenders.jayes.util.Graph;
import org.eclipse.recommenders.jayes.util.Graph.Edge;
import org.eclipse.recommenders.jayes.util.Pair;
import org.eclipse.recommenders.jayes.util.triangulation.GraphElimination;
import org.eclipse.recommenders.jayes.util.triangulation.IEliminationHeuristic;

public class JunctionTreeBuilder {
    private IEliminationHeuristic heuristic;

    public static JunctionTreeBuilder forHeuristic(IEliminationHeuristic heuristic) {
        return new JunctionTreeBuilder(heuristic);
    }

    protected JunctionTreeBuilder(IEliminationHeuristic heuristic) {
        this.heuristic = heuristic;
    }

    public JunctionTree buildJunctionTree(BayesNet net) {
        JunctionTree junctionTree = new JunctionTree(new Graph());
        junctionTree.setClusters(triangulateGraphAndFindCliques(buildMoralGraph(net), weightNodesByOutcomes(net),
                heuristic));
        junctionTree.setSepSets(computeSepsets(junctionTree, net));
        return junctionTree;
    }

    private Graph buildMoralGraph(BayesNet net) {
        Graph moral = new Graph();
        moral.initialize(net.getNodes().size());
        for (final BayesNode node : net.getNodes()) {
            addMoralEdges(moral, node);
        }
        return moral;
    }

    private void addMoralEdges(Graph moral, final BayesNode node) {
        final ListIterator<BayesNode> it = node.getParents().listIterator();
        while (it.hasNext()) {
            final BayesNode parent = it.next();
            final ListIterator<BayesNode> remainingParentsIt = node.getParents().listIterator(it.nextIndex());
            while (remainingParentsIt.hasNext()) { // connect parents
                final BayesNode otherParent = remainingParentsIt.next();
                moral.addEdge(parent.getId(), otherParent.getId());
            }
            moral.addEdge(node.getId(), parent.getId());
        }
    }

    private List<List<Integer>> triangulateGraphAndFindCliques(Graph graph, double[] weights,
            IEliminationHeuristic eliminationHeuristic) {
        GraphElimination triangulate = new GraphElimination(graph, weights, eliminationHeuristic);

        final List<List<Integer>> cliques = new ArrayList<List<Integer>>();
        for (List<Integer> nextClique : triangulate) {
            if (!containsSuperset(cliques, nextClique)) {
                cliques.add(nextClique);
            }
        }
        return cliques;
    }

    private double[] weightNodesByOutcomes(BayesNet net) {
        double[] weights = new double[net.getNodes().size()];
        for (BayesNode node : net.getNodes()) {
            weights[node.getId()] = Math.log(node.getOutcomeCount());
            // using these weights is the same as minimizing the resulting cluster factor size
            // which is given by the product of the variable outcome counts.
        }
        return weights;
    }

    private boolean containsSuperset(final Collection<? extends Collection<Integer>> sets, final Collection<Integer> set) {
        boolean isSubsetOfOther = false;
        for (final Collection<Integer> superset : sets) {
            if (superset.containsAll(set)) {
                isSubsetOfOther = true;
                break;
            }
        }
        return isSubsetOfOther;
    }

    private List<Pair<Edge, List<Integer>>> computeSepsets(JunctionTree junctionTree, BayesNet net) {
        final List<Pair<Edge, List<Integer>>> candidates = enumerateCandidateSepSets(junctionTree.getClusters());
        Collections.sort(candidates, new SepsetComparator(net));
        return computeMaxSpanningTree(junctionTree.getGraph(), candidates);

    }

    private List<Pair<Edge, List<Integer>>> enumerateCandidateSepSets(List<List<Integer>> clusters) {
        final List<Pair<Edge, List<Integer>>> sepSets = new ArrayList<Pair<Edge, List<Integer>>>();
        final ListIterator<List<Integer>> it = clusters.listIterator();
        while (it.hasNext()) {
            final List<Integer> clique1 = it.next();
            final ListIterator<List<Integer>> remainingIt = clusters.listIterator(it.nextIndex());
            while (remainingIt.hasNext()) { // generate sepSets
                final List<Integer> clique2 = new ArrayList<Integer>(remainingIt.next());
                clique2.retainAll(clique1);
                sepSets.add(newPair(new Edge(it.nextIndex() - 1, remainingIt.nextIndex() - 1), clique2));
            }
        }
        return sepSets;
    }

    private List<Pair<Edge, List<Integer>>> computeMaxSpanningTree(Graph graph,
            final List<Pair<Edge, List<Integer>>> sortedCandidateSepSets) {

        final ArrayDeque<Pair<Edge, List<Integer>>> pq = new ArrayDeque<Pair<Edge, List<Integer>>>(
                sortedCandidateSepSets);

        final int vertexCount = graph.getAdjacency().size();
        final UnionFind[] sets = UnionFind.createArray(vertexCount);

        final List<Pair<Edge, List<Integer>>> leftSepSets = new ArrayList<Pair<Edge, List<Integer>>>();
        while (leftSepSets.size() < (vertexCount - 1)) {
            final Pair<Edge, List<Integer>> sep = pq.poll();
            final boolean bothEndsInSameTree = sets[sep.getFirst().getFirst()].find() == sets[sep.getFirst()
                    .getSecond()].find();
            if (!bothEndsInSameTree) {
                sets[sep.getFirst().getFirst()].merge(sets[sep.getFirst().getSecond()]);
                leftSepSets.add(sep);
                graph.addEdge(sep.getFirst().getFirst(), sep.getFirst().getSecond());
            }
        }
        return leftSepSets;
    }

    private final class SepsetComparator implements Comparator<Pair<Edge, List<Integer>>> {

        private final BayesNet net;

        public SepsetComparator(BayesNet net) {
            this.net = net;
        }

        // heuristic: choose sepSet with most variables first,
        // if equal, choose the on with least table size
        @Override
        public int compare(final Pair<Edge, List<Integer>> sepSet1, final Pair<Edge, List<Integer>> sepSet2) {
            final int compareNumberOfVariables = compare(sepSet1.getSecond().size(), sepSet2.getSecond().size());
            if (compareNumberOfVariables != 0) {
                return -compareNumberOfVariables;
            }
            final int tableSize1 = getTableSize(sepSet1.getSecond());
            final int tableSize2 = getTableSize(sepSet2.getSecond());
            return compare(tableSize1, tableSize2);

        }

        private int getTableSize(final List<Integer> cluster) {
            int tableSize = 1;
            for (final int id : cluster) {
                tableSize *= net.getNode(id).getOutcomeCount();
            }
            return tableSize;
        }

        private int compare(final int i1, final int i2) {
            return i1 - i2;
        }
    }

}
