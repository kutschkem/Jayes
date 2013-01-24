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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.eclipse.recommenders.jayes.util.MathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.primitives.Doubles;

public class KMeans {

    private static final Logger logger = LoggerFactory.getLogger(KMeans.class);

    public List<Cluster> kMeans(int k, List<double[]> points) {
        Cluster[] clusters = getInitialClusters(k, points);
        boolean changed = true;
        while (changed) {
            changed = assignPoints(points, clusters);
            for (Cluster c : clusters) {
                c.setCenter(c.computeMean());
                logger.debug(Arrays.toString(c.getCenter()));
            }
        }
        return Arrays.asList(clusters);
    }

    /**
     * computes initial clusters according to k-means++ algorithm (Arthur & Vassilvitskii, 2007)
     * 
     * @param k
     *            number of clusters to initialize
     * @param points
     */
    private Cluster[] getInitialClusters(int k, List<double[]> points) {
        points = new LinkedList<double[]>(points);
        Cluster[] clusters = new Cluster[k];
        Random random = new Random();
        clusters[0] = new Cluster();
        clusters[0].setCenter(points.get(random.nextInt(points.size())));
        points.remove(clusters[0].getCenter());

        for (int i = 1; i < clusters.length; i++) {
            final Map<double[], Double> distances = computeSquaredDistances(
                    points, clusters, i);
            double[] newCenter = sampleNewCenter(points, random, distances);
            clusters[i] = new Cluster();
            clusters[i].setCenter(newCenter);
            points.remove(newCenter);
        }
        return clusters;
    }

    private double[] sampleNewCenter(List<double[]> points, Random random,
            final Map<double[], Double> distances) {
        Collections.sort(points, new Comparator<double[]>() {

            @Override
            public int compare(double[] o1, double[] o2) {
                return Double.compare(distances.get(o1), distances.get(o2));
            }

        });
        double sample = random.nextDouble()
                * MathUtils.sum(Doubles.toArray(distances.values()));
        double[] newCenter = sample(points, distances, sample);
        return newCenter;
    }

    private Map<double[], Double> computeSquaredDistances(
            List<double[]> points, Cluster[] clusters, int validClusters) {
        final Map<double[], Double> distances = new HashMap<double[], Double>();
        assignPoints(points, Arrays.copyOf(clusters, validClusters));
        for (int j = 0; j < validClusters; j++) {
            for (double[] point : clusters[j].getPoints()) {
                distances.put(point, Math.pow(clusters[j].getCenterDistance(point), 2));
            }
            clusters[j].clear();
        }
        return distances;
    }

    private double[] sample(List<double[]> points,
            Map<double[], Double> distances, double sample) {
        double currentProb = 0;
        int resultIndex = 0;
        for (double[] point : points) {
            currentProb += distances.get(point);
            if (sample < currentProb) {
                break;
            }
            resultIndex++;
        }
        resultIndex = Math.min(resultIndex, points.size() - 1);

        return points.get(resultIndex);
    }

    private boolean assignPoints(List<double[]> points, Cluster[] clusters) {
        boolean changed;
        changed = false;
        for (double[] p : points) {
            int srcCluster = -1;
            int targetCluster = -1;
            double minDist = Integer.MAX_VALUE;
            for (int i = 0; i < clusters.length; i++) {
                if (clusters[i].getPoints().contains(p)) {
                    srcCluster = i;
                }
                double dist = clusters[i].getCenterDistance(p);
                if (minDist > dist) {
                    minDist = dist;
                    targetCluster = i;
                }
            }
            if (targetCluster != srcCluster) {
                changed = true;
                Cluster.transfer(srcCluster == -1 ? null : clusters[srcCluster], clusters[targetCluster], p);
            }
        }
        return changed;
    }

}
