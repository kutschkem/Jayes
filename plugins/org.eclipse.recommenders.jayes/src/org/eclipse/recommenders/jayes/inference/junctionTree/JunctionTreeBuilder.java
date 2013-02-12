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
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.eclipse.recommenders.internal.jayes.util.UnionFindSet;
import org.eclipse.recommenders.jayes.BayesNet;
import org.eclipse.recommenders.jayes.BayesNode;
import org.eclipse.recommenders.jayes.util.Graph;
import org.eclipse.recommenders.jayes.util.Graph.Edge;
import org.eclipse.recommenders.jayes.util.OrderIgnoringPair;
import org.eclipse.recommenders.jayes.util.Pair;
import org.eclipse.recommenders.jayes.util.triangulation.MinFillIn;

public class JunctionTreeBuilder {
    private final BayesNet net;
    private final Graph moral;
    private JunctionTree junctionTree = new JunctionTree(new Graph());

    public static JunctionTree fromNet(final BayesNet net) {
        return new JunctionTreeBuilder(net).getJunctionTree();
    }

    protected JunctionTreeBuilder(final BayesNet net) {
        this.net = net;
        moral = new Graph();
        buildMoralGraph();
        junctionTree.setClusters(triangulateGraphAndFindCliques());
        junctionTree.setSepSets(computeSepsets());
    }

    private Graph buildMoralGraph() {
        moral.initialize(net.getNodes().size());
        final Set<OrderIgnoringPair<BayesNode>> connected = new HashSet<OrderIgnoringPair<BayesNode>>();
        for (final BayesNode node : net.getNodes()) {
            addMoralEdges(connected, node);
        }
        return moral;
    }

    private void addMoralEdges(final Set<OrderIgnoringPair<BayesNode>> connected, final BayesNode node) {
        final ListIterator<BayesNode> it = node.getParents().listIterator();
        while (it.hasNext()) {
            final BayesNode parent = it.next();
            final ListIterator<BayesNode> remainingParentsIt = node.getParents().listIterator(it.nextIndex());
            while (remainingParentsIt.hasNext()) { // connect parents
                final BayesNode otherParent = remainingParentsIt.next();
                connect(connected, parent, otherParent);
            }
            connect(connected, node, parent);
        }
    }

    private void connect(final Set<OrderIgnoringPair<BayesNode>> connected, final BayesNode node1, final BayesNode node2) {
        final OrderIgnoringPair<BayesNode> pair = new OrderIgnoringPair<BayesNode>(node1, node2);
        if (!connected.contains(pair)) {
            connected.add(pair);
            moral.addEdge(node1.getId(), node2.getId());
        }
    }

    private List<List<Integer>> triangulateGraphAndFindCliques() {
        MinFillIn triangulate = new MinFillIn(moral, weightNodesByOutcomes());

        final List<List<Integer>> cliques = new ArrayList<List<Integer>>();
        for (List<Integer> nextClique : triangulate) {
            if (!containsSuperset(cliques, nextClique)) {
                cliques.add(nextClique);
            }
        }
        return cliques;
    }

    private double[] weightNodesByOutcomes() {
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

    private List<Pair<Edge, List<Integer>>> computeSepsets() {
        final List<Pair<Edge, List<Integer>>> candidates = enumerateCandidateSepSets();
        return computeMaxSpanningTree(candidates);

    }

    private List<Pair<Edge, List<Integer>>> enumerateCandidateSepSets() {
        final List<Pair<Edge, List<Integer>>> sepSets = new ArrayList<Pair<Edge, List<Integer>>>();
        final ListIterator<List<Integer>> it = junctionTree.getClusters().listIterator();
        while (it.hasNext()) {
            final List<Integer> clique1 = it.next();
            final ListIterator<List<Integer>> remainingIt = junctionTree.getClusters().listIterator(it.nextIndex());
            while (remainingIt.hasNext()) { // generate sepSets
                final List<Integer> clique2 = new ArrayList<Integer>(remainingIt.next());
                clique2.retainAll(clique1);
                sepSets.add(newPair(new Edge(it.nextIndex() - 1, remainingIt.nextIndex() - 1), clique2));
            }
        }
        return sepSets;
    }

    private List<Pair<Edge, List<Integer>>> computeMaxSpanningTree(
            final List<Pair<Edge, List<Integer>>> candidateSepSets) {
        Collections.sort(candidateSepSets, new SepsetComparator());

        final ArrayDeque<Pair<Edge, List<Integer>>> pq = new ArrayDeque<Pair<Edge, List<Integer>>>(candidateSepSets);

        final int vertexCount = junctionTree.getGraph().getAdjacency().size();
        final UnionFindSet[] sets = UnionFindSet.createArray(vertexCount);

        final List<Pair<Edge, List<Integer>>> leftSepSets = new ArrayList<Pair<Edge, List<Integer>>>();
        while (leftSepSets.size() < (vertexCount - 1)) {
            final Pair<Edge, List<Integer>> sep = pq.poll();
            final boolean bothEndsInSameTree = sets[sep.getFirst().getFirst()].find() == sets[sep.getFirst()
                    .getSecond()].find();
            if (!bothEndsInSameTree) {
                sets[sep.getFirst().getFirst()].merge(sets[sep.getFirst().getSecond()]);
                leftSepSets.add(sep);
                junctionTree.getGraph().addEdge(sep.getFirst().getFirst(), sep.getFirst().getSecond());
            }
        }
        return leftSepSets;
    }

    private final class SepsetComparator implements Comparator<Pair<Edge, List<Integer>>> {

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

    public JunctionTree getJunctionTree() {
        return junctionTree;
    }

}
