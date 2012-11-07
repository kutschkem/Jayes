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

import java.io.FileWriter;
import java.io.IOException;

import org.eclipse.recommenders.jayes.BayesNet;
import org.eclipse.recommenders.jayes.BayesNode;
import org.eclipse.recommenders.jayes.io.util.XMLUtil;

/**
 * a Writer thats writes the XMLBIF v0.3 format (<a
 * href="http://www.cs.cmu.edu/~fgcozman/Research/InterchangeFormat/"
 * >specification</a>)
 * 
 * @author Michael Kutschke
 * 
 */
public class XMLBIFWriter {

    private static final String xmlHeader = "<?xml version=\"1.0\"?>\n";
    private static final String comment = "<!--\n\t Bayesian Network in XMLBIF v0.3 \n-->\n";
    private static final String DTD = "<!-- DTD for the XMLBIF 0.3 format -->\n" + "<!DOCTYPE BIF [\n"
            + "\t<!ELEMENT BIF ( NETWORK )*>\n" + "\t\t<!ATTLIST BIF VERSION CDATA #REQUIRED>\n"
            + "\t<!ELEMENT NETWORK ( NAME, ( PROPERTY | VARIABLE | DEFINITION )* )>\n"
            + "\t<!ELEMENT NAME (#PCDATA)>\n" + "\t<!ELEMENT VARIABLE ( NAME, ( OUTCOME |  PROPERTY )* ) >\n"
            + "\t\t<!ATTLIST VARIABLE TYPE (nature|decision|utility) \"nature\">\n"
            + "\t<!ELEMENT OUTCOME (#PCDATA)>\n" + "\t<!ELEMENT DEFINITION ( FOR | GIVEN | TABLE | PROPERTY )* >\n"
            + "\t<!ELEMENT FOR (#PCDATA)>\n" + "\t<!ELEMENT GIVEN (#PCDATA)>\n" + "\t<!ELEMENT TABLE (#PCDATA)>\n"
            + "\t<!ELEMENT PROPERTY (#PCDATA)>\n" + "]>\n";
    private static final String templateBIF = "<BIF VERSION=\"0.3\">\n" + "<NETWORK>\n<NAME>%1$s</NAME>\n\n"
            + "<!-- Variables -->\n%2$s\n" + "<!-- Probability Distributions -->\n%3$s\n" + "</NETWORK>\n</BIF>";

    public String write(BayesNet net) {
        StringBuilder bldr = new StringBuilder();
        bldr.append(xmlHeader);
        bldr.append(comment);
        bldr.append(DTD);
        bldr.append(templateBIF);

        String variables = getVariables(net);
        String variableDefs = getVariableDefs(net);

        return String.format(bldr.toString(), net.getName(), variables, variableDefs);
    }

    public void writeToFile(BayesNet net, String filename) throws IOException {
        FileWriter wrtr = new FileWriter(filename);
        wrtr.write(write(net));
        wrtr.close();
    }

    private String getVariableDefs(BayesNet net) {
        StringBuilder bldr = new StringBuilder();

        for (BayesNode node : net.getNodes()) {
            bldr.append("<DEFINITION>\n\t");
            bldr.append(XMLUtil.surround(node.getName(), "FOR"));
            for (BayesNode parent : node.getParents()) {
                bldr.append("\n\t");
                bldr.append(XMLUtil.surround(parent.getName(), "GIVEN"));
            }
            bldr.append("\n\t<TABLE>");
            for (double d : node.getFactor().getValues().getDouble()) {
                bldr.append(d);
                bldr.append(" ");
            }
            bldr.append("</TABLE>\n</DEFINITION>\n\n");
        }

        return bldr.toString();
    }

    private String getVariables(BayesNet net) {
        StringBuilder bldr = new StringBuilder();

        for (BayesNode node : net.getNodes()) {
            bldr.append("<VARIABLE>\n");
            bldr.append(XMLUtil.surround(node.getName(), "NAME"));
            bldr.append("\n");
            for (String outcome : node.getOutcomes()) {
                bldr.append(XMLUtil.surround(XMLUtil.clean(outcome), "OUTCOME"));
                bldr.append("\n");
            }
            bldr.append("</VARIABLE>\n\n");
        }

        return bldr.toString();
    }
}
