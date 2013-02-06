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

import static com.google.common.primitives.Doubles.toArray;
import static org.eclipse.recommenders.jayes.testgen.TestcaseSerializer.BELIEF;
import static org.eclipse.recommenders.jayes.testgen.TestcaseSerializer.EVIDENCE;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.recommenders.jayes.BayesNet;
import org.eclipse.recommenders.jayes.BayesNode;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

public class TestcaseDeserializer {

    private BayesNet net;

    public TestcaseDeserializer(BayesNet net) {
        this.net = net;
    }

    public List<TestCase> deserialize(String string) throws IOException {
        return deserialize(new StringReader(string));
    }

    public List<TestCase> deserialize(Reader rdr) throws IOException {
        JsonReader jsonRdr = new JsonReader(rdr);
        List<TestCase> result = new ArrayList<TestCase>();
        jsonRdr.beginArray();
        while (jsonRdr.hasNext()) {//single test cases
            TestCase tc = new TestCase();
            jsonRdr.beginObject();
            while (jsonRdr.hasNext()) {
                BayesNode node = net.getNode(jsonRdr.nextName());
                jsonRdr.beginObject();
                readEvidence(jsonRdr, node, tc);
                readBeliefs(jsonRdr, node, tc);
                jsonRdr.endObject();
            }
            jsonRdr.endObject();
            result.add(tc);
        }
        jsonRdr.endArray();
        jsonRdr.close();

        return result;
    }

    private void readEvidence(JsonReader jsonRdr, BayesNode node, TestCase tc) throws IOException {
        String str = jsonRdr.nextName();
        assert (str.equals(EVIDENCE));
        if (jsonRdr.peek() != JsonToken.NULL) {
            String evidence = jsonRdr.nextString();
            tc.evidence.put(node, evidence);
        } else {
            jsonRdr.nextNull();
        }
    }

    private void readBeliefs(JsonReader jsonRdr, BayesNode node, TestCase tc) throws IOException {
        String str = jsonRdr.nextName();
        assert (str.equals(BELIEF));
        List<Double> doubles = new ArrayList<Double>();
        jsonRdr.beginArray();
        while (jsonRdr.hasNext()) {
            doubles.add(jsonRdr.nextDouble());
        }
        jsonRdr.endArray();
        tc.beliefs.put(node, toArray(doubles));
    }

}
