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
package org.eclipse.recommenders.eval.jayes.kmeans;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

public class Cluster {

    private List<double[]> points = new LinkedList<double[]>();
    private double[] center;

    public double[] computeMean() {
        double[] mean = new double[points.get(0).length];
        for (double[] p : points) {
            for (int i = 0; i < p.length; i++)
                mean[i] += p[i];
        }
        for (int i = 0; i < mean.length; i++) {
            mean[i] /= points.size();
        }
        return mean;
    }

    public void add(final double[] point) {
        assert (Iterables.all(points, new ArrayLengthEquals(point)));
        points.add(point);
    }

    public void remove(double[] point) {
        points.remove(point);
    }

    public static void transfer(Cluster c1, Cluster c2, double[] point) {
        if (c1 != null) {
            c1.remove(point);
        }
        c2.add(point);
    }

    public double getCenterDistance(double[] p) {
        if (p.length != center.length)
            throw new IllegalArgumentException("dimensions do not match!");
        double distance = 0;

        for (int i = 0; i < p.length; i++)
            distance += Math.pow(p[i] - center[i], 2);

        distance = Math.sqrt(distance);

        return distance;
    }

    public double[] getCenter() {
        return center;
    }

    public void setCenter(double[] newcenter) {
        center = newcenter;
    }

    public List<double[]> getPoints() {
        return points;
    }

    @Override
    public String toString() {
        return "Cluster with center : " + Arrays.toString(center);
    }

    public void clear() {
        points.clear();
    }

    private static final class ArrayLengthEquals implements Predicate<double[]> {
        private final double[] point;

        private ArrayLengthEquals(double[] point) {
            this.point = point;
        }

        @Override
        public boolean apply(double[] input) {
            return input.length == point.length;
        }
    }

}
