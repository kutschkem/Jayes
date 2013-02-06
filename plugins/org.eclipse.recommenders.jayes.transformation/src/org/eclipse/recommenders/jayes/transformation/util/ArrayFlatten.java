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
package org.eclipse.recommenders.jayes.transformation.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.common.base.Function;

public class ArrayFlatten implements Function<double[][], double[]> {

    public static List<double[]> unflatten(double[] array, int subsize) {
        if (array.length % subsize != 0) {
            throw new IllegalArgumentException("array.length % subsize != 0");
        }
        List<double[]> doubles = new ArrayList<double[]>();
        for (int i = 0; i < array.length; i += subsize) {
            doubles.add(Arrays.copyOfRange(array, i, i + subsize));
        }
        return doubles;
    }

    public static double[] flatten(final double[][] array) {
        int length = 0;
        for (double[] arr : array) {
            length += arr.length;
        }
        final double[] result = new double[length];
        int index = 0;
        for (final double[] arr : array) {
            for (double d : arr) {
                result[index] = d;
                index++;
            }
        }
        return result;
    }

    @Override
    public double[] apply(double[][] array) {
        return flatten(array);
    }

}
