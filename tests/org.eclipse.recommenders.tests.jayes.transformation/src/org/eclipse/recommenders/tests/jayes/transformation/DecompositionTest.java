package org.eclipse.recommenders.tests.jayes.transformation;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.eclipse.recommenders.jayes.BayesNet;
import org.eclipse.recommenders.jayes.BayesNode;
import org.eclipse.recommenders.jayes.inference.junctionTree.JunctionTreeAlgorithm;
import org.eclipse.recommenders.jayes.transformation.IDecompositionStrategy;
import org.eclipse.recommenders.jayes.transformation.LatentDeterministicDecomposition;
import org.eclipse.recommenders.jayes.transformation.SmoothedFactorDecomposition;
import org.eclipse.recommenders.jayes.transformation.util.DecompositionFailedException;
import org.eclipse.recommenders.jayes.util.ArrayUtils;
import org.junit.Test;

public class DecompositionTest {

    private static final double TOLERANCE = 0.0001;

    //Tests the "normal" case where no reordering has to take place to make the decomposition useful
    @Test
    public void testLDDNoReordering() throws DecompositionFailedException {
        BayesNet net = new BayesNet();
        BayesNode a = new BayesNode("A");
        net.addNode(a);
        a.addOutcomes("true", "false");
        a.setProbabilities(new double[] { 0.8, 0.2 });
        BayesNode b = new BayesNode("B");
        b.setParents(Arrays.asList(a));
        b.addOutcomes("a", "b");
        b.setProbabilities(ArrayUtils.flatten(new double[][]
        {
                { 0.25, 0.75 },
                { 0.75, 0.25 }
        }));
        net.addNode(b);

        JunctionTreeAlgorithm algo = new JunctionTreeAlgorithm();
        algo.setNetwork(net);

        algo.addEvidence(b, "b");
        double[] beliefA = algo.getBeliefs(a);
        double[] beliefB = algo.getBeliefs(b);

        LatentDeterministicDecomposition ldd = new LatentDeterministicDecomposition();
        ldd.decompose(net, b);

        algo = new JunctionTreeAlgorithm();
        algo.setNetwork(net);

        algo.addEvidence(b, "b");
        assertArrayEquals(beliefA, algo.getBeliefs(net.getNode("A")), TOLERANCE);
        assertArrayEquals(beliefB, algo.getBeliefs(net.getNode("B")), TOLERANCE);

    }

    @Test
    public void testLDD2() throws DecompositionFailedException {
        exerciseAndVerifyReordering(new LatentDeterministicDecomposition());
    }

    //Test the case where the factor cannot be expressed perfectly as
    // combination of base vectors
    @Test
    public void testLDD3() throws DecompositionFailedException {
        BayesNet net = new BayesNet();
        BayesNode a = new BayesNode("A");
        net.addNode(a);
        a.addOutcomes("true", "false");
        a.setProbabilities(new double[] { 0.8, 0.2 });
        BayesNode b = new BayesNode("B");
        b.setParents(Arrays.asList(a));
        b.addOutcomes("a", "b", "c", "d", "e");
        b.setProbabilities(ArrayUtils.flatten(new double[][]
        {
                { 0.1, 0.05, 0.05, 0.4, 0.4 },
                { 0.1, 0.4, 0.4, 0.05, 0.05 }
        }));
        net.addNode(b);

        JunctionTreeAlgorithm algo = new JunctionTreeAlgorithm();
        algo.setNetwork(net);

        algo.addEvidence(b, "a");
        double[] beliefA = algo.getBeliefs(a);
        double[] beliefB = algo.getBeliefs(b);

        LatentDeterministicDecomposition ldd = new LatentDeterministicDecomposition();
        ldd.decompose(net, b);

        algo = new JunctionTreeAlgorithm();
        algo.setNetwork(net);

        algo.addEvidence(b, "a");
        assertArrayEquals(beliefA, algo.getBeliefs(net.getNode("A")), TOLERANCE);
        assertArrayEquals(beliefB, algo.getBeliefs(net.getNode("B")), TOLERANCE);

    }

    // tests a case where LDD will explicitly fail on node B
    @Test
    public void testLDD4() {
        BayesNet net = new BayesNet();
        BayesNode a = new BayesNode("A");
        net.addNode(a);
        a.addOutcomes("true", "false");
        a.setProbabilities(new double[] { 0.8, 0.2 });
        BayesNode b = new BayesNode("B");
        b.setParents(Arrays.asList(a));
        b.addOutcomes("a", "b", "c", "e", "f");
        b.setProbabilities(ArrayUtils.flatten(new double[][]
        {
                { 0.25, 0.75, 0.25, 0.75, 0 },
                { 0.75, 0.25, 0.75, 0.25, 1 }
        }));
        net.addNode(b);

        JunctionTreeAlgorithm algo = new JunctionTreeAlgorithm();
        algo.setNetwork(net);

        algo.addEvidence(b, "b");
        double[] beliefA = algo.getBeliefs(a);
        double[] beliefB = algo.getBeliefs(b);

        LatentDeterministicDecomposition ldd = new LatentDeterministicDecomposition();

        try {
            ldd.decompose(net, b);
            fail("Decomposition should have failed, but didn't");
        } catch (DecompositionFailedException e) {

        }

        //just sanity check that nothing got screwed up

        algo = new JunctionTreeAlgorithm();
        algo.setNetwork(net);

        algo.addEvidence(b, "b");
        assertArrayEquals(beliefA, algo.getBeliefs(net.getNode("A")), TOLERANCE);
        assertArrayEquals(beliefB, algo.getBeliefs(net.getNode("B")), TOLERANCE);

    }

    @Test
    public void testSFD() throws DecompositionFailedException {
        exerciseAndVerifyReordering(new SmoothedFactorDecomposition());
    }

    //Tests the case where the lowest dimension is the biggest, and the factor needs to be reordered
    private void exerciseAndVerifyReordering(IDecompositionStrategy decomposition) throws DecompositionFailedException {
        BayesNet net = new BayesNet();
        BayesNode a = new BayesNode("A");
        net.addNode(a);
        a.addOutcomes("true", "false");
        a.setProbabilities(new double[] { 0.8, 0.2 });
        BayesNode b = new BayesNode("B");
        b.setParents(Arrays.asList(a));
        b.addOutcomes("a", "b", "c", "d");
        b.setProbabilities(ArrayUtils.flatten(new double[][]
        {
                { 0.1, 0.3, 0.3, 0.1 },
                { 0.1, 0.3, 0.3, 0.1 }
        }));
        net.addNode(b);

        JunctionTreeAlgorithm algo = new JunctionTreeAlgorithm();
        algo.setNetwork(net);

        algo.addEvidence(b, "b");
        double[] beliefA = algo.getBeliefs(a);
        double[] beliefB = algo.getBeliefs(b);

        decomposition.decompose(net, b);

        algo = new JunctionTreeAlgorithm();
        algo.setNetwork(net);

        algo.addEvidence(b, "b");
        assertArrayEquals(beliefA, algo.getBeliefs(net.getNode("A")), TOLERANCE);
        assertArrayEquals(beliefB, algo.getBeliefs(net.getNode("B")), TOLERANCE);
    }

}
