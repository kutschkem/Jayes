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
package org.eclipse.recommenders.eval.jayes;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.recommenders.eval.jayes.util.RecommenderModelLoader;
import org.eclipse.recommenders.jayes.BayesNet;
import org.eclipse.recommenders.jayes.BayesNode;
import org.eclipse.recommenders.jayes.inference.IBayesInferer;
import org.eclipse.recommenders.jayes.io.XDSLReader;
import org.eclipse.recommenders.jayes.io.XMLBIFReader;
import org.eclipse.recommenders.jayes.util.NumericalInstabilityException;

public class DataPointGenerator {

    private Map<IBayesInferer, String> libMap = new HashMap<IBayesInferer, String>();
    private List<IBayesInferer> inferrers = new ArrayList<IBayesInferer>();
    private Map<IBayesInferer, Long> setupTimes = new HashMap<IBayesInferer, Long>();

    private BayesNet net;

    public void load(String file) throws FileNotFoundException, Exception {
        InputStream str = getClass().getClassLoader().getResourceAsStream(file);
        if (file.endsWith(".data")) {
            setNetwork(RecommenderModelLoader.load(file));
        } else if (file.endsWith(".xdsl")) {
            XDSLReader xdslReader = new XDSLReader();
            xdslReader.setLegacyMode(true);
            setNetwork(xdslReader.read(str));
        } else if (file.endsWith(".xml")) {
            setNetwork(new XMLBIFReader().read(str));
        } else {
            throw new IllegalArgumentException("File name does not correspond to a supported data format");
        }
    }

    public DataPoint generate(Map<BayesNode, String> evidence, int number) {
        return generate(evidence, number, null);
    }

    public DataPoint generate(Map<BayesNode, String> evidence, int number, Map<BayesNode, double[]> goldStandard) {
        long[] updateTimes = new long[inferrers.size()];
        long[] queryTimes = new long[inferrers.size()];
        double[][][] beliefs = new double[inferrers.size()][][];
        for (int i = 0; i < beliefs.length; i++) {
            beliefs[i] = new double[getNetwork().getNodes().size()][];
        }
        Map<String, Double> meanSquaredError = new HashMap<String, Double>();
        for (String lib : libMap.values()) {
            meanSquaredError.put(lib, 0.0);
        }
        int maxErrorNode = 0;
        for (int i = 0; i < number; i++) {

            for (int j = 0; j < inferrers.size(); j++) {
                IBayesInferer infer = inferrers.get(j);
                // measure ---------------
                // evidence setting + belief update + querying all variables
                // once
                long time = System.nanoTime();
                infer.setEvidence(evidence);
                updateTimes[j] += System.nanoTime() - time;
                time = System.nanoTime();
                for (BayesNode n : getNetwork().getNodes()) {
                    try {
                        beliefs[j][n.getId()] = infer.getBeliefs(n);
                    } catch (NumericalInstabilityException exc) {
                        System.err.println("Warning: Numerical Instability (" + libMap.get(infer) + ")");
                    }
                }
                queryTimes[j] += System.nanoTime() - time;
                // ------------------------
            }

            for (BayesNode n : getNetwork().getNodes()) {
                for (int j = 0; j < inferrers.size(); j++) {
                    double hMSE = goldStandard == null ? 0 : computeMSE(goldStandard.get(n), beliefs[j][n.getId()]);
                    if (beliefs[j][n.getId()] == null) {
                        hMSE = 1.0;
                    }
                    if (hMSE > meanSquaredError.get(libMap.get(inferrers.get(j)))) {
                        meanSquaredError.put(libMap.get(inferrers.get(j)), hMSE);
                        maxErrorNode = n.getId();
                    }
                }
            }
        }
        DataPoint p = new DataPoint(toIntegerMap(evidence));
        for (int i = 0; i < inferrers.size(); i++) {
            String lib = libMap.get(inferrers.get(i));
            p.setMeanSquaredError(lib, meanSquaredError.get(lib));
            p.setUpdateTime(lib, updateTimes[i] / number);
            p.setQueryTime(lib, queryTimes[i] / number);
            p.setSetupTime(lib, setupTimes.get(inferrers.get(i)));
        }
        p.setMaxErrorNode(maxErrorNode);

        return p;
    }

    private Map<Integer, Integer> toIntegerMap(Map<BayesNode, String> evidence) {
        Map<Integer, Integer> result = new HashMap<Integer, Integer>();
        for (Entry<BayesNode, String> e : evidence.entrySet()) {
            result.put(e.getKey().getId(), e.getKey().getOutcomeIndex(e.getValue()));
        }
        return result;
    }

    protected double computeMSE(double[] beliefs, double[] nodeValue) {
        if (nodeValue == null) {
            return 1.0;
        }

        double mse = 0;
        for (int i = 0; i < beliefs.length; i++)
            mse = Math.max(mse, Math.pow(nodeValue[i] - beliefs[i], 2));
        return mse;
    }

    public void addInferrer(String lib, IBayesInferer inferrer) {
        inferrers.add(inferrer);
        libMap.put(inferrer, lib);
    }

    public void setNetwork(BayesNet net) {
        this.net = net;
        for (IBayesInferer inferrer : inferrers) {
            long time = System.nanoTime();
            inferrer.setNetwork(net);
            setupTimes.put(inferrer, System.nanoTime() - time);
        }
    }

    public BayesNet getNetwork() {
        return net;
    }

    public Collection<String> getLibs() {
        return libMap.values();
    }

}
