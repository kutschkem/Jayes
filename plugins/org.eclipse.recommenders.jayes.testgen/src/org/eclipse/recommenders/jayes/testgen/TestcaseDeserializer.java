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
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.recommenders.jayes.BayesNet;
import org.eclipse.recommenders.jayes.BayesNode;
import org.eclipse.recommenders.jayes.util.ArrayUtils;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

public class TestcaseDeserializer {
	
	private BayesNet net;
	
	public TestcaseDeserializer(BayesNet net){
		this.net = net;
	}
	
	public List<TestCase> deserialize(String string) throws IOException{
		JsonReader rdr = new JsonReader(new StringReader(string));
		List<TestCase> result = new ArrayList<TestCase>();
		rdr.beginArray();
		while(rdr.hasNext()){//single test cases
			TestCase tc = new TestCase();
			rdr.beginObject();
			while(rdr.hasNext()){
			BayesNode node = net.getNode(rdr.nextName());
			rdr.beginObject();
			String str = rdr.nextName();
			assert(str.equals("evidence"));
			if(rdr.peek() != JsonToken.NULL){
				String evidence = rdr.nextString();
				tc.evidence.put(node, evidence);
			}else{
				rdr.nextNull();
			}
			str = rdr.nextName();
			assert(str.equals("belief"));
			List<Double> doubles = new ArrayList<Double>();
			rdr.beginArray();
			while(rdr.hasNext()){
				doubles.add(rdr.nextDouble());
			}
			rdr.endArray();
			tc.beliefs.put(node, (double[]) ArrayUtils.toPrimitiveArray(doubles.toArray(new Double[0])));
			rdr.endObject();
			}
			rdr.endObject();
			result.add(tc);
		}
		rdr.endArray();
		rdr.close();
		
		return result;
	}

}
