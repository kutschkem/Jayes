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
import java.util.Map;

import org.eclipse.recommenders.jayes.BayesNet;
import org.eclipse.recommenders.jayes.BayesNode;
import org.eclipse.recommenders.jayes.inference.junctionTree.JunctionTreeAlgorithm;

import com.google.gson.stream.JsonWriter;

public class TestcaseSerializer {
	
	private BayesNet net;
	private JunctionTreeAlgorithm algo = new JunctionTreeAlgorithm();
	
	public TestcaseSerializer(BayesNet net){
		this.net = net;
		algo.setNetwork(net);
	}
	
	
	public String writeTestcases(List<Map<BayesNode,String>> testcases){
		StringWriter strWriter = new StringWriter();
		try{
		JsonWriter wrt = new JsonWriter(strWriter);
		wrt.beginArray();
		for(Map<BayesNode,String> testcase : testcases){
			algo.setEvidence(testcase);
			wrt.beginObject();
			for(BayesNode node: net.getNodes()){
				wrt.name(node.getName());
				wrt.beginObject();
				wrt.name("evidence");
				if(testcase.containsKey(node)){
					wrt.value(testcase.get(node));
				}else{
					wrt.nullValue();
				}
				wrt.name("belief");
				wrt.beginArray();
				for(double d: algo.getBeliefs(node)){
					wrt.value(d);
				}
				wrt.endArray();
				wrt.endObject();
			}
			wrt.endObject();
		}
		wrt.endArray();
		wrt.close();
		}catch(IOException ex){//should not happen
			ex.printStackTrace();
		}
		
		return strWriter.getBuffer().toString();
	}

}
