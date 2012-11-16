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
package org.eclipse.recommenders.tests.jayes.testgeneration;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.recommenders.jayes.BayesNet;
import org.eclipse.recommenders.jayes.BayesNode;
import org.eclipse.recommenders.jayes.io.XDSLReader;
import org.eclipse.recommenders.jayes.io.XMLBIFReader;
import org.xml.sax.SAXException;

public class TestGen {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws SAXException 
	 * @throws ParserConfigurationException 
	 */
	public static void main(String[] args) throws IOException, ParserConfigurationException, SAXException {
		if(args.length != 4){
			throw new IllegalArgumentException("need 4 parameters: modelFile, numTests, rate, outputFile");
		}
		
		String modelFile = args[0];
		int numTests = Integer.valueOf(args[1]);
		double rate = Double.valueOf(args[2]);
		String outputFile = args[3];
		BayesNet net = deserializeNet(modelFile);
		
		TestcaseGenerator testgen = new TestcaseGenerator();
		testgen.setBN(net);
		testgen.setEvidenceRate(rate);
		List<Map<BayesNode,String>> testcases = new ArrayList<Map<BayesNode,String>>();
		
		for(int i = 0; i< numTests; i++){
			testcases.add(testgen.testcase());
		}
		
		TestcaseSerializer serializer = new TestcaseSerializer(net);
		String str = serializer.writeTestcases(testcases);
		
		Writer wrt = new BufferedWriter(new FileWriter(outputFile));
		wrt.append(str);
		wrt.close();
	}

	private static BayesNet deserializeNet(String modelFile) throws ParserConfigurationException, SAXException, IOException {
		if(modelFile.matches(".*\\.xml")){
			return new XMLBIFReader().read(modelFile);
		}
		if(modelFile.matches(".*\\.xdsl")){
			return new XDSLReader().read(modelFile);
		}
		throw new IllegalArgumentException("unrecognized file format");
	}

}
