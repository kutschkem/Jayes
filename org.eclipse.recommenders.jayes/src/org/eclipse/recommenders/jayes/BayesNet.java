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
package org.eclipse.recommenders.jayes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.recommenders.jayes.util.BidirectionalMap;

public class BayesNet {

    private List<BayesNode> nodes = new ArrayList<BayesNode>();
    private BidirectionalMap<String, BayesNode> nodeMap = new BidirectionalMap<String, BayesNode>();

    public int addNode(BayesNode node) {
        node.setId(nodes.size());
        nodes.add(node);
        if (nodeMap.containsKey(node.getName())) {
            throw new IllegalStateException("Name conflict: " + node.getName() + " already present");
        }
        nodeMap.put(node.getName(), node);
        return node.getId();
    }

    public BayesNode getNode(String name) {
        return nodeMap.get(name);
    }

    public BayesNode getNode(int id) {
        return nodes.get(id);
    }

    public List<BayesNode> getNodes() {
        return Collections.unmodifiableList(nodes);
    }

}
