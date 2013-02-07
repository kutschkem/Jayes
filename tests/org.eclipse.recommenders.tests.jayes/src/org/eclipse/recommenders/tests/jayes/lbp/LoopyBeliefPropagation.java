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
package org.eclipse.recommenders.tests.jayes.lbp;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.recommenders.jayes.BayesNet;
import org.eclipse.recommenders.jayes.BayesNode;
import org.eclipse.recommenders.jayes.factor.AbstractFactor;
import org.eclipse.recommenders.jayes.factor.DenseFactor;
import org.eclipse.recommenders.jayes.factor.arraywrapper.DoubleArrayWrapper;
import org.eclipse.recommenders.jayes.inference.AbstractInferer;
import org.eclipse.recommenders.jayes.util.MathUtils;
import org.eclipse.recommenders.jayes.util.Pair;

/**
 * an implementation of Loopy Belief Propagation. Not ready for production use, only serves to check the correctness of
 * the other algorithms on simple networks.
 * 
 * @author Michael Kutschke
 * 
 */
public class LoopyBeliefPropagation extends AbstractInferer {

    /**
     * the Factor/Node Graph is represented as bipartite Graph, with nodes being edge sources and factors being the edge
     * targets
     */
    private final List<List<Edge>> graph = new ArrayList<List<Edge>>();
    private final List<List<Edge>> transponedGraph = new ArrayList<List<Edge>>();
    private final List<Integer> dirty = new LinkedList<Integer>();
    private final Map<Edge, int[]> preparedOps = new HashMap<Edge, int[]>();
    private BayesNet net;

    private static class Edge {
        /**
         * message from node to factor
         */
        double[] messageXF;

        /**
         * message from Factor to Node
         */
        double[] messageFX;
        /**
         * source node
         */
        int source;
        /**
         * target factor
         */
        int target;
        boolean dirty = true;

        @Override
        public String toString() {
            return source + "->" + target;
        }
    }

    @Override
    public void addEvidence(final BayesNode node, final String outcome) {
        super.addEvidence(node, outcome);
        dirty.add(node.getId());

    }

    @Override
    public double[] getBeliefs(final BayesNode node) {
        if (!beliefsValid) {
            beliefsValid = true;
            updateBeliefs();
        }
        double[] p = new double[node.getOutcomeCount()];
        if (evidence.containsKey(node)) {
            p[node.getOutcomeIndex(evidence.get(node))] = 1.0;
        } else {
            for (final Edge e : graph.get(node.getId())) {
                // the belief is the product of all adjacent Factor messages,
                // normalized
                for (int i = 0; i < p.length; i++) {
                    p[i] += e.messageFX[i];
                }
            }
            p = MathUtils.normalizeLog(p);
            MathUtils.exp(p);
        }
        return p;
    }

    @Override
    public void setNetwork(final BayesNet bn) {
        super.setNetwork(bn);
        this.net = bn;
        for (final BayesNode n : bn.getNodes()) {
            transponedGraph.add(new ArrayList<Edge>());
            dirty.add(n.getId());
        }
        for (final BayesNode n : bn.getNodes()) {
            final List<Edge> adjacency = new ArrayList<Edge>();
            graph.add(adjacency);
            // a node is connected to it's own factor
            final Edge own = addEdgeToOwnFactor(n, adjacency);
            prepareOwnFactor(n, own);

            for (final BayesNode c : n.getChildren()) {
                // n is parent of c, therefore it's logical "node"-Node is
                // connected to c's
                // logical "factor"-Node
                final Edge e = new Edge();
                e.source = n.getId();
                e.target = c.getId();
                e.messageFX = new double[n.getOutcomeCount()];
                e.messageXF = new double[n.getOutcomeCount()];
                adjacency.add(e);
                transponedGraph.get(c.getId()).add(e);

                // we always pass via mult/sum from target to source (or from
                // all other sources),
                // so we need to prepare operations with c's factor
                final AbstractFactor f = new DenseFactor();
                f.setDimensions(new int[] { n.getOutcomeCount() });
                f.setDimensionIDs(new int[] { n.getId() });
                final int[] prep = c.getFactor().prepareMultiplication(f);
                preparedOps.put(e, prep);
            }
        }

        resetMessages();

    }

    private void prepareOwnFactor(final BayesNode n, final Edge own) {
        final AbstractFactor fOwn = new DenseFactor();
        fOwn.setDimensions(new int[] { n.getOutcomeCount() });
        fOwn.setDimensionIDs(new int[] { n.getId() });
        final int[] prepOwn = n.getFactor().prepareMultiplication(fOwn);
        preparedOps.put(own, prepOwn);
    }

    private Edge addEdgeToOwnFactor(final BayesNode n, final List<Edge> adjacency) {
        final Edge own = new Edge();
        own.source = n.getId();
        own.target = n.getId();
        own.messageFX = new double[n.getOutcomeCount()];
        own.messageXF = new double[n.getOutcomeCount()];
        adjacency.add(own);
        transponedGraph.get(n.getId()).add(own);
        return own;
    }

    @Override
    public void setEvidence(final Map<BayesNode, String> evidence) {
        beliefsValid = false;
        for (final BayesNode n : net.getNodes()) {
            if (evidence.containsKey(n)) {
                this.evidence.put(n, evidence.get(n));
                dirty.add(n.getId());
            } else if (this.evidence.remove(n) != null) {
                dirty.add(n.getId());
            }
        }

    }

    @Override
    protected void updateBeliefs() {
        resetMessages();
        List<Integer> messagePassingOrder = new ArrayList<Integer>();
        for (int root = 0; root < graph.size(); root++) {
            messagePassingOrder.addAll(postOrder(transponedGraph, root, 1));
            messagePassingOrder.addAll(preOrder(graph, root, 1));
        }
        for (int n : messagePassingOrder) {
            updateFactor(n);
            for (final Edge e : transponedGraph.get(n)) {
                updateNode(e.source);
            }
        }
    }

    private List<Integer> postOrder(List<List<Edge>> graph, int root, int maxDepth) {
        Deque<Pair<Integer, Iterator<Edge>>> deque = new ArrayDeque<Pair<Integer, Iterator<Edge>>>();
        int[] depth = new int[graph.size()];
        List<Integer> result = new ArrayList<Integer>();
        deque.add(new Pair<Integer, Iterator<Edge>>(root, graph.get(root).iterator()));
        depth[root] = 1;
        while (!deque.isEmpty()) {
            Pair<Integer, Iterator<Edge>> pair = deque.peek();
            Iterator<Edge> it = pair.getSecond();
            if (!it.hasNext()) {
                deque.pop();
                result.add(pair.getFirst());
                continue;
            }
            Edge next = it.next();
            if (next.source == next.target || depth[next.source] <= depth[next.target]) {
                continue;
            }
            depth[next.target]++;
            if (depth[next.target] <= maxDepth) {
                deque.push(new Pair<Integer, Iterator<Edge>>(next.target, graph.get(next.target).iterator()));
            }
        }
        return result;
    }

    private List<Integer> preOrder(List<List<Edge>> graph, int root, int maxDepth) {
        Deque<Iterator<Edge>> deque = new ArrayDeque<Iterator<Edge>>();
        int[] depth = new int[graph.size()];
        List<Integer> result = new ArrayList<Integer>();
        result.add(root);
        deque.add(graph.get(root).iterator());
        depth[root] = 1;
        while (!deque.isEmpty()) {
            Iterator<Edge> it = deque.peek();
            if (!it.hasNext()) {
                deque.pop();
                continue;
            }
            Edge next = it.next();
            if (next.source == next.target || depth[next.source] <= depth[next.target]) {
                continue;
            }
            result.add(next.target);
            depth[next.target]++;
            if (depth[next.target] <= maxDepth) {
                deque.push(graph.get(next.target).iterator());
            }
        }
        return result;
    }

    private void resetMessages() {
        for (final List<Edge> ad : graph) {
            for (final Edge e : ad) {
                Arrays.fill(e.messageFX, 0.0);
                Arrays.fill(e.messageXF, 0.0);
            }
        }
    }

    private void updateNode(final int source) {
        for (final Edge e : graph.get(source)) {
            if (!e.dirty) {
                continue;
            }
            double[] result = new double[e.messageFX.length];
            Arrays.fill(result, 0.0);
            // the message to the Factor consists of
            // the product of the messages from all other adjacent factors
            for (final Edge e2 : graph.get(source)) {
                if (e2 != e) {
                    for (int i = 0; i < result.length; i++) {
                        result[i] += e2.messageFX[i];
                    }
                }
            }
            result = MathUtils.normalizeLog(result);

            e.messageXF = result;
        }

    }

    private void updateFactor(final int index) {
        for (final Edge e : transponedGraph.get(index)) {
            e.dirty = false;
            final BayesNode n = net.getNode(e.target);
            final AbstractFactor f = n.getFactor().clone();
            MathUtils.log(f.getValues());
            f.setLogScale(true);
            selectEvidence(f);
            for (final Edge e2 : transponedGraph.get(index)) {
                if (e2 != e) {
                    f.multiplyPrepared(new DoubleArrayWrapper(e2.messageXF), preparedOps.get(e2));//TODO
                }
            }

            double[] result = new double[net.getNode(e.source).getOutcomeCount()];
            f.sumPrepared(new DoubleArrayWrapper(result), preparedOps.get(e));

            result = MathUtils.normalizeLog(result);

            e.messageFX = result;
            e.dirty = true;
        }

    }

    private void selectEvidence(final AbstractFactor f) {
        for (int dim : f.getDimensionIDs()) {
            BayesNode node = net.getNode(dim);
            if (evidence.containsKey(node)) {
                f.select(dim, node.getOutcomeIndex(evidence.get(node)));
            } else {
                f.select(dim, -1);
            }
        }
    }

}
