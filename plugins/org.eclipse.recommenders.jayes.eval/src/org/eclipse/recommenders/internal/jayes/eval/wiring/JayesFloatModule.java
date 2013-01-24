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

import org.eclipse.recommenders.eval.jayes.memory.InjectableFactorFactory;
import org.eclipse.recommenders.eval.jayes.memory.JTATestAdapter;
import org.eclipse.recommenders.jayes.factor.FactorFactory;
import org.eclipse.recommenders.jayes.inference.IBayesInferer;
import org.eclipse.recommenders.jayes.inference.junctionTree.JunctionTreeAlgorithm;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

public class JayesFloatModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(IBayesInferer.class).to(JunctionTreeAlgorithm.class);
        bind(JunctionTreeAlgorithm.class).to(JTATestAdapter.class);
        bind(JTATestAdapter.class).in(Scopes.SINGLETON); // important for IStatisticsProvider
        bind(String.class).annotatedWith(Names.named(SPECIFIER)).toInstance("JayesFloat");
        bind(FactorFactory.class).to(InjectableFactorFactory.class);
        bind(new TypeLiteral<Class<?>>() {
        }).annotatedWith(Names.named("floatingPointType")).toInstance(float.class);

    }

}
