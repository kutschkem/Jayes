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
package org.eclipse.recommenders.tests.jayes.logging;

import java.util.List;
import java.util.Map;

import org.eclipse.recommenders.jayes.Factor;
import org.eclipse.recommenders.jayes.inference.junctionTree.JunctionTreeAlgorithm;
import org.eclipse.recommenders.jayes.util.IArrayWrapper;
import org.eclipse.recommenders.jayes.util.Pair;
import org.eclipse.recommenders.jayes.util.Graph.Edge;

public class JTATestAdapter extends JunctionTreeAlgorithm {
	
	public Factor[] getNodePotentials(){
		return nodePotentials;
	}
	
	public Map<Edge,Factor> getSepsets(){
		return sepSets;
	}
	
	public Map<Edge,int[]> getPreparedMultiplications(){
		return preparedMultiplications;
	}
	
	public int[][] getPreparedQueries(){
		return preparedQueries;
	}
	
	public List<Pair<Factor, IArrayWrapper>> getInitializations(){
		return initializations;
	}

}
