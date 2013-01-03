/*******************************************************************************
 * Copyright (c) 2012 Michael Kutschke. All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Michael Kutschke - initial API and implementation
 ******************************************************************************/
package org.eclipse.recommenders.jayes.transformation;

import org.eclipse.recommenders.jayes.BayesNet;
import org.eclipse.recommenders.jayes.BayesNode;
import org.eclipse.recommenders.jayes.transformation.util.DecompositionFailedException;

/**
 * Interface for decomposition algorithms. The basic idea is to decompose a probability distribution p(x,y) by
 * introducing a new variable z such that <br/>
 * \f$ p(x,y) = \sum_z p(x,z)p(z,y) \f$ <br/>
 * This can be done through matrix decomposition.
 */
public interface IDecompositionStrategy {

    /**
     * decomposes a single conditional probability distribution.
     * 
     * @param node
     *            node contained in net
     * @throws DecompositionFailedException
     *             when the decomposition failed. In this case, the strategy should not alter the network
     */
    void decompose(BayesNet net, BayesNode node) throws DecompositionFailedException;

}
