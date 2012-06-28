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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.recommenders.jayes.BayesNet;
import org.eclipse.recommenders.jayes.BayesNode;
import org.eclipse.recommenders.jayes.Factor;
import org.eclipse.recommenders.jayes.SparseFactor;
import org.eclipse.recommenders.jayes.inference.AbstractInferer;
import org.eclipse.recommenders.jayes.util.ArrayUtils;
import org.eclipse.recommenders.jayes.util.BayesUtils;
import org.eclipse.recommenders.jayes.util.DoubleArrayFlyWeight;
import org.eclipse.recommenders.jayes.util.Graph;
import org.eclipse.recommenders.jayes.util.Graph.Edge;
import org.eclipse.recommenders.jayes.util.IntArrayFlyWeight;
import org.eclipse.recommenders.jayes.util.MathUtils;
import org.eclipse.recommenders.jayes.util.NumericalInstabilityException;
import org.eclipse.recommenders.jayes.util.Pair;

public class JunctionTreeAlgorithm extends AbstractInferer {

    protected final Map<Edge, Factor> sepSets = new HashMap<Edge, Factor>();
    protected Graph junctionTree;
    protected Factor[] nodePotentials;
    protected int[] homeClusters;
    // need IdentityHashmap here because an Edge and
    // it's backward Edge are considered equal
    // (which is also needed for simplicity)
    protected final IdentityHashMap<Edge, int[]> preparedMultiplications = new IdentityHashMap<Edge, int[]>();
    protected List<Integer>[] concernedClusters;
    protected Factor[] queryFactors;
    protected int[][] preparedQueries;
    protected boolean[] isBeliefValid;
    protected final List<Pair<Factor, double[]>> initializations = new ArrayList<Pair<Factor, double[]>>();

    protected final List<int[]> queryFactorReverseMapping = new ArrayList<int[]>();

    // used for computing evidence collection skip
    protected final Set<Integer> clustersHavingEvidence = new HashSet<Integer>();

    private int logThreshold = Integer.MAX_VALUE;
    protected double[] scratchpad;

    /**
     * cliques bigger (in the number of variables) than the threshold are
     * computed on the log-scale. Validate different values if you encounter
     * numerical instabilities.
     * 
     * @param logThreshold
     */
    public void setLogThreshold(final int logThreshold) {
        this.logThreshold = logThreshold;
    }

    public int getLogThreshold() {
        return logThreshold;
    }

    @Override
    public double[] getBeliefs(final BayesNode node) {
        if (!beliefsValid) {
            beliefsValid = true;
            updateBeliefs();
        }
        final int nodeId = node.getId();
        if (!isBeliefValid[nodeId]) {
            isBeliefValid[nodeId] = true;
            if (!evidence.containsKey(node)) {
                validateBelief(nodeId);
            } else {
                Arrays.fill(beliefs[nodeId], 0);
                beliefs[nodeId][node.getOutcomeIndex(evidence.get(node))] = 1;
            }
        }
        return super.getBeliefs(node);
    }

    private void validateBelief(final int nodeId) {
        final Factor f = queryFactors[nodeId];
        f.sumPrepared(beliefs[nodeId], preparedQueries[nodeId]);
        if (f.isLogScale()) {
            MathUtils.exp(beliefs[nodeId]);
        }
        try {
            beliefs[nodeId] = MathUtils.normalize(beliefs[nodeId]);
        } catch (final IllegalArgumentException exception) {
            throw new NumericalInstabilityException("Numerical instability detected for evidence: " + evidence
                    + " and node : " + nodeId + ", consider setting the LogTreshold lower in the OptimizationHints",
                    exception);
        }
    }

    @Override
    protected void updateBeliefs() {
        Arrays.fill(isBeliefValid, false);
        doUpdateBeliefs();
    }

    private void doUpdateBeliefs() {

        incorporateAllEvidence();
        int propagationRoot = findPropagationRoot();

        replayFactorInitializations();
        collectEvidence(propagationRoot, skipCollection(propagationRoot));
        distributeEvidence(propagationRoot, skipDistribution(propagationRoot));
    }

    private void replayFactorInitializations() {
        for (final Pair<Factor, double[]> init : initializations) {
            init.getFirst().copyValues(init.getSecond());
        }
    }

    private void incorporateAllEvidence() {
        for (Pair<Factor, double[]> init : initializations) {
            init.getFirst().resetSelections();
        }

        clustersHavingEvidence.clear();
        for (BayesNode n : evidence.keySet()) {
            incorporateEvidence(n);
        }
    }

    private void incorporateEvidence(final BayesNode node) {
        int n = node.getId();
        // get evidence to all concerned factors (includes home cluster)
        for (final Integer concernedCluster : concernedClusters[n]) {
            nodePotentials[concernedCluster].select(n, node.getOutcomeIndex(evidence.get(node)));
            clustersHavingEvidence.add(concernedCluster);
        }
    }

    private int findPropagationRoot() {
        int propagationRoot = 0;
        for (BayesNode n : evidence.keySet()) {
            propagationRoot = homeClusters[n.getId()];
        }
        return propagationRoot;
    }

    /**
     * checks which nodes need not be processed during collectEvidence (because
     * of preprocessing). These are those nodes without evidence which are
     * leaves or which only have non-evidence descendants
     * 
     * @param root
     *            the node to start the check from
     * @return a set of the nodes not needing a call of collectEvidence
     */
    private Set<Integer> skipCollection(final int root) {
        final Set<Integer> skipped = new HashSet<Integer>(nodePotentials.length);
        recursiveSkipCollection(root, new HashSet<Integer>(nodePotentials.length), skipped);
        return skipped;
    }

    private void recursiveSkipCollection(final int node, final Set<Integer> visited, final Set<Integer> skipped) {
        visited.add(node);
        boolean areAllDescendantsSkipped = true;
        for (final Edge e : junctionTree.getIncidentEdges(node)) {
            if (!visited.contains(e.getSecond())) {
                recursiveSkipCollection(e.getSecond(), visited, skipped);
                if (!skipped.contains(e.getSecond())) {
                    areAllDescendantsSkipped = false;
                }
            }
        }
        if (areAllDescendantsSkipped && !clustersHavingEvidence.contains(node)) {
            skipped.add(node);
        }

    }

    /**
     * checks which nodes do not need to be visited during evidence
     * distribution. These are exactly those nodes which are
     * <ul>
     * <li>not the query factor of a non-evidence variable</li>
     * <li>AND have no descendants that cannot be skipped</li>
     * </ul>
     * 
     * @param distNode
     * @return
     */
    private Set<Integer> skipDistribution(final int distNode) {
        final Set<Integer> skipped = new HashSet<Integer>(nodePotentials.length);
        recursiveSkipDistribution(distNode, new HashSet<Integer>(nodePotentials.length), skipped);
        return skipped;
    }

    private void recursiveSkipDistribution(final int node, final Set<Integer> visited, final Set<Integer> skipped) {
        visited.add(node);
        boolean areAllDescendantsSkipped = true;
        for (final Edge e : junctionTree.getIncidentEdges(node)) {
            if (!visited.contains(e.getSecond())) {
                recursiveSkipDistribution(e.getSecond(), visited, skipped);
                if (!skipped.contains(e.getSecond())) {
                    areAllDescendantsSkipped = false;
                }
            }
        }
        if (areAllDescendantsSkipped && !isQueryFactorOfUnobservedVariable(node)) {
            skipped.add(node);
        }
    }

    private boolean isQueryFactorOfUnobservedVariable(final int node) {
        for (int i : queryFactorReverseMapping.get(node)) {
            if (!evidence.containsKey(net.getNode(i))) {
                return true;
            }
        }
        return false;
    }

    private void collectEvidence(final int cluster, final Set<Integer> marked) {
        marked.add(cluster);
        for (final Edge e : junctionTree.getIncidentEdges(cluster)) {
            if (!marked.contains(e.getSecond())) {
                collectEvidence(e.getSecond(), marked);
                messagePass(e.getBackEdge());
            }
        }
    }

    private void distributeEvidence(final int cluster, final Set<Integer> marked) {
        marked.add(cluster);
        for (final Edge e : junctionTree.getIncidentEdges(cluster)) {
            if (!marked.contains(e.getSecond())) {
                messagePass(e);
                distributeEvidence(e.getSecond(), marked);
            }
        }
    }

    private void messagePass(final Edge sepSetEdge) {

        final Factor sepSet = sepSets.get(sepSetEdge);
        if (!needMessagePass(sepSet)) {
            return;
        }

        final double[] newSepValues = sepSet.getValues();
        System.arraycopy(newSepValues, 0, scratchpad, 0, newSepValues.length);

        final int[] preparedOp = preparedMultiplications.get(sepSetEdge.getBackEdge());
        nodePotentials[sepSetEdge.getFirst()].sumPrepared(newSepValues, preparedOp);

        if (isOnlyFirstLogScale(sepSetEdge)) {
            MathUtils.exp(newSepValues);
        }
        if (areBothEndsLogScale(sepSetEdge)) {
            MathUtils.secureSubtract(newSepValues, scratchpad, scratchpad);
        } else {
            MathUtils.secureDivide(newSepValues, scratchpad, scratchpad);
        }

        if (isOnlySecondLogScale(sepSetEdge)) {
            MathUtils.log(scratchpad);
        }

        nodePotentials[sepSetEdge.getSecond()].multiplyPrepared(scratchpad, preparedMultiplications.get(sepSetEdge));

    }

    /*
     * we don't get additional information if all variables in the sepSet are
     * observed, so skip message pass
     */
    private boolean needMessagePass(final Factor sepSet) {
        for (final int var : sepSet.getDimensionIDs()) {
            if (!evidence.containsKey(net.getNode(var))) {
                return true;
            }
        }
        return false;
    }

    private boolean isOnlyFirstLogScale(final Edge edge) {
        return nodePotentials[edge.getFirst()].isLogScale() && !nodePotentials[edge.getSecond()].isLogScale();
    }

    private boolean isOnlySecondLogScale(final Edge edge) {
        return !nodePotentials[edge.getFirst()].isLogScale() && nodePotentials[edge.getSecond()].isLogScale();
    }

    @Override
    public void setNetwork(final BayesNet bn) {
        super.setNetwork(bn);
        initializeFields();
        final List<List<Integer>> clusters = buildJunctionTree().getClusters();
        setHomeClusters(clusters);
        setQueryFactors();
        sparsifyPotentials();
        initializePotentialValues();
        multiplyCPTsIntoPotentials();
        prepareMultiplications();
        prepareScratch();
        invokeInitialBeliefUpdate();
        storePotentialValues();

        logSparsenessInfo();

    }

    private void logSparsenessInfo() {
        int denseLength = 0;
        int sparseLength = 0;
        int nonSparseFactorLength = 0;
        for (Factor f : nodePotentials) {
            if (f instanceof SparseFactor) {
                SparseFactor _f = (SparseFactor) f;
                if (_f.isSparse()) {
                    denseLength += _f.computeLength();
                    sparseLength += _f.getValues().length;
                    sparseLength += _f.computeLength() / _f.getSparseness();
                } else {
                    nonSparseFactorLength += _f.getValues().length;
                }
            } else {
                nonSparseFactorLength += f.getValues().length;
            }
        }
        Logger log = Logger.getLogger("org.eclipse.recommenders.jayes");

        sparseLength += nonSparseFactorLength;
        denseLength += nonSparseFactorLength;

        log.log(Level.INFO, "dense factor size: " + denseLength);
        log.log(Level.INFO, "sparse factor size: " + sparseLength);
        log.log(Level.INFO, "ratio: " + (sparseLength / (double) denseLength));
    }

    private void sparsifyPotentials() {
        Map<SparseFactor, List<Factor>> potentialMap = new HashMap<SparseFactor, List<Factor>>();
        for (final BayesNode node : net.getNodes()) {
            final Factor nodeHome = nodePotentials[homeClusters[node.getId()]];
            if (nodeHome instanceof SparseFactor) {
                SparseFactor f = ((SparseFactor) nodeHome);
                if (!potentialMap.containsKey(f)) {
                    potentialMap.put(f, new ArrayList<Factor>());
                }
                potentialMap.get(f).add(node.getFactor());
            }
        }
        for (Factor f : nodePotentials) {
            if (potentialMap.containsKey(f)) {
                ((SparseFactor) f).sparsify(potentialMap.get(f));
            }
        }

    }

    private void prepareScratch() {
        int maxSize = 0;
        for (Factor sepSet : sepSets.values()) {
            maxSize = Math.max(maxSize, sepSet.getValues().length);
        }
        scratchpad = new double[maxSize];

    }

    @SuppressWarnings("unchecked")
    private void initializeFields() {
        isBeliefValid = new boolean[beliefs.length];
        Arrays.fill(isBeliefValid, false);
        final int numNodes = net.getNodes().size();
        homeClusters = new int[numNodes];
        queryFactors = new Factor[numNodes];
        preparedQueries = new int[numNodes][];
        concernedClusters = new List[numNodes];
        for (int i = 0; i < concernedClusters.length; i++) {
            concernedClusters[i] = new ArrayList<Integer>();
        }
    }

    private JunctionTree buildJunctionTree() {
        final JunctionTree jtree = JunctionTreeBuilder.fromNet(net);
        this.junctionTree = jtree.getGraph();

        initializeClusterFactors(jtree.getClusters());
        initializeSepsetFactors(jtree.getSepSets());

        return jtree;
    }

    private void initializeClusterFactors(final List<List<Integer>> clusters) {
        nodePotentials = new Factor[clusters.size()];
        for (final ListIterator<List<Integer>> cliqueIt = clusters.listIterator(); cliqueIt.hasNext();) {
            final List<Integer> cluster = cliqueIt.next();
            final Factor cliqueFactor = createFactor(cluster);
            nodePotentials[cliqueIt.nextIndex() - 1] = cliqueFactor;
            for (final Integer var : cluster) {
                concernedClusters[var].add(cliqueIt.nextIndex() - 1);
            }
        }
    }

    private void initializeSepsetFactors(final List<Pair<Edge, List<Integer>>> sepSets) {
        for (final Pair<Edge, List<Integer>> sep : sepSets) {
            this.sepSets.put(sep.getFirst(), createFactor(sep.getSecond()));
        }
    }

    protected Factor createFactor(final List<Integer> vars) {
        final Factor f = new SparseFactor();
        final List<Integer> dimensions = new ArrayList<Integer>();
        for (final Integer dim : vars) {
            dimensions.add(net.getNode(dim).getOutcomeCount());
        }
        f.setDimensions((int[]) ArrayUtils.toPrimitiveArray(dimensions.toArray(new Integer[0])));
        f.setDimensionIDs((int[]) ArrayUtils.toPrimitiveArray(vars.toArray(new Integer[0])));
        if (vars.size() > getLogThreshold()) {
            f.setLogScale(true);
        }
        return f;
    }

    private void setHomeClusters(final List<List<Integer>> clusters) {
        for (final BayesNode node : net.getNodes()) {
            final List<Integer> nodeAndParents = BayesUtils.getNodeAndParentIds(node);
            for (final ListIterator<List<Integer>> clusterIt = clusters.listIterator(); clusterIt.hasNext();) {
                if (clusterIt.next().containsAll(nodeAndParents)) {
                    homeClusters[node.getId()] = clusterIt.nextIndex() - 1;
                    break;
                }
            }
        }
    }

    private void setQueryFactors() {
        for (final BayesNode n : net.getNodes()) {
            for (final Integer f : concernedClusters[n.getId()]) {
                final boolean isFirstOrSmallerTable = queryFactors[n.getId()] == null
                        || queryFactors[n.getId()].getValues().length > nodePotentials[f].getValues().length;
                if (isFirstOrSmallerTable) {
                    queryFactors[n.getId()] = nodePotentials[f];
                }
            }
        }

        for (int i = 0; i < nodePotentials.length; i++) {
            List<Integer> queryVars = new ArrayList<Integer>();
            for (int var : nodePotentials[i].getDimensionIDs()) {
                if (queryFactors[var] == nodePotentials[i]) {
                    queryVars.add(var);
                }
            }
            queryFactorReverseMapping.add((int[]) ArrayUtils.toPrimitiveArray(queryVars.toArray(new Integer[0])));
        }
    }

    private void prepareMultiplications() {
        // compress by combining equal prepared statements, thus saving memory
        final IntArrayFlyWeight flyWeight = new IntArrayFlyWeight();
        prepareSepsetMultiplications(flyWeight);
        prepareQueries(flyWeight);

        logMemorySavingsFromFlyweightPattern(flyWeight);
    }

    private void prepareSepsetMultiplications(final IntArrayFlyWeight flyWeight) {
        for (int node = 0; node < nodePotentials.length; node++) {
            for (final Edge e : junctionTree.getIncidentEdges(node)) {
                final int[] preparedMultiplication = nodePotentials[e.getSecond()]
                        .prepareMultiplication(sepSets.get(e));
                preparedMultiplications.put(e, flyWeight.getInstance(preparedMultiplication));
            }
        }
    }

    private void prepareQueries(final IntArrayFlyWeight flyWeight) {
        for (final BayesNode node : net.getNodes()) {
            final Factor beliefFactor = new Factor();
            beliefFactor.setDimensions(new int[] { node.getOutcomeCount() });
            beliefFactor.setDimensionIDs(new int[] { node.getId() });
            final int[] preparedQuery = queryFactors[node.getId()].prepareMultiplication(beliefFactor);
            preparedQueries[node.getId()] = flyWeight.getInstance(preparedQuery);
        }
    }

    private void invokeInitialBeliefUpdate() {

        collectEvidence(0, new HashSet<Integer>());
        distributeEvidence(0, new HashSet<Integer>());
    }

    private void initializePotentialValues() {
        final double ONE_LOG = 0.0;
        final double ONE = 1.0;

        for (final Factor f : nodePotentials) {
            f.fill(f.isLogScale() ? ONE_LOG : ONE);
        }

        for (final Entry<Edge, Factor> sepSet : sepSets.entrySet()) {
            if (!areBothEndsLogScale(sepSet.getKey())) {
                // if one part is log-scale, we transform to non-log-scale
                sepSet.getValue().fill(ONE);
            } else {
                sepSet.getValue().fill(ONE_LOG);
            }
        }
    }

    private void multiplyCPTsIntoPotentials() {
        for (final BayesNode node : net.getNodes()) {
            final Factor nodeHome = nodePotentials[homeClusters[node.getId()]];
            if (nodeHome.isLogScale()) {
                nodeHome.multiplyCompatibleToLog(node.getFactor());
            } else {
                nodeHome.multiplyCompatible(node.getFactor());
            }
        }
    }

    private boolean areBothEndsLogScale(final Edge edge) {
        return nodePotentials[edge.getFirst()].isLogScale() && nodePotentials[edge.getSecond()].isLogScale();
    }

    private void storePotentialValues() {
        DoubleArrayFlyWeight flyweight = new DoubleArrayFlyWeight();
        for (final Factor pot : nodePotentials) {
            initializations.add(new Pair<Factor, double[]>(pot, flyweight.getInstance(pot.getValues().clone())));
        }

        for (final Factor sep : sepSets.values()) {
            initializations.add(new Pair<Factor, double[]>(sep, flyweight.getInstance(sep.getValues().clone())));
        }

        logMemorySavingsFromFlyweightPattern(flyweight);

    }

    private void logMemorySavingsFromFlyweightPattern(DoubleArrayFlyWeight flyweight) {
        int factorSizes = 0;
        for (final Factor pot : nodePotentials) {
            factorSizes += pot.getValues().length;
        }

        for (final Factor sep : sepSets.values()) {
            factorSizes += sep.getValues().length;
        }

        int flyweightsize = 0;
        for (double[] d : flyweight) {
            flyweightsize += d.length;
        }

        Logger log = Logger.getLogger("org.eclipse.recommenders.jayes");
        log.log(Level.INFO, "initializations, orgininal size: " + factorSizes);
        log.log(Level.INFO, "initializations, flyweight size: " + flyweightsize);
        log.log(Level.INFO, "ratio: " + (flyweightsize / (double) factorSizes));
    }

    private void logMemorySavingsFromFlyweightPattern(IntArrayFlyWeight flyweight) {
        int factorSizes = 0;
        for (int[] intArr : preparedMultiplications.values()) {
            factorSizes += intArr.length;
        }

        for (int[] prep : preparedQueries) {
            factorSizes += prep.length;
        }

        int flyweightsize = 0;
        for (int[] d : flyweight) {
            flyweightsize += d.length;
        }

        Logger log = Logger.getLogger("org.eclipse.recommenders.jayes");
        log.log(Level.INFO, "prepared ops, orgininal size: " + factorSizes);
        log.log(Level.INFO, "prepared ops, flyweight size: " + flyweightsize);
        log.log(Level.INFO, "ratio: " + (flyweightsize / (double) factorSizes));
    }

}
