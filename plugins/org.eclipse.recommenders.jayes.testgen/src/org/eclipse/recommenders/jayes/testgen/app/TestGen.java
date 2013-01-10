/**
 * Copyright (c) 2012 Michael Kutschke.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Michael Kutschke - initial API and implementation.
 */
package org.eclipse.recommenders.jayes.testgen.app;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.recommenders.jayes.BayesNet;
import org.eclipse.recommenders.jayes.BayesNode;
import org.eclipse.recommenders.jayes.inference.IBayesInferer;
import org.eclipse.recommenders.jayes.inference.junctionTree.JunctionTreeAlgorithm;
import org.eclipse.recommenders.jayes.io.XDSLReader;
import org.eclipse.recommenders.jayes.io.XMLBIFReader;
import org.eclipse.recommenders.jayes.testgen.TestCase;
import org.eclipse.recommenders.jayes.testgen.TestcaseSerializer;
import org.eclipse.recommenders.jayes.testgen.scenario.impl.SampledScenarioGenerator;
import org.xml.sax.SAXException;

public class TestGen {

    private static final String OUTPUT_FILE_NAME = "testCases.json";

    /**
     * @param args
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    public static void main(String[] args) throws IOException, ParserConfigurationException, SAXException {
        if (args.length != 1) {
            throw new IllegalArgumentException("need 1 parameter: -conf <file>           Config file");
        }

        Properties properties = new Properties();
        properties.load(new BufferedInputStream(new FileInputStream(args[0])));

        String modelFile = properties.getProperty("testgen.model");
        int numTests = Integer.valueOf(properties.getProperty("testgen.numTests"));
        double rate = Double.valueOf(properties.getProperty("testgen.observationRate"));
        String outputDir = properties.getProperty("outputDir");

        BayesNet net = deserializeNet(modelFile);

        SampledScenarioGenerator testgen = new SampledScenarioGenerator();
        testgen.setBN(net);
        testgen.setEvidenceRate(rate);

        IBayesInferer inference = new JunctionTreeAlgorithm();
        inference.setNetwork(net);

        List<TestCase> testcases = new ArrayList<TestCase>();

        for (int i = 0; i < numTests; i++) {
            Map<BayesNode, String> evidence = testgen.testcase();
            TestCase testCase = new TestCase();
            testCase.evidence = evidence;
            inference.setEvidence(evidence);
            testCase.beliefs = computeBeliefs(inference, net);
            testcases.add(testCase);
        }

        TestcaseSerializer serializer = new TestcaseSerializer(net);
        String str = serializer.writeTestcases(testcases);

        Writer wrt = new BufferedWriter(new FileWriter(new File(outputDir, OUTPUT_FILE_NAME)));
        wrt.append(str);
        wrt.close();
    }

    private static Map<BayesNode, double[]> computeBeliefs(IBayesInferer inference, BayesNet net) {
        Map<BayesNode, double[]> beliefs = new HashMap<BayesNode, double[]>();
        for (BayesNode node : net.getNodes()) {
            beliefs.put(node, inference.getBeliefs(node));
        }
        return beliefs;
    }

    private static BayesNet deserializeNet(String modelFile) throws ParserConfigurationException, SAXException,
            IOException {
        if (modelFile.matches(".*\\.xml")) {
            return new XMLBIFReader().read(modelFile);
        }
        if (modelFile.matches(".*\\.xdsl")) {
            return new XDSLReader().read(modelFile);
        }
        throw new IllegalArgumentException("unrecognized file format");
    }

}
