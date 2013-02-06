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
package org.eclipse.recommenders.eval.jayes.statistics.memory;

import org.eclipse.recommenders.jayes.factor.FactorFactory;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class InjectableFactorFactory extends FactorFactory {

    @Inject(optional = true)
    @Override
    public void setFloatingPointType(@Named("floatingPointType") Class<?> floatingPointType) {
        super.setFloatingPointType(floatingPointType);
    }

    @Inject(optional = true)
    @Override
    public void setLogThreshold(@Named("logthreshold") int threshold) {
        super.setLogThreshold(threshold);
    }

}
