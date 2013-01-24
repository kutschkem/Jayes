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
package org.eclipse.recommenders.tests.jayes;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.CharBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.recommenders.jayes.BayesNet;
import org.eclipse.recommenders.jayes.BayesNode;
import org.eclipse.recommenders.jayes.inference.IBayesInferer;
import org.eclipse.recommenders.jayes.inference.junctionTree.JunctionTreeAlgorithm;
import org.eclipse.recommenders.jayes.io.XMLBIFReader;
import org.eclipse.recommenders.jayes.testgen.TestCase;
import org.eclipse.recommenders.jayes.testgen.TestcaseDeserializer;
import org.eclipse.recommenders.jayes.testgen.scenario.impl.SampledScenarioGenerator;
import org.eclipse.recommenders.tests.jayes.lbp.LoopyBeliefPropagation;
import org.eclipse.recommenders.tests.jayes.util.NetExamples;
import org.junit.Test;

public class JunctionTreeTest {

    private static final double TOLERANCE = 0.01;
    private static final double SMALL_TOLERANCE = 0.00001;

    @Test
    public void testInference1() {
        BayesNet net = NetExamples.testNet1();
        BayesNode a = net.getNode("a");
        BayesNode b = net.getNode("b");

        IBayesInferer inference = new JunctionTreeAlgorithm();
        inference.addEvidence(a, "false");
        inference.addEvidence(b, "lu");
        inference.setNetwork(net);

        IBayesInferer compare = new LoopyBeliefPropagation();
        compare.setNetwork(net);
        compare.addEvidence(a, "false");
        compare.addEvidence(b, "lu");

        for (BayesNode n : net.getNodes())
            assertArrayEquals(compare.getBeliefs(n), inference.getBeliefs(n), 0.01);
    }

    @Test
    public void testLogScale() {
        BayesNet net = NetExamples.testNet1();
        BayesNode a = net.getNode("a");
        BayesNode b = net.getNode("b");

        JunctionTreeAlgorithm inferer = new JunctionTreeAlgorithm();
        inferer.getFactory().setLogThreshold(0);
        inferer.addEvidence(a, "false");
        inferer.addEvidence(b, "lu");
        inferer.setNetwork(net);

        IBayesInferer compare = new LoopyBeliefPropagation();
        compare.setNetwork(net);
        compare.addEvidence(a, "false");
        compare.addEvidence(b, "lu");

        for (BayesNode n : net.getNodes())
            assertArrayEquals(compare.getBeliefs(n), inferer.getBeliefs(n), TOLERANCE);
    }

    @Test
    public void testMixedScale() {
        BayesNet net = NetExamples.testNet1();
        BayesNode a = net.getNode("a");
        BayesNode b = net.getNode("b");

        JunctionTreeAlgorithm inferer = new JunctionTreeAlgorithm();
        // a treshold of two will make the a,b,c clique log scale but the
        // c,d clique normal
        inferer.getFactory().setLogThreshold(2);
        inferer.addEvidence(a, "false");
        inferer.addEvidence(b, "lu");
        inferer.setNetwork(net);

        IBayesInferer compare = new LoopyBeliefPropagation();
        compare.setNetwork(net);
        compare.addEvidence(a, "false");
        compare.addEvidence(b, "lu");

        for (BayesNode n : net.getNodes())
            assertArrayEquals(compare.getBeliefs(n), inferer.getBeliefs(n), 0.01);
    }

    @Test
    public void testFailedCase1() {
        BayesNet net = NetExamples.testNet1();

        BayesNode a = net.getNode("a");
        BayesNode b = net.getNode("b");
        BayesNode c = net.getNode("c");

        JunctionTreeAlgorithm inferer = new JunctionTreeAlgorithm();
        inferer.setNetwork(net);

        Map<BayesNode, String> evidence = new HashMap<BayesNode, String>();
        evidence.put(a, "false");
        evidence.put(c, "true");
        inferer.setEvidence(evidence);
        assertEquals(0.22, inferer.getBeliefs(b)[0], 0.01);
    }

    @Test
    public void testUnconnected() {
        BayesNet net = NetExamples.unconnectedNet();
        BayesNode a = net.getNode("a");
        BayesNode b = net.getNode("b");

        IBayesInferer inference = new JunctionTreeAlgorithm();
        inference.addEvidence(a, "false");
        inference.addEvidence(b, "true");
        inference.setNetwork(net);

        IBayesInferer compare = new LoopyBeliefPropagation();
        compare.setNetwork(net);
        compare.addEvidence(a, "false");
        compare.addEvidence(b, "true");

        for (BayesNode n : net.getNodes())
            assertArrayEquals(inference.getBeliefs(n), compare.getBeliefs(n), 0.01);
    }

    @Test
    public void testSparseFactors() {
        BayesNet net = NetExamples.sparseNet();

        BayesNode a = net.getNode("a");
        BayesNode b = net.getNode("b");

        IBayesInferer inference = new JunctionTreeAlgorithm();
        inference.addEvidence(a, "false");
        inference.addEvidence(b, "lu");
        inference.setNetwork(net);

        IBayesInferer compare = new LoopyBeliefPropagation();
        compare.setNetwork(net);
        compare.addEvidence(a, "false");
        compare.addEvidence(b, "lu");

        for (BayesNode n : net.getNodes())
            assertArrayEquals(compare.getBeliefs(n), inference.getBeliefs(n), 0.01);
    }

    @Test
    public void testLargerScaleCorrectness() throws Exception {
        getClass().getClassLoader();
        BayesNet net = new XMLBIFReader().read(getClass().getClassLoader().getResourceAsStream("JPanel.xml"));
        TestcaseDeserializer deser = new TestcaseDeserializer(net);
        Reader rdr = new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResourceAsStream(
                "testcases_JPanel.json")));
        StringBuffer buf = new StringBuffer();
        CharBuffer cbuff = CharBuffer.allocate(1024);
        while (rdr.read(cbuff) != -1) {
            cbuff.flip();
            buf.append(cbuff);
            cbuff.clear();
        }
        rdr.close();

        List<TestCase> testcases = deser.deserialize(buf.toString());

        JunctionTreeAlgorithm algo = new JunctionTreeAlgorithm();
        algo.setNetwork(net);

        for (TestCase tc : testcases) {
            algo.setEvidence(tc.evidence);
            for (BayesNode node : net.getNodes()) {
                assertArrayEquals(tc.beliefs.get(node), algo.getBeliefs(node), SMALL_TOLERANCE);
            }
        }
    }

    @Test
    public void testLargerScaleCorrectnessAB() throws Exception {
        BayesNet net = new XMLBIFReader().read(getClass().getClassLoader().getResourceAsStream("JPanel.xml"));

        SampledScenarioGenerator testgen = new SampledScenarioGenerator();
        testgen.setNetwork(net);
        testgen.seed(1337);
        testgen.setEvidenceRate(0.5);

        JunctionTreeAlgorithm a = new JunctionTreeAlgorithm();
        a.setNetwork(net);

        JunctionTreeAlgorithm b = new JunctionTreeAlgorithm();
        b.getFactory().setFloatingPointType(float.class);
        b.setNetwork(net);

        for (int i = 0; i < 1000; i++) {
            Map<BayesNode, String> testcase = testgen.testcase();
            b.setEvidence(testcase);
            a.setEvidence(testcase);
            for (BayesNode node : net.getNodes()) {
                assertArrayEquals(a.getBeliefs(node), b.getBeliefs(node), SMALL_TOLERANCE);
            }
        }
    }

}
