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
package org.eclipse.recommenders.jayes.inference;

import java.util.Map;

import org.eclipse.recommenders.jayes.BayesNet;
import org.eclipse.recommenders.jayes.BayesNode;

public interface IBayesInferer {

    public void setNetwork(BayesNet bayesNet);

    public void setEvidence(Map<BayesNode, String/*outcome*/> evidence);

    public void addEvidence(BayesNode node, String outcome);

    public Map<BayesNode, String> getEvidence();

    public double[] getBeliefs(BayesNode node);

}
