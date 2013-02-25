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

import static org.eclipse.recommenders.jayes.io.util.XDSLConstants.CPT;
import static org.eclipse.recommenders.jayes.io.util.XDSLConstants.ID;
import static org.eclipse.recommenders.jayes.io.util.XDSLConstants.PARENTS;
import static org.eclipse.recommenders.jayes.io.util.XDSLConstants.PROBABILITIES;
import static org.eclipse.recommenders.jayes.io.util.XDSLConstants.STATE;

import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.lang3.StringEscapeUtils;
import org.eclipse.recommenders.jayes.BayesNet;
import org.eclipse.recommenders.jayes.BayesNode;
import org.eclipse.recommenders.jayes.io.util.XMLUtil;

public class XDSLWriter {

    private static final String xmlHeader = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n";
    private static final String comment = "<!--\n\t Bayesian Network in XDSL format \n-->\n";

    public String write(BayesNet net) {
        StringBuilder bldr = new StringBuilder();
        bldr.append(xmlHeader);
        bldr.append(comment);

        int offset = bldr.length();
        getVariableDefs(bldr, net);
        XMLUtil.surround(offset, bldr, "nodes");
        XMLUtil.surround(offset, bldr, "smile", "version", "1.0", ID, net.getName(), "numsamples", "1000",
                "discsamples", "10000");

        return bldr.toString();
    }

    private void getVariableDefs(StringBuilder bldr, BayesNet net) {
        for (BayesNode node : net.getNodes()) {
            int offset = bldr.length();
            encodeStates(bldr, node);
            encodeParents(bldr, node);
            bldr.append('\n');
            encodeProbabilities(bldr, node);
            XMLUtil.surround(offset, bldr, CPT, ID, node.getName());
            bldr.append('\n');
        }
    }

    private void encodeStates(StringBuilder bldr, BayesNode node) {
        for (String outcome : node.getOutcomes()) {
            XMLUtil.emptyTag(bldr, STATE, ID, StringEscapeUtils.escapeXml(outcome));
            bldr.append('\n');
        }
    }

    private void encodeParents(StringBuilder bldr, BayesNode node) {
        int offset = bldr.length();
        for (BayesNode p : node.getParents()) {
            // XDSL can't handle names containing whitespaces!
            bldr.append(p.getName().trim().replaceAll("\\s+", "_"));
            bldr.append(' ');
        }
        if (!node.getParents().isEmpty()) {
            bldr.deleteCharAt(bldr.length() - 1); // delete last whitespace
        }

        XMLUtil.surround(offset, bldr, PARENTS);
    }

    private void encodeProbabilities(StringBuilder bldr, BayesNode node) {
        if (node.getProbabilities().length == 0) {
            throw new IllegalArgumentException("Bayesian Network is broken: " + node.getName()
                    + " has an empty conditional probability table");
        }
        int offset = bldr.length();
        for (Number d : node.getFactor().getValues()) {
            bldr.append(d);
            bldr.append(' ');
        }
        bldr.deleteCharAt(bldr.length() - 1); // delete last whitespace
        XMLUtil.surround(offset, bldr, PROBABILITIES);
    }

    public void writeToFile(BayesNet net, String filename) throws IOException {
        FileWriter wrtr = new FileWriter(filename);
        wrtr.write(write(net));
        wrtr.close();
    }

}
