/**
 * Copyright (c) 2012 Michael Kutschke. All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Michael Kutschke - initial API and implementation.
 */
package org.eclipse.recommenders.jayes.transformation.util;

import java.util.Arrays;

import org.eclipse.recommenders.jayes.util.sharing.CanonicalSet;
import org.eclipse.recommenders.jayes.util.sharing.Entry;

import com.google.common.base.Function;

public class CanonicalDoubleArrayManager implements Function<double[], double[]> {

    private CanonicalSet<double[]> canonicals = new CanonicalSet<double[]>() {

        @Override
        protected Entry<double[]> createEntry(double[] array) {
            return new Entry<double[]>(array) {

                @Override
                protected int computeHash(double[] array) {
                    return Arrays.hashCode(array);
                }

                @Override
                protected boolean equals(double[] array,
                        double[] array2) {
                    return Arrays.equals(array, array2);
                }

            };
        }

        @Override
        protected boolean hasProperType(Object o) {
            return o instanceof double[];
        }

    };

    public double[] getInstance(double[] array) {
        if (!canonicals.contains(array)) {
            canonicals.add(array);
        }
        return canonicals.get(array);
    }

    @Override
    public double[] apply(double[] array) {
        return getInstance(array);
    }

}
