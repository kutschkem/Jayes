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
package org.eclipse.recommenders.jayes.testgen.sat;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class HornClause implements Cloneable {

    private Integer positiveLiteral;
    private Set<Integer> negativeLiterals;

    /**
     * 
     * @param posLiteral
     *            can be null to indicate there is no positive literal
     */
    public HornClause(Integer posLiteral, Collection<Integer> negLiterals) {
        this.positiveLiteral = posLiteral;
        this.negativeLiterals = new LinkedHashSet<Integer>(negLiterals);
    }

    public HornClause(Integer posLiteral, Integer... negLiterals) {
        this(posLiteral, Arrays.asList(negLiterals));
    }

    public Integer getPositiveLiteral() {
        return positiveLiteral;
    }

    public void removeNegativeLiteral(int lit) {
        negativeLiterals.remove(lit);
    }

    public boolean containsNegativeLiteral(int lit) {
        return negativeLiterals.contains(lit);
    }

    public boolean hasNegativeLiteral() {
        return !negativeLiterals.isEmpty();
    }

    public Set<Integer> getNegativeLiterals() {
        return Collections.unmodifiableSet(negativeLiterals);
    }

    @Override
    public HornClause clone() {
        HornClause clone = null;
        try {
            clone = (HornClause) super.clone();
            clone.negativeLiterals = new LinkedHashSet<Integer>(negativeLiterals);
        } catch (CloneNotSupportedException e) {
            // can't happen, we are cloneable
            e.printStackTrace();
        }
        return clone;
    }

    @Override
    public String toString() {
        return negativeLiterals.toString() + "->" + positiveLiteral;
    }

}
