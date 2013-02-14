/*******************************************************************************
 * Copyright (c) 2013 Michael Kutschke.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Michael Kutschke - initial API and implementation
 ******************************************************************************/
package org.eclipse.recommenders.jayes.util.triangulation;

public class MinDegree implements IEliminationHeuristic {

    @Override
    public int getHeuristicValue(QuotientGraph graph, int node) {
        return graph.getNeighbors(node).size();
    }

}
