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
package org.eclipse.recommenders.eval.jayes.statistics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.recommenders.eval.jayes.DataPoint;
import org.eclipse.recommenders.eval.jayes.kmeans.Cluster;
import org.eclipse.recommenders.eval.jayes.kmeans.KMeans;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataPointStatistics {

    private Iterable<String> libraries;
    private Logger logger = LoggerFactory.getLogger(getClass());

    public DataPointStatistics(Iterable<String> libraries) {
        this.libraries = libraries;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Number> computeStatistics(List<DataPoint> points) {
        DataPoint means = computeMeans(points);

        DataPoint variance = computeVariance(points, means);
        Map<String, Long> totalVariances = (Map<String, Long>) variance.getAdditionalInfos();

        Map<String, Number> stat = new TreeMap<String, Number>();

        for (String lib : libraries) {
            stat.put("mean total time (" + lib + ")", means.getTime(lib) / Math.pow(10, 6));
            stat.put("total time standard deviation (" + lib + ")",
                    Math.sqrt(totalVariances.get(lib)) / Math.pow(10, 3));
            stat.put("mean time standard deviation (" + lib + ")",
                    Math.sqrt(totalVariances.get(lib) / (double) points.size()) / Math.pow(10, 3));
            stat.put("mean query time (" + lib + ")", means.getQueryTime(lib) / Math.pow(10, 6));
            stat.put("mean setup time (" + lib + ")", means.getSetupTime(lib) / Math.pow(10, 6));
            stat.put("max squared error (" + lib + ")", means.getMeanSquaredError(lib));
        }

        clustering(points, stat);

        return stat;
    }

    private DataPoint computeMeans(List<DataPoint> points) {
        DataPoint meta = new DataPoint(null);
        for (String lib : libraries) {
            meta.setQueryTime(lib, 0L);
            meta.setUpdateTime(lib, 0L);
            meta.setSetupTime(lib, 0L);
            meta.setMeanSquaredError(lib, 0.0);
        }
        for (DataPoint p : points) {

            for (String lib : libraries) {
                meta.setQueryTime(lib, meta.getQueryTime(lib) + p.getQueryTime(lib));
                meta.setUpdateTime(lib, meta.getUpdateTime(lib) + p.getUpdateTime(lib));
                meta.setSetupTime(lib, meta.getSetupTime(lib) + p.getSetupTime(lib));
                if (p.getMeanSquaredError(lib) >= meta.getMeanSquaredError(lib)) {
                    meta.setMeanSquaredError(lib, p.getMeanSquaredError(lib));
                }
            }
        }
        for (String lib : libraries) {
            meta.setQueryTime(lib, meta.getQueryTime(lib) / points.size());
            meta.setUpdateTime(lib, meta.getUpdateTime(lib) / points.size());
            meta.setSetupTime(lib, meta.getSetupTime(lib) / points.size());
        }
        return meta;
    }

    private DataPoint computeVariance(List<DataPoint> points, DataPoint means) {
        DataPoint variance = new DataPoint(null);
        Map<String, Long> totalVariance = new HashMap<String, Long>();

        for (String lib : libraries) {
            variance.setQueryTime(lib, 0L);
            variance.setSetupTime(lib, 0L);
            variance.setUpdateTime(lib, 0L);
            totalVariance.put(lib, 0L);
            for (DataPoint point : points) {
                variance.setQueryTime(
                        lib,
                        (long) (variance.getQueryTime(lib) + Math.pow(
                                (point.getQueryTime(lib) - means.getQueryTime(lib)), 2.0)));
                variance.setUpdateTime(
                        lib,
                        (long) (variance.getUpdateTime(lib) + Math.pow(
                                (point.getUpdateTime(lib) - means.getUpdateTime(lib)), 2.0)));
                variance.setSetupTime(
                        lib,
                        (long) (variance.getSetupTime(lib) + Math.pow(
                                (point.getSetupTime(lib) - means.getSetupTime(lib)), 2.0)));
                totalVariance.put(lib,
                        (long) (totalVariance.get(lib) + Math.pow(point.getTime(lib) / Math.pow(10, 3), 2.0)));
            }
            variance.setQueryTime(lib, variance.getQueryTime(lib) / points.size());
            variance.setUpdateTime(lib, variance.getUpdateTime(lib) / points.size());
            variance.setSetupTime(lib, variance.getSetupTime(lib) / points.size());
            totalVariance
                    .put(lib,
                            (long) (totalVariance.get(lib) / points.size() - Math.pow(
                                    means.getTime(lib) / Math.pow(10, 3), 2)));
        }

        variance.setAdditionalInfos(totalVariance);

        // the variance of a sum of uncorrelated variables is the sum of the
        // variances; assume uncorrelatedness
        return variance;
    }

    public void clustering(List<DataPoint> points, Map<String, Number> stat) {
        logger.info("-- K-means");

        for (String lib : libraries) {
            List<Cluster> clusters = performKmeans(2, points, lib);
            stat.put("cluster 1 center (" + lib + ")", clusters.get(0).getCenter()[0]);
            stat.put("cluster 2 center (" + lib + ")", clusters.get(1).getCenter()[0]);
            stat.put("cluster distance (" + lib + ")", clusters.get(0).getCenterDistance(clusters.get(1).getCenter()));
            stat.put("cluster 1 size (" + lib + ")", clusters.get(0).getPoints().size());
            stat.put("cluster 2 size (" + lib + ")", clusters.get(1).getPoints().size());
        }
    }

    private List<Cluster> performKmeans(int k, Iterable<DataPoint> points, String lib) {
        List<double[]> dPoints = new ArrayList<double[]>();
        for (DataPoint p : points) {
            dPoints.add(new double[] { p.getTime(lib) / Math.pow(10, 6) });
        }

        KMeans kmeans = new KMeans();
        List<Cluster> cluster = kmeans.kMeans(k, dPoints);
        return cluster;

    }

}
