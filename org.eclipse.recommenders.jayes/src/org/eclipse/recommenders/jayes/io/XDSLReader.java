/*******************************************************************************
 * Copyright (c) 2012 Michael Kutschke.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Michael Kutschke - initial API and implementation
 ******************************************************************************/
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

public class XDSLReader {

    public BayesNet read(String filename) throws IOException {
        return read(new File(filename));
    }

    public BayesNet read(File biffile) throws IOException {
    	return read(new BufferedInputStream(new FileInputStream(biffile)));
    }
    
    public BayesNet read(InputStream str) throws IOException{
    	Document doc = obtainDocument(str);
    	
    	return readFromDocument(doc);
    }

    private Document obtainDocument(InputStream xdslStream) throws IOException {
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        // docBuilderFactory.setValidating(true);
        DocumentBuilder docBldr;
        try {
            docBldr = docBuilderFactory.newDocumentBuilder();
            Document doc = docBldr.parse(xdslStream);

            doc.normalize();
            return doc;
        } catch (ParserConfigurationException e) {
            throw new IOException("Bad parser configuration, probably missing dependency", e);
        } catch (SAXException e) {
            throw new IOException("Parse failed", e);
        }

    }

    public BayesNet readFromString(String xdslString) throws IOException {
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setValidating(true);
        try {
            DocumentBuilder docBldr = docBuilderFactory.newDocumentBuilder();

            Document doc = docBldr.parse(new ByteArrayInputStream(xdslString.getBytes()));

            return readFromDocument(doc);
        } catch (ParserConfigurationException e) {
            throw new IOException("Bad parser configuration, probably missing dependency", e);
        } catch (SAXException e) {
            throw new IOException("Parse failed", e);
        }

    }

    private BayesNet readFromDocument(Document doc) {
        BayesNet net = new BayesNet();

        Node smileNode = doc.getElementsByTagName("smile").item(0);
        String networkName = getId(smileNode);
        net.setName(networkName);

        initializeNodes(doc, net);

        XPathEvaluator xpath = (XPathEvaluator) doc.getFeature("+XPath", null);

        NodeList nodelist = doc.getElementsByTagName("cpt");
        for (int i = 0; i < nodelist.getLength(); i++) {
            Node node = nodelist.item(i);
            String name = getId(node);

            BayesNode bNode = net.getNode(name);

            setParents(bNode, net, node, xpath);

            parseProbabilities(xpath, node, bNode);

        }

        return net;
    }

    private String getId(Node node) {
        return node.getAttributes().getNamedItem("id").getTextContent();
    }

    private void initializeNodes(Document doc, BayesNet net) {
        XPathEvaluator xpath = (XPathEvaluator) doc.getFeature("+XPath", null);

        NodeList nodelist = doc.getElementsByTagName("cpt");
        for (int i = 0; i < nodelist.getLength(); i++) {
            Node node = nodelist.item(i);

            BayesNode bNode = new BayesNode(getId(node));

            for (Iterator<Node> it = XPathUtil.evalXPath(xpath, "state", node); it.hasNext();) {
                bNode.addOutcome(getId(it.next()));
            }

            net.addNode(bNode);

        }
    }

    private void setParents(BayesNode bNode, BayesNet net, Node node, XPathEvaluator xpath) {
        List<BayesNode> parents = new ArrayList<BayesNode>();
        Iterator<Node> parentNode = XPathUtil.evalXPath(xpath, "parents", node);
        List<String> parentNames = new ArrayList<String>();
        if (parentNode.hasNext()) {
            StringTokenizer tokenizer = new StringTokenizer(parentNode.next().getTextContent());
            while (tokenizer.hasMoreTokens()) {
                parentNames.add(tokenizer.nextToken());
            }
        }

        for (String parentname : parentNames) {
            parents.add(net.getNode(parentname));
        }

        bNode.setParents(parents);
    }

    private void parseProbabilities(XPathEvaluator xpath, Node node, BayesNode bNode) {
        String table = XPathUtil.evalXPath(xpath, "probabilities", node).next().getTextContent();

        List<Double> probabilities = new ArrayList<Double>();
        StringTokenizer tok = new StringTokenizer(table);
        while (tok.hasMoreTokens()) {
            probabilities.add(Double.valueOf(tok.nextToken()));
        }

        bNode.setProbabilities((double[]) ArrayUtils.toPrimitiveArray(probabilities.toArray(new Double[] {})));
    }

}
