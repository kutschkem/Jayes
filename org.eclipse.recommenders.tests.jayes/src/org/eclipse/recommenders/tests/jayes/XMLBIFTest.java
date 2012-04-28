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
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.recommenders.jayes.BayesNet;
import org.eclipse.recommenders.jayes.BayesNode;
import org.eclipse.recommenders.jayes.inference.junctionTree.JunctionTreeAlgorithm;
import org.eclipse.recommenders.jayes.io.XMLBIFReader;
import org.eclipse.recommenders.jayes.io.XMLBIFWriter;
import org.junit.Test;
import org.xml.sax.SAXException;

public class XMLBIFTest {

    @Test
    public void readerTest() throws ParserConfigurationException, SAXException, IOException {
        // tests whether parsing functions
        XMLBIFReader rdr = new XMLBIFReader();
        BayesNet net = rdr.read(new File("test/models/dog.xml"));
        assertTrue(net != null);
        assertEquals(5, net.getNodes().size());
    }

    @Test
    public void roundtripTest() throws ParserConfigurationException, SAXException, IOException {
        // tests whether XMLBIF reader and writer are consistent
        XMLBIFReader rdr = new XMLBIFReader();

        BayesNet net = rdr.read(new File("test/models/dog.xml"));

        JunctionTreeAlgorithm jta1 = new JunctionTreeAlgorithm();
        jta1.setNetwork(net);

        BayesNet net2 = rdr.readFromString(new XMLBIFWriter().write(net));

        JunctionTreeAlgorithm jta2 = new JunctionTreeAlgorithm();
        jta2.setNetwork(net2);

        for (BayesNode node : net.getNodes()) {
            assertArrayEquals(jta1.getBeliefs(node), jta2.getBeliefs(node), 0.000001);
        }

    }
}
