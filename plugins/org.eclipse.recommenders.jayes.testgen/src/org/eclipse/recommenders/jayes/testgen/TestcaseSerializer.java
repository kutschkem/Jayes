/**
 * Copyright (c) 2012 Michael Kutschke.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Michael Kutschke - initial API and implementation.
 */
package org.eclipse.recommenders.jayes.testgen;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import org.eclipse.recommenders.jayes.BayesNet;
import org.eclipse.recommenders.jayes.BayesNode;

import com.google.gson.stream.JsonWriter;

public class TestcaseSerializer {

    public static final String BELIEF = "belief";
    public static final String EVIDENCE = "evidence";
    private BayesNet net;

    public TestcaseSerializer(BayesNet net) {
        this.net = net;
    }

    public String writeTestcases(List<TestCase> testcases) {
        StringWriter strWriter = new StringWriter();
        try {
            JsonWriter wrt = new JsonWriter(strWriter);
            wrt.beginArray();
            for (TestCase testcase : testcases) {
                wrt.beginObject();
                for (BayesNode node : net.getNodes()) {
                    wrt.name(node.getName());
                    wrt.beginObject();
                    writeEvidence(wrt, testcase, node);
                    writeBelief(wrt, testcase, node);
                    wrt.endObject();
                }
                wrt.endObject();
            }
            wrt.endArray();
            wrt.close();
        } catch (IOException ex) {//should not happen, since we only write to a String
            ex.printStackTrace();
        }

        return strWriter.getBuffer().toString();
    }

    private void writeBelief(JsonWriter wrt, TestCase testcase, BayesNode node) throws IOException {
        wrt.name(BELIEF);
        wrt.beginArray();
        for (double d : testcase.beliefs.get(node)) {
            wrt.value(d);
        }
        wrt.endArray();
    }

    private void writeEvidence(JsonWriter wrt, TestCase testcase, BayesNode node) throws IOException {
        wrt.name(EVIDENCE);
        if (testcase.evidence.containsKey(node)) {
            wrt.value(testcase.evidence.get(node));
        } else {
            wrt.nullValue();
        }
    }

}
