/**
 * Copyright (c) 2010, 2011 Darmstadt University of Technology.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Michael Kutschke - initial API and implementation.
 */
package org.eclipse.recommenders.tests.jayes.testgen;

import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.*;
import static org.junit.matchers.JUnitMatchers.hasItems;

import java.util.Collection;

import org.eclipse.recommenders.jayes.factor.AbstractFactor;
import org.eclipse.recommenders.jayes.factor.DenseFactor;
import org.eclipse.recommenders.jayes.testgen.sat.HornClause;
import org.eclipse.recommenders.jayes.testgen.sat.HornSAT;
import org.eclipse.recommenders.jayes.testgen.sat.Learner;
import org.eclipse.recommenders.jayes.util.ArrayUtils;
import org.eclipse.recommenders.jayes.util.Pair;
import org.eclipse.recommenders.jayes.util.arraywrapper.DoubleArrayWrapper;
import org.junit.Test;

public class SATTest {

    private static final DoubleArrayWrapper DETERMINISTIC_DISTRIBUTRION = new DoubleArrayWrapper(
            ArrayUtils.flatten(new double[][][] {
                    {
                    { 1.0, 0.0 },
                    { 1.0, 0.0 }
            },
                    {
                    { 0.0, 1.0 },
                    { 1.0, 0.0 }
            }
            }));

    @Test
    public void testSatSimple() {
        HornSAT sat = new HornSAT();
        sat.addClause(new HornClause(0));
        sat.addClause(new HornClause(1, 0));
        sat.addClause(new HornClause(2, 0, 1));
        sat.addClause(new HornClause(4, 3));

        assertThat(sat.computeModel(), hasItems(0, 1, 2));
        assertThat(sat.computeModel(), not(hasItems(3, 4)));
    }

    @Test
    public void testLearner() {
        Learner learner = new Learner();

        AbstractFactor f = create2x2x2Factor();

        learner.learnOnes(f);

        HornSAT sat = learner.getSat();
        Integer condition1 = learner.getTranslation().get(new Pair<Integer, Integer>(0, 0));
        sat.addTempClause(new HornClause(condition1));

        Collection<Integer> model = sat.computeModel();
        assertEquals(2, model.size());

        Integer conclusion = learner.getTranslation().get(new Pair<Integer, Integer>(2, 0));
        assertThat(model, hasItems(condition1, conclusion));

        sat.resetTempClauses();
        Integer condition2 = learner.getTranslation().get(new Pair<Integer, Integer>(1, 1));
        sat.addTempClause(new HornClause(condition2));

        model = sat.computeModel();
        assertEquals(2, model.size());
        assertThat(model, hasItems(condition2, conclusion));
    }

    @Test
    public void testLearnerZeroLearning() {
        Learner learner = new Learner();

        AbstractFactor f = create2x2x2Factor();

        learner.learnZeros(f);

        HornSAT sat = learner.getSat();
        sat.addTempClause(new HornClause(learner.getTranslation().get(new Pair<Integer, Integer>(0, 0))));

        assertEquals(1, sat.computeModel().size());

        sat.resetTempClauses();
        sat.addTempClause(new HornClause(learner.getTranslation().get(new Pair<Integer, Integer>(0, 0))));
        sat.addTempClause(new HornClause(learner.getTranslation().get(new Pair<Integer, Integer>(2, 1))));

        assertNull(sat.computeModel());
    }

    private AbstractFactor create2x2x2Factor() {
        AbstractFactor f = new DenseFactor();
        f.setDimensions(2, 2, 2);
        f.setDimensionIDs(0, 1, 2);
        f.setValues(DETERMINISTIC_DISTRIBUTRION);
        return f;
    }

}
