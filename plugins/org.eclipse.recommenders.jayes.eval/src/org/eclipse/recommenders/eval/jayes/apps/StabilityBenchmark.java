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
package org.eclipse.recommenders.eval.jayes.apps;

import static org.eclipse.recommenders.eval.jayes.util.CLIUtil.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.recommenders.eval.jayes.util.CLIUtil;
import org.eclipse.recommenders.eval.jayes.util.GuiceUtil;
import org.eclipse.recommenders.jayes.BayesNet;
import org.eclipse.recommenders.jayes.BayesNode;
import org.eclipse.recommenders.jayes.inference.IBayesInferer;
import org.eclipse.recommenders.jayes.inference.junctionTree.JunctionTreeAlgorithm;
import org.eclipse.recommenders.jayes.inference.junctionTree.JunctionTreeBuilder;
import org.eclipse.recommenders.jayes.testgen.scenario.impl.SampledScenarioGenerator;
import org.eclipse.recommenders.jayes.util.NumericalInstabilityException;
import org.eclipse.recommenders.jayes.util.triangulation.MinDegree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.name.Names;

public class StabilityBenchmark implements IApplication {

    private Logger logger = LoggerFactory.getLogger(StabilityBenchmark.class);
    private List<Module> guiceModules = new ArrayList<Module>();
    private Map<String, IBayesInferer> inferrers = new HashMap<String, IBayesInferer>();
    private Map<IBayesInferer, Double> results = new HashMap<IBayesInferer, Double>();

    private BayesNet buildSimpleTreeNetwork(int leaves, double minProb) {
        BayesNet net = new BayesNet();
        BayesNode root = net.createNode("root");
        root.addOutcomes("true", "false");
        root.setProbabilities(1 - minProb, minProb);
        for (int i = 0; i < leaves; i++) {
            BayesNode leaf = net.createNode("leaf" + i);
            leaf.addOutcomes("true", "false");
            leaf.setParents(Arrays.asList(root));
            leaf.setProbabilities(minProb, 1 - minProb, 1 - minProb, minProb);
        }
        return net;
    }

    public void injectEvaluationSubjects() {
        for (Module module : guiceModules) {
            Injector jayes = Guice.createInjector(module);
            inferrers.put(jayes.getInstance(Key.get(String.class, Names.named("specifier"))),
                    jayes.getInstance(IBayesInferer.class));
        }
    }

    @Override
    public Object start(IApplicationContext context) throws Exception {
        String[] args = (String[]) context.getArguments().get(OSGi_CMD_ARGS);
        logger.debug(Arrays.toString(args));
        String config = CLIUtil.findArg(args, CONFIG_PARAM);

        Properties properties = new Properties();
        properties.load(getClass().getClassLoader().getResourceAsStream(config));

        guiceModules = GuiceUtil.loadModules(Splitter.on(',').trimResults().split(properties.getProperty(MODULES)));

        injectEvaluationSubjects();

        evaluate();

        return 0;
    }

    private void evaluate() {
        for (String inferrer : inferrers.keySet()) {
            double minProb = 0.5;
            logger.info(inferrer);
            while (!results.containsKey(inferrers.get(inferrer))) {
                provokeNumericalInstability(inferrers.get(inferrer), minProb, 5000);
                minProb *= minProb;
            }
            logger.warn("hit numerical instability at aprox. " + results.get(inferrers.get(inferrer)).toString()
                    + " nodes");

        }
    }

    private void provokeNumericalInstability(IBayesInferer inferrer, double minProb, int maxLeaves) {
        for (int i = 2; i < maxLeaves; i *= 1.5) {
            BayesNet testNet = buildSimpleTreeNetwork(i, minProb);
            if (inferrer instanceof JunctionTreeAlgorithm) {
                ((JunctionTreeAlgorithm) inferrer).setJunctionTreeBuilder(JunctionTreeBuilder
                        .forHeuristic(new MinDegree()));
            }
            inferrer.setNetwork(testNet);

            BayesNet sampleNetwork = buildSimpleTreeNetwork(i, 1 - minProb);
            SampledScenarioGenerator gen = new SampledScenarioGenerator();
            gen.setEvidenceRate(0.5);
            gen.setNetwork(sampleNetwork);
            logger.info(minProb + " / " + i);
            for (Map<BayesNode, String> evidence : gen.generate(100)) {
                inferrer.setEvidence(translate(evidence, testNet));
                for (BayesNode node : testNet.getNodes()) {
                    try {
                        inferrer.getBeliefs(node);
                    } catch (NumericalInstabilityException e) {
                        results.put(inferrer, (double) i);
                        return;
                    }
                }
            }
        }

    }

    private Map<BayesNode, String> translate(Map<BayesNode, String> evidence, BayesNet testNet) {
        Map<BayesNode, String> translated = new HashMap<BayesNode, String>();
        for (Entry<BayesNode, String> entry : evidence.entrySet()) {
            translated.put(testNet.getNode(entry.getKey().getName()), entry.getValue());
        }
        return translated;
    }

    @Override
    public void stop() {

    }

}
