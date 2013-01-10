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
package org.eclipse.recommenders.jayes.testgen.scenario;

import java.util.Collection;
import java.util.Map;

import org.eclipse.recommenders.jayes.BayesNode;

public interface ScenarioGenerator {

    Collection<Map<BayesNode, String>> generate(int number);

    void addObserved(BayesNode node);

    void addHidden(BayesNode node);

    void setFixed(BayesNode node, String outcome);

}
