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
package org.eclipse.recommenders.jayes.io;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.recommenders.jayes.BayesNet;
import org.eclipse.recommenders.jayes.BayesNode;
import org.eclipse.recommenders.jayes.io.util.XPathUtil;
import org.eclipse.recommenders.jayes.util.ArrayUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.xpath.XPathEvaluator;
import org.xml.sax.SAXException;

/**
 * a Reader thats reads the XMLBIF v0.3 format (<a
 * href="http://www.cs.cmu.edu/~fgcozman/Research/InterchangeFormat/"
 * >specification</a>)
 * 
 * @author Michael Kutschke
 * 
 */
public class XMLBIFReader {

    public BayesNet read(String filename) throws ParserConfigurationException, SAXException, IOException {
        return read(new File(filename));
    }

    public BayesNet read(File biffile) throws ParserConfigurationException, SAXException, IOException {
        Document doc = obtainDocument(new BufferedInputStream(new FileInputStream(biffile)));

        return readFromDocument(doc);
    }

    private Document obtainDocument(InputStream biffile) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setValidating(true);
        DocumentBuilder docBldr = docBuilderFactory.newDocumentBuilder();
    
        Document doc = docBldr.parse(biffile);
        doc.normalize();
    
        return doc;
    }

    public BayesNet readFromString(String xmlBif) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setValidating(true);
        DocumentBuilder docBldr = docBuilderFactory.newDocumentBuilder();

        Document doc = docBldr.parse(new ByteArrayInputStream(xmlBif.getBytes()));

        return readFromDocument(doc);

    }

    private BayesNet readFromDocument(Document doc) {
        BayesNet net = new BayesNet();

        net.setName(doc.getElementsByTagName("NAME").item(0).getTextContent());

        initializeNodes(doc, net);

        XPathEvaluator xpath = (XPathEvaluator) doc.getFeature("+XPath", null);

        NodeList nodelist = doc.getElementsByTagName("DEFINITION");
        for (int i = 0; i < nodelist.getLength(); i++) {
            Node node = nodelist.item(i);
            String name = XPathUtil.evalXPath(xpath, "FOR", node).next().getTextContent();

            BayesNode bNode = net.getNode(name);

            setParents(bNode, net, node, xpath);

            parseProbabilities(xpath, node, bNode);

        }

        return net;
    }

    private void initializeNodes(Document doc, BayesNet net) {
        XPathEvaluator xpath = (XPathEvaluator) doc.getFeature("+XPath", null);
    
        NodeList nodelist = doc.getElementsByTagName("VARIABLE");
        for (int i = 0; i < nodelist.getLength(); i++) {
            Node node = nodelist.item(i);
            Node name = XPathUtil.evalXPath(xpath, "NAME", node).next();
    
            BayesNode bNode = new BayesNode(name.getTextContent());
    
            for (Iterator<Node> it = XPathUtil.evalXPath(xpath, "OUTCOME", node); it.hasNext();) {
                bNode.addOutcome(it.next().getTextContent());
            }
    
            net.addNode(bNode);
    
        }
    }

    private void setParents(BayesNode bNode, BayesNet net, Node node, XPathEvaluator xpath) {
        List<BayesNode> parents = new ArrayList<BayesNode>();
        for (Iterator<Node> it = XPathUtil.evalXPath(xpath, "GIVEN", node); it.hasNext();) {
            parents.add(net.getNode(it.next().getTextContent()));
        }
        bNode.setParents(parents);
    }

    private void parseProbabilities(XPathEvaluator xpath, Node node, BayesNode bNode) {
        String table = XPathUtil.evalXPath(xpath, "TABLE", node).next().getTextContent();

        List<Double> probabilities = new ArrayList<Double>();
        StringTokenizer tok = new StringTokenizer(table);
        while (tok.hasMoreTokens()) {
            probabilities.add(Double.valueOf(tok.nextToken()));
        }

        bNode.setProbabilities((double[]) ArrayUtils.toPrimitiveArray(probabilities.toArray(new Double[] {})));
    }

	public BayesNet read(InputStream systemResourceAsStream) throws ParserConfigurationException, SAXException, IOException {
		Document doc = obtainDocument(systemResourceAsStream);
		return readFromDocument(doc);
	}

}
