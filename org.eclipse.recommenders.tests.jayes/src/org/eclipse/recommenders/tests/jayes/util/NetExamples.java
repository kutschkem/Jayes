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
package org.eclipse.recommenders.tests.jayes.util;

import java.util.Arrays;

import org.eclipse.recommenders.jayes.BayesNet;
import org.eclipse.recommenders.jayes.BayesNode;
import org.eclipse.recommenders.jayes.util.ArrayUtils;

public class NetExamples {

    private NetExamples() {

    }

    /**
     * a simple network with 4 nodes. The structure is as follows: a - b \ / c -
     * d so while this is an easy network, LBP should show some expected trouble
     * with this. JunctionTree will merge a, b and c together to form a Clique
     * 
     * @return
     */
    public static BayesNet testNet1() {
        BayesNet net = new BayesNet();
        BayesNode a = new BayesNode("a");
        a.addOutcome("true");
        a.addOutcome("false");
        a.setProbabilities(new double[] { 0.2, 0.8 });
        net.addNode(a);

        BayesNode b = new BayesNode("b");
        b.addOutcome("la");
        b.addOutcome("le");
        b.addOutcome("lu");
        net.addNode(b);
        b.setParents(Arrays.asList(a));
        b.setProbabilities(ArrayUtils.flatten(new double[][] { { 0.1, 0.4, 0.5 }, { 0.3, 0.4, 0.3 } }));

        BayesNode c = new BayesNode("c");
        c.addOutcome("true");
        c.addOutcome("false");
        net.addNode(c);
        c.setParents(Arrays.asList(a, b));
        c.setProbabilities(ArrayUtils.flatten(new double[][][] { { { 0.1, 0.9 }, { 0.0, 1.0 }, { 0.5, 0.5 } },
                { { 0.2, 0.8 }, { 0.0, 1.0 }, { 0.7, 0.3 } } }));

        BayesNode d = new BayesNode("d");
        d.addOutcome("true");
        d.addOutcome("false");
        net.addNode(d);
        d.setParents(Arrays.asList(c));
        d.setProbabilities(ArrayUtils.flatten(new double[][] { { 0.5, 0.5 }, { 0.2, 0.8 } }));

        return net;
    }

    public static BayesNet unconnectedNet() {
        BayesNet net = new BayesNet();
        net.addNode(new BayesNode("a"));
        net.addNode(new BayesNode("b"));
        net.getNode("a").addOutcome("true");
        net.getNode("a").addOutcome("false");
        net.getNode("b").addOutcome("true");
        net.getNode("b").addOutcome("false");

        net.getNode("a").setProbabilities(new double[] { 0.4, 0.6 });
        net.getNode("b").setProbabilities(new double[] { 0.55, 0.45 });

        return net;
    }

}
