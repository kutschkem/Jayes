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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.recommenders.jayes.factor.AbstractFactor;
import org.eclipse.recommenders.jayes.util.BidirectionalMap;
import org.eclipse.recommenders.jayes.util.MathUtils;
import org.eclipse.recommenders.jayes.util.Pair;
import org.eclipse.recommenders.jayes.util.sharing.CanonicalIntArraySet;

public class Learner {

    private BidirectionalMap<Pair<Integer, Integer>, Integer> translation = new BidirectionalMap<Pair<Integer, Integer>, Integer>();
    private HornSAT sat = new HornSAT();

    public BidirectionalMap<Pair<Integer, Integer>, Integer> getTranslation() {
        return translation;
    }

    public HornSAT getSat() {
        return sat;
    }

    public void learnOnes(AbstractFactor f) {
        List<int[]> ones = findValues(f, 1.0);
        // partition by last dimension ( the others are causes)
        List<int[]>[] partitionedOnes = partitionByLastDimension(f, ones);
        // LEARN
        List<int[]> clauses = new ArrayList<int[]>();
        for (List<int[]> partOnes : partitionedOnes) {
            generalize(f, partOnes, clauses);
        }

        for (int[] clause : clauses) {
            sat.addClause(buildClause(f, clause));
        }
    }

    public void learnZeros(AbstractFactor f) {
        List<int[]> zeros = findValues(f, 0.0);
        List<int[]>[] partitionedZeros = partitionByLastDimension(f, zeros);
        // LEARN
        List<int[]> clauses = new ArrayList<int[]>();
        for (List<int[]> partZeros : partitionedZeros) {
            generalize(f, partZeros, clauses);
        }

        for (int[] clause : clauses) {
            sat.addClause(buildNegClause(f, clause));
        }
    }

    private List<int[]> findValues(AbstractFactor f, double value) {
        List<int[]> ones = new ArrayList<int[]>();
        // find all ones ( == logical, causal relationships)
        for (int i = 0; i < MathUtils.product(f.getDimensions()); i++)
            if (f.getValue(i) == value)
                ones.add(virtualAddr(f.getDimensions(), i));
        return ones;
    }

    public static int[] virtualAddr(int[] dimensions, int address) {
        int[] result = new int[dimensions.length];
        for (int i = dimensions.length - 1; i >= 0; i--) {
            result[i] = address % dimensions[i];
            address /= dimensions[i];
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<int[]>[] partitionByLastDimension(AbstractFactor f, List<int[]> ones) {
        List<int[]>[] partitionedOnes = new List[f.getDimensions()[f.getDimensions().length - 1]];
        for (int i = 0; i < partitionedOnes.length; i++) {
            partitionedOnes[i] = new ArrayList<int[]>();
        }
        for (int[] one : ones)
            partitionedOnes[one[one.length - 1]].add(one);
        return partitionedOnes;
    }

    private void generalize(AbstractFactor f, List<int[]> clauses, List<int[]> result) {
        List<int[]> learned = clauses;
        for (int i = 0; i < f.getDimensions().length; i++) {
            // i is the number of generalized variables ( variables -
            // variables in the clauses)
            List<int[]> generalizedClauses = partition(f, learned, new HashSet<Integer>(), i);
            removeExplainableClauses(learned, generalizedClauses);
            // clauses that could not be generalized need to be learned
            Set<int[]> canonicals = new CanonicalIntArraySet();
            canonicals.addAll(learned);
            result.addAll(canonicals);
            learned = generalizedClauses;
        }
    }

    private void removeExplainableClauses(List<int[]> clauses, List<int[]> generalizedClauses) {
        Iterator<int[]> it = clauses.iterator();
        while (it.hasNext()) { // remove clauses that were generalized
            int[] toBeExpl = it.next();
            for (int[] learnedCl : generalizedClauses) {
                if (isExplainedBy(toBeExpl, learnedCl)) {
                    it.remove();
                    break;
                }
            }
        }
    }

    private boolean isExplainedBy(int[] clauseToBeExplained, int[] generalizedClause) {
        for (int j = 0; j < generalizedClause.length; j++) {
            if (generalizedClause[j] != -1) {
                if (generalizedClause[j] != clauseToBeExplained[j]) {
                    return false;
                }
            }
        }
        return true;
    }

    private HornClause buildNegClause(AbstractFactor f, int[] clause) {
        Pair<Integer, Integer> concl = new Pair<Integer, Integer>(f.getDimensionIDs()[clause.length - 1],
                clause[clause.length - 1]);
        List<Pair<Integer, Integer>> prem = new ArrayList<Pair<Integer, Integer>>();
        for (int i = 0; i < clause.length - 1; i++) {
            if (clause[i] == -1)
                continue;
            prem.add(new Pair<Integer, Integer>(f.getDimensionIDs()[i], clause[i]));
        }
        prem.add(concl);
        List<Integer> premices = new ArrayList<Integer>();
        for (Pair<Integer, Integer> p : prem) {
            if (!translation.containsKey(p))
                translation.put(p, translation.size());
            premices.add(translation.get(p));
        }

        return new HornClause(null, premices);
    }

    private HornClause buildClause(AbstractFactor f, int[] clause) {
        Pair<Integer, Integer> concl = new Pair<Integer, Integer>(f.getDimensionIDs()[clause.length - 1],
                clause[clause.length - 1]);
        List<Pair<Integer, Integer>> prem = new ArrayList<Pair<Integer, Integer>>();
        for (int i = 0; i < clause.length - 1; i++) {
            if (clause[i] == -1)
                continue;
            prem.add(new Pair<Integer, Integer>(f.getDimensionIDs()[i], clause[i]));
        }
        List<Integer> premices = new ArrayList<Integer>();
        for (Pair<Integer, Integer> p : prem) {
            if (!translation.containsKey(p))
                translation.put(p, translation.size());
            premices.add(translation.get(p));
        }
        if (!translation.containsKey(concl))
            translation.put(concl, translation.size());

        return new HornClause(translation.get(concl), premices);
    }

    private List<int[]> partition(AbstractFactor f, List<int[]> ones, Set<Integer> done, int numGeneralizedVars) {
        List<int[]> result = new ArrayList<int[]>();

        if (done.size() == f.getDimensions().length - 2) {
            int dim = 0;
            for (int i = 0; i < f.getDimensions().length - 1; i++)
                if (!done.contains(i))
                    dim = i;
            if (ones.size() == f.getDimensions()[dim]) {
                int[] one = Arrays.copyOf(ones.get(0), ones.get(0).length);
                one[dim] = -1;
                result.add(one);
            }
        } else {
            for (int i = 0; i < f.getDimensions().length - 1; i++) {
                if (done.contains(i))
                    continue;
                done.add(i); // for backtracking
                List<int[]>[] parts = partitionByDimension(f, ones, i);

                for (List<int[]> part : parts) {
                    if (part == null)
                        continue;
                    List<int[]> lowerPart = partition(f, part, done, numGeneralizedVars);
                    addInnovations(lowerPart, numGeneralizedVars, result);

                }
                done.remove(i);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<int[]>[] partitionByDimension(AbstractFactor f, List<int[]> clauses, int dimension) {
        List<int[]>[] parts = new List[f.getDimensions()[dimension] + 1];

        for (int[] one : clauses) {
            if (parts[one[dimension] + 1] == null)
                parts[one[dimension] + 1] = new ArrayList<int[]>();
            parts[one[dimension] + 1].add(one);
        }
        return parts;
    }

    private void addInnovations(List<int[]> newClauses, int numGeneralizedVars, List<int[]> result) {
        for (int[] clause : newClauses) {
            int minusOnes = countMinusOnes(clause);
            if (minusOnes > numGeneralizedVars)
                result.add(clause);
        }
    }

    private int countMinusOnes(int[] clause) {
        int minusOnes = 0;
        for (int j = 0; j < clause.length; j++)
            if (clause[j] == -1)
                minusOnes++;
        return minusOnes;
    }

    public String learnedToString() {
        StringBuilder buf = new StringBuilder();
        buf.append("[");
        for (HornClause cl : sat.getClauses()) {
            buf.append('(');
            for (Integer neg : cl.getNegativeLiterals()) {
                buf.append(translation.getKey(neg));
                buf.append(',');
            }
            buf.append("->");
            buf.append(translation.getKey(cl.getPositiveLiteral()));
            buf.append(')');
        }
        buf.append(']');
        return buf.toString();
    }

}
