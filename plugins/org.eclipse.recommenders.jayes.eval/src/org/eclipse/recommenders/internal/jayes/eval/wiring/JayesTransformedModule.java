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
package org.eclipse.recommenders.internal.jayes.eval.wiring;

import static org.eclipse.recommenders.eval.jayes.memory.JunctionTreeMemoryStatisticsProvider.SPECIFIER;
import static org.eclipse.recommenders.eval.jayes.util.TransformationDecorator.DELEGATE;

import org.eclipse.recommenders.eval.jayes.memory.JTATestAdapter;
import org.eclipse.recommenders.eval.jayes.util.TransformationDecorator;
import org.eclipse.recommenders.jayes.inference.IBayesInferer;
import org.eclipse.recommenders.jayes.transformation.IDecompositionStrategy;
import org.eclipse.recommenders.jayes.transformation.SmoothedFactorDecomposition;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.name.Names;

public class JayesTransformedModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(IBayesInferer.class).to(TransformationDecorator.class);
        bind(IBayesInferer.class).annotatedWith(Names.named(DELEGATE)).to(JTATestAdapter.class);
        bind(JTATestAdapter.class).in(Scopes.SINGLETON);
        bind(String.class).annotatedWith(Names.named(SPECIFIER)).toInstance("JayesTransform");
        bind(IDecompositionStrategy.class).to(SmoothedFactorDecomposition.class);
    }

}
