/*******************************************************************************
 * Copyright (c) 2012 Michael Kutschke.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Michael Kutschke - initial API and implementation
 ******************************************************************************/
package org.eclipse.recommenders.eval.jayes.statistics.memory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;

import org.eclipse.recommenders.eval.jayes.statistics.IStatisticsProvider;
import org.eclipse.recommenders.eval.jayes.util.JTATestAdapter;
import org.eclipse.recommenders.jayes.factor.AbstractFactor;
import org.eclipse.recommenders.jayes.factor.arraywrapper.IArrayWrapper;
import org.eclipse.recommenders.jayes.util.MathUtils;
import org.eclipse.recommenders.jayes.util.Pair;

import com.google.common.primitives.Ints;
import com.google.inject.Inject;
import com.google.inject.name.Named;

public class JunctionTreeMemoryStatisticsProvider implements IStatisticsProvider {

    public static final String SPECIFIER = "specifier";

    private JTATestAdapter jta;
    private String specifier;

    @Inject
    public JunctionTreeMemoryStatisticsProvider(JTATestAdapter jta, @Named(SPECIFIER) String specifier) {
        this.jta = jta;
        this.specifier = specifier;
    }

    private void reportMemorySavingsFromFlyweights(Map<String, Number> map) {
        this.reportMemorySavingsFromFlyweightPatternForInitalizations(map);
        this.reportMemorySavingsFromFlyweightPatternForPreparedOperations(map);
    }

    private void reportMemorySavingsFromFlyweightPatternForInitalizations(Map<String, Number> map) {
        IdentityHashMap<IArrayWrapper, IArrayWrapper> instances = new IdentityHashMap<IArrayWrapper, IArrayWrapper>();
        int factorSizes = 0;
        for (final AbstractFactor pot : jta.getNodePotentials()) {
            factorSizes += pot.getValues().length();
            instances.put(pot.getValues(), pot.getValues());
        }

        for (final AbstractFactor sep : jta.getSepsets().values()) {
            factorSizes += sep.getValues().length();
            instances.put(sep.getValues(), sep.getValues());
        }

        int flyweightsize = 0;
        for (IArrayWrapper d : instances.keySet()) {
            flyweightsize += d.length();
        }

        map.put(specify("initializations, original size"), factorSizes);
        map.put(specify("initializations, shared size"), flyweightsize);
        map.put(specify("initializations, ratio shared/orig."), ((double) flyweightsize / factorSizes));
    }

    private String specify(String string) {
        return string + " (" + specifier + ")";
    }

    private void reportMemorySavingsFromFlyweightPatternForPreparedOperations(Map<String, Number> map) {
        IdentityHashMap<int[], int[]> instances = new IdentityHashMap<int[], int[]>();
        int factorSizes = 0;
        for (int[] intArr : jta.getPreparedMultiplications().values()) {
            factorSizes += intArr.length;
            instances.put(intArr, intArr);
        }

        for (int[] prep : jta.getPreparedQueries()) {
            factorSizes += prep.length;
            instances.put(prep, prep);
        }

        int flyweightsize = 0;
        for (int[] d : instances.keySet()) {
            flyweightsize += d.length;
        }

        map.put(specify("prepared ops, original size"), factorSizes);
        map.put(specify("prepared ops, shared size"), flyweightsize);
        map.put(specify("prepared ops, ratio shared/orig."), ((double) flyweightsize / factorSizes));
    }

    private void reportSparsenessInfo(Map<String, Number> map) {
        int denseLength = 0;
        int sparseLength = 0;
        for (AbstractFactor f : jta.getNodePotentials()) {
            denseLength += MathUtils.product(f.getDimensions());//the length that a dense factor would have
            sparseLength += f.getValues().length() + f.getOverhead();
        }

        map.put(specify("dense factor size"), denseLength);
        map.put(specify("sparse factor size"), sparseLength);
        map.put(specify("ratio sparse/dense"), ((double) sparseLength / denseLength));
    }

    private void reportCompleteMemoryInfo(Map<String, Number> map) {
        long size = estimateMemoryConsumption();

        map.put(specify("approx. total size of the model, in bytes"), size);
    }

    private long estimateMemoryConsumption() {
        long bytes = 0; // size in bytes
        for (AbstractFactor f : jta.getNodePotentials()) {
            bytes += f.getValues().length() * f.getValues().sizeOfElement();
            bytes += f.getDimensions().length * Ints.BYTES;
            bytes += f.getDimensionIDs().length * Ints.BYTES;
            bytes += f.getOverhead();
        }
        for (AbstractFactor f : jta.getSepsets().values()) {
            bytes += f.getValues().length() * f.getValues().sizeOfElement();
            bytes += f.getDimensions().length * Ints.BYTES;
            bytes += f.getDimensionIDs().length * Ints.BYTES;
        }
        HashSet<int[]> check = new HashSet<int[]>();
        for (int[] p : jta.getPreparedMultiplications().values()) {
            if (!check.contains(p)) {
                check.add(p);
                bytes += p.length * Ints.BYTES;
            }
        }
        for (int[] p : jta.getPreparedQueries()) {
            if (!check.contains(p)) {
                check.add(p);
                bytes += p.length * Ints.BYTES;
            }
        }
        HashSet<IArrayWrapper> check2 = new HashSet<IArrayWrapper>();
        for (Pair<AbstractFactor, IArrayWrapper> p : jta.getInitializations()) {
            if (!check2.contains(p.getSecond())) {
                check2.add(p.getSecond());
                bytes += p.getSecond().length() * p.getSecond().sizeOfElement();
            }
        }
        return bytes;
    }

    @Override
    public Map<String, Number> computeStatistics() {
        Map<String, Number> statistics = new HashMap<String, Number>();
        this.reportMemorySavingsFromFlyweights(statistics);
        this.reportCompleteMemoryInfo(statistics);
        this.reportSparsenessInfo(statistics);
        return statistics;
    }

}
