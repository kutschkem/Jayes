package org.eclipse.recommenders.tests.jayes.testgeneration;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.recommenders.jayes.BayesNode;

public class TestCase {
	
	public Map<BayesNode,String> evidence = new HashMap<BayesNode,String>();
	public Map<BayesNode,double[]> beliefs = new HashMap<BayesNode,double[]>();

}
