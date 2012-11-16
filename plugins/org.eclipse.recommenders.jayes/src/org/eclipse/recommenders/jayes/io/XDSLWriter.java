package org.eclipse.recommenders.jayes.io;

import java.io.FileWriter;
import java.io.IOException;

import org.eclipse.recommenders.jayes.BayesNet;
import org.eclipse.recommenders.jayes.BayesNode;
import org.eclipse.recommenders.jayes.io.util.XMLUtil;

public class XDSLWriter {

    private static final String xmlHeader = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n";
    private static final String comment = "<!--\n\t Bayesian Network in XDSL format \n-->\n";
    private static final String templateXDSL = "<smile version=\"1.0\" id=\"%1$s\" numsamples=\"1000\" discsamples=\"10000\">\n"
            + "\t<nodes>%2$s\n\t</nodes>\n</smile>";

    public String write(BayesNet net) {
        StringBuilder bldr = new StringBuilder();
        bldr.append(xmlHeader);
        bldr.append(comment);
        bldr.append(templateXDSL);

        String variableDefs = getVariableDefs(net);

        return String.format(bldr.toString(), net.getName(), XMLUtil.addTab("\n" + variableDefs));
    }

    private String getVariableDefs(BayesNet net) {
        StringBuilder bldr = new StringBuilder();

        for (BayesNode node : net.getNodes()) {
            String states = encodeStates(node);
            String parents = encodeParents(node);
            String probs = encodeProbabilities(node);
            bldr.append(XMLUtil.surround(states + parents + "\n" + probs, "cpt", "id", node.getName()));
            bldr.append("\n");
        }

        return bldr.toString();
    }

    private String encodeStates(BayesNode node) {
        StringBuilder bldr = new StringBuilder();

        for (String outcome : node.getOutcomes()) {
            bldr.append(XMLUtil.emptyTag("state", "id", XMLUtil.clean(outcome)));
            bldr.append("\n");
        }

        return bldr.toString();
    }

    private String encodeParents(BayesNode node) {
        StringBuilder bldr = new StringBuilder();

        for (BayesNode p : node.getParents()) {
            // XDSL can't handle names containing whitespaces!
            bldr.append(p.getName().trim().replaceAll("\\s+", "_"));
            bldr.append(" ");
        }

        return XMLUtil.surround(bldr.toString(), "parents");
    }

    private String encodeProbabilities(BayesNode node) {
        StringBuilder bldr = new StringBuilder();

        for (double d : node.getFactor().getValues().getDouble()) {
            bldr.append(d);
            bldr.append(" ");
        }
        return XMLUtil.surround(bldr.toString(), "probabilities");
    }

    public void writeToFile(BayesNet net, String filename) throws IOException {
        FileWriter wrtr = new FileWriter(filename);
        wrtr.write(write(net));
        wrtr.close();
    }

}
