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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BayesNet {

    private List<BayesNode> nodes = new ArrayList<BayesNode>();
    private Map<String, BayesNode> nodeMap = new HashMap<String, BayesNode>();

    private String name = "Bayesian Network";

    /**
     * 
     * @deprecated use createNode instead
     */
    @Deprecated
    public int addNode(BayesNode node) {
        node.setId(nodes.size());
        nodes.add(node);
        if (nodeMap.containsKey(node.getName())) {
            throw new IllegalStateException("Name conflict: " + node.getName() + " already present");
        }
        nodeMap.put(node.getName(), node);
        return node.getId();
    }

    @SuppressWarnings("deprecation")
    public BayesNode createNode(String name) {
        BayesNode node = new BayesNode(name);
        addNode(node);
        return node;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
