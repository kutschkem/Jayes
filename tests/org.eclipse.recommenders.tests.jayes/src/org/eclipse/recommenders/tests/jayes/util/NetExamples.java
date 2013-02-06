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
import org.eclipse.recommenders.jayes.transformation.util.ArrayFlatten;

public class NetExamples {

    private NetExamples() {

    }

    /**
     * a simple network with 4 nodes. The structure is as follows: <br/>
     * 
     * <pre>
     * a - b 
     *  \ / 
     *   c - d
     * 
     * </pre>
     * 
     * so while this is an easy network, LBP should show some expected trouble with this. JunctionTree will merge a, b
     * and c together to form a Clique
     * 
     * @return
     */
    public static BayesNet testNet1() {
        BayesNet net = new BayesNet();
        BayesNode a = net.createNode("a");
        a.addOutcomes("true", "false");
        a.setProbabilities(0.2, 0.8);

        BayesNode b = net.createNode("b");
        b.addOutcomes("la", "le", "lu");
        b.setParents(Arrays.asList(a));
        //@formatter:off
        b.setProbabilities(ArrayFlatten.flatten(new double[][] {
                { 0.1, 0.4, 0.5 }, // a = true
                { 0.3, 0.4, 0.3 }  // a = false
        }));
        //@formatter:on

        BayesNode c = net.createNode("c");
        c.addOutcomes("true", "false");
        c.setParents(Arrays.asList(a, b));
        //@formatter:off
        c.setProbabilities(ArrayUtils.flatten(new double[][][] {
                {					// a = true
                    { 0.1, 0.9 }, 	//   b = la
                    { 0.0, 1.0 }, 	//   b = le
                    { 0.5, 0.5 } 	//   b = lu
        },
                { 					// a = false
                    { 0.2, 0.8 }, 	//   b = la
                    { 0.0, 1.0 },   //   b = le
                    { 0.7, 0.3 }    //   b = lu
        }
        }));
        //@formatter:on

        BayesNode d = net.createNode("d");
        d.addOutcomes("true", "false");
        d.setParents(Arrays.asList(c));
        //@formatter:off
        d.setProbabilities(ArrayFlatten.flatten(new double[][] {
                { 0.5, 0.5 }, // c = true
                { 0.2, 0.8 }  // c = false
        }));
        //@formatter:on

        return net;
    }

    public static BayesNet unconnectedNet() {
        BayesNet net = new BayesNet();
        BayesNode a = net.createNode("a");
        BayesNode b = net.createNode("b");
        a.addOutcomes("true", "false");
        b.addOutcomes("true", "false");

        a.setProbabilities(0.4, 0.6);
        b.setProbabilities(0.55, 0.45);

        return net;
    }

    public static BayesNet sparseNet() {
        BayesNet net = new BayesNet();

        BayesNode a = net.createNode("a");
        a.addOutcome("true");
        a.addOutcome("false");
        a.setProbabilities(0, 1);

        BayesNode b = net.createNode("b");
        b.addOutcomes("la", "le", "lu");
        b.setParents(Arrays.asList(a));
        //@formatter:off
        b.setProbabilities(ArrayFlatten.flatten(new double[][] {
                { 0.1, 0.4, 0.5 }, // a = true
                { 0.3, 0.4, 0.3 }  // a = false
        }));
        //@formatter:on

        BayesNode c = net.createNode("c");
        c.addOutcomes("true", "false", "sth", "sthElse");
        c.setParents(Arrays.asList(a, b));
        //@formatter:off
        c.setProbabilities(ArrayUtils.flatten(new double[][][] {
                { 							// a = true
                    { 0.0, 0.0, 0.1, 0.9 }, //	 b = la
                    { 0.0, 0.0, 0.0, 1.0 }, //	 b = le
                    { 0.0, 0.0, 0.0, 1.0 }  //	 b = lu
        },
                { 							// a = false
                    { 0.0, 0.5, 0.0, 0.5 }, //	 b = la
                    { 0.0, 0.0, 0.0, 1.0 }, //	 b = le
                    { 0.0, 0.7, 0.3, 0.0 }  //	 b = lu
        }
        }));
        //@formatter:on

        BayesNode d = net.createNode("d");
        d.addOutcomes("true", "false");
        d.setParents(Arrays.asList(c));
        //@formatter:off
        d.setProbabilities(ArrayFlatten.flatten(new double[][] {
                { 0.5, 0.5 }, // c = true
                { 0.2, 0.8 }, // c = false
                { 0.5, 0.5 }, // c = sth
                { 0.0, 1.0 }  // c = sthElse
        }));
        //@formatter:on

        return net;
    }

    public static BayesNet treeNet() {
        BayesNet net = new BayesNet();

        BayesNode a = net.createNode("a");
        a.addOutcomes("true", "false");

        BayesNode b = net.createNode("b");
        b.addOutcomes("true", "false");
        b.setParents(Arrays.asList(a));

        BayesNode c = net.createNode("c");
        c.addOutcomes("true", "false");
        c.setParents(Arrays.asList(a));

        BayesNode d = net.createNode("d");
        d.addOutcomes("true", "false");
        d.setParents(Arrays.asList(a));

        a.setProbabilities(0.4, 0.6);
        b.setProbabilities(0.55, 0.45, 0.45, 0.55);
        c.setProbabilities(0.55, 0.45, 0.45, 0.55);
        d.setProbabilities(0.55, 0.45, 0.45, 0.55);

        return net;
    }

}
