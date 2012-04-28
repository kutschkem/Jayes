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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.recommenders.jayes.BayesNet;
import org.eclipse.recommenders.jayes.BayesNode;
import org.eclipse.recommenders.jayes.inference.junctionTree.JunctionTreeAlgorithm;
import org.eclipse.recommenders.jayes.io.XMLBIFReader;
import org.junit.Test;
import org.xml.sax.SAXException;

public class XMLBIFTest {

    @Test
    public void test() throws ParserConfigurationException, SAXException, IOException {
        XMLBIFReader rdr = new XMLBIFReader();
        BayesNet net = rdr.read(new File("test/models/dog.xml"));

        JunctionTreeAlgorithm jta = new JunctionTreeAlgorithm();
        jta.setNetwork(net);

        for (BayesNode node : net.getNodes()) {
            System.out.println(node.getName());
            System.out.println(Arrays.toString(jta.getBeliefs(node)));
        }
    }
}
