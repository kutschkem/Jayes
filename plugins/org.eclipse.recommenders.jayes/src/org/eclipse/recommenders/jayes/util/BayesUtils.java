/**
 * Copyright (c) 2011 Michael Kutschke.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Michael Kutschke - initial API and implementation.
 */
package org.eclipse.recommenders.jayes.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.recommenders.jayes.BayesNet;
import org.eclipse.recommenders.jayes.BayesNode;

public class BayesUtils {

    private static void DFS(BayesNode n, Set<BayesNode> visited, List<BayesNode> finished) {
        if (visited.contains(n))
            return;
        visited.add(n);
        for (BayesNode c : n.getChildren())
            DFS(c, visited, finished);
        finished.add(n);
    }

    public static List<BayesNode> topsort(List<BayesNode> list) {
        List<BayesNode> result = new LinkedList<BayesNode>();
        Set<BayesNode> visited = new HashSet<BayesNode>();
        for (BayesNode n : list)
            DFS(n, visited, result);
        Collections.reverse(result);
        return result;
    }

    public static List<Integer> getNodeAndParentIds(final BayesNode n) {
        final List<Integer> nodeAndParents = new ArrayList<Integer>(n.getParents().size() + 1);
        nodeAndParents.add(n.getId());
        for (final BayesNode p : n.getParents()) {
            nodeAndParents.add(p.getId());
        }
        return nodeAndParents;
    }

    public static Map<Integer, Integer> toIntegerMap(final Map<BayesNode, String> evidence) {
        Map<Integer, Integer> intMap = new HashMap<Integer, Integer>();
        for (Entry<BayesNode, String> entry : evidence.entrySet()) {
            BayesNode node = entry.getKey();
            intMap.put(node.getId(), node.getOutcomeIndex(entry.getValue()));
        }
        return intMap;
    }

    public static Map<BayesNode, String> toNodeMap(BayesNet net,
            Map<Integer/*node id*/, Integer/*outcome index*/> evidence) {
        Map<BayesNode, String> result = new HashMap<BayesNode, String>();
        for (Entry<Integer, Integer> entry : evidence.entrySet()) {
            BayesNode node = net.getNode(entry.getKey());
            result.put(node, node.getOutcomeName(entry.getValue()));
        }
        return result;
    }

}
