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
package org.eclipse.recommenders.eval.jayes;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DataPoint {

    public DataPoint(Map<Integer, Integer> evidence) {
        this.evidence = evidence;
    }

    private Map</*library name*/String, Double> meanSquaredError = new HashMap<String, Double>();
    private Map</*library name*/String, Long> timeSet = new HashMap<String, Long>();
    private Map<String, Long> timeQuery = new HashMap<String, Long>();
    private Map<String, Long> timeUpdate = new HashMap<String, Long>();
    private int maxErrorNode;
    private Map<Integer, Integer> evidence;
    private Object additionalInfos;

    public Set<String> getLibs() {
        return Collections.unmodifiableSet(timeUpdate.keySet());
    }

    public void setUpdateTime(String lib, long time) {
        timeUpdate.put(lib, time);
    }

    public void setQueryTime(String lib, long time) {
        timeQuery.put(lib, time);
    }

    public long getQueryTime(String lib) {
        return timeQuery.get(lib);
    }

    public long getUpdateTime(String lib) {
        return timeUpdate.get(lib);
    }

    public long getTime(String lib) {
        return timeUpdate.get(lib) + timeQuery.get(lib);
    }

    public void setSetupTime(String lib, long time) {
        timeSet.put(lib, time);
    }

    public long getSetupTime(String lib) {
        return timeSet.get(lib);
    }

    public void setMeanSquaredError(String lib, double mse) {
        this.meanSquaredError.put(lib, mse);
    }

    public double getMeanSquaredError(String lib) {
        return meanSquaredError.get(lib);
    }

    public int getMaxErrorNode() {
        return maxErrorNode;
    }

    public void setMaxErrorNode(int maxErrorNode) {
        this.maxErrorNode = maxErrorNode;
    }

    public Object getAdditionalInfos() {
        return additionalInfos;
    }

    public void setAdditionalInfos(Object additionalInfos) {
        this.additionalInfos = additionalInfos;
    }

    public Map<Integer, Integer> getEvidence() {
        return Collections.unmodifiableMap(evidence);
    }

    @Override
    public String toString() {
        StringBuilder bldr = new StringBuilder();
        bldr.append("{ ");
        for (String lib : getLibs()) {
            bldr.append("\"lib\": {");
            bldr.append("\"MSE\": ");
            bldr.append(meanSquaredError.get(lib));
            bldr.append(", \"time\": ");
            bldr.append(getTime(lib) / Math.pow(10, 9));
            bldr.append("}, ");
        }

        return bldr.toString();
    }

}
