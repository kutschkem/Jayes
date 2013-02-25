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
package org.eclipse.recommenders.tests.jayes.io;

import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.xml.HasXPath.hasXPath;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.recommenders.jayes.BayesNet;
import org.eclipse.recommenders.jayes.BayesNode;
import org.eclipse.recommenders.jayes.inference.junctionTree.JunctionTreeAlgorithm;
import org.eclipse.recommenders.jayes.io.XDSLReader;
import org.eclipse.recommenders.jayes.io.XDSLWriter;
import org.eclipse.recommenders.jayes.io.XMLBIFReader;
import org.eclipse.recommenders.jayes.io.XMLBIFWriter;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class IOTest {

    @Test
    public void XMLBIFreaderTest() throws ParserConfigurationException, SAXException, IOException {
        // tests whether parsing functions
        XMLBIFReader rdr = new XMLBIFReader();
        BayesNet net = rdr.read(new File("test/models/dog.xml"));
        assertTrue(net != null);
        assertEquals(5, net.getNodes().size());
    }

    @Test
    public void XMLBIFroundtripTest() throws ParserConfigurationException, SAXException, IOException {
        // tests whether XMLBIF reader and writer are consistent
        XMLBIFReader rdr = new XMLBIFReader();

        BayesNet net = rdr.read(new File("test/models/dog.xml"));

        JunctionTreeAlgorithm jta1 = new JunctionTreeAlgorithm();
        jta1.setNetwork(net);

        BayesNet net2 = rdr.readFromString(new XMLBIFWriter().write(net));

        JunctionTreeAlgorithm jta2 = new JunctionTreeAlgorithm();
        jta2.setNetwork(net2);

        for (BayesNode node : net.getNodes()) {
            assertEquals(node.getName(), net2.getNode(node.getId()).getName());
            assertArrayEquals(jta1.getBeliefs(node), jta2.getBeliefs(node), 0.000001);
        }

    }

    /**
     * assert that a network directly generated from GeNIe is (1) parsed correctly and (2) gives the same results as
     * GeNIe
     * 
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    @Test
    public void XDSLreaderTest() throws ParserConfigurationException, SAXException, IOException {
        XDSLReader rdr = new XDSLReader();

        BayesNet net = rdr.read("test/models/rain.xdsl");

        JunctionTreeAlgorithm jta = new JunctionTreeAlgorithm();
        jta.setNetwork(net);
        jta.addEvidence(net.getNode("grass_wet"), "yes");
        jta.addEvidence(net.getNode("neighbor_grass_wet"), "yes");

        // compare with computed results from GeNIe
        assertArrayEquals(new double[] { 0.7271, 0.2729 }, jta.getBeliefs(net.getNode("sprinkler_on")), 0.0001);
        assertArrayEquals(new double[] { 0.4596, 0.5404 }, jta.getBeliefs(net.getNode("rain")), 0.0001);

    }

    @Test
    public void XDSLWriterTest() throws Exception {
        XDSLReader rdr = new XDSLReader();

        BayesNet net = rdr.read("test/models/rain.xdsl");

        XDSLWriter wrtr = new XDSLWriter();
        String xdslRepresentation = wrtr.write(net);

        // check that there are no nested cpt's
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBldr = docBuilderFactory.newDocumentBuilder();
        Document doc = docBldr.parse(new InputSource(new StringReader(xdslRepresentation)));
        doc.normalize();

        assertThat(doc.getDocumentElement(), hasXPath("//cpt"));
        assertThat(doc.getDocumentElement(), not(hasXPath("//cpt/cpt")));
    }

    @Test
    public void XDSLroundtripTest() throws ParserConfigurationException, SAXException, IOException {
        XDSLReader rdr = new XDSLReader();

        BayesNet net = rdr.read("test/models/rain.xdsl");

        XDSLWriter wrtr = new XDSLWriter();
        String xdslRepresentation = wrtr.write(net);

        net = rdr.readFromString(xdslRepresentation);

        JunctionTreeAlgorithm jta = new JunctionTreeAlgorithm();
        jta.setNetwork(net);
        jta.addEvidence(net.getNode("grass_wet"), "yes");
        jta.addEvidence(net.getNode("neighbor_grass_wet"), "yes");

        // compare with computed results from GeNIe
        assertArrayEquals(new double[] { 0.7271, 0.2729 }, jta.getBeliefs(net.getNode("sprinkler_on")), 0.0001);
        assertArrayEquals(new double[] { 0.4596, 0.5404 }, jta.getBeliefs(net.getNode("rain")), 0.0001);
    }
}
