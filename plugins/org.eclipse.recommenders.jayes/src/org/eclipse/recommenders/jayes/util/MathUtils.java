/**
 * Copyright (c) 2011 Michael Kutschke.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Michael Kutschke - initial API and implementation.
 */
package org.eclipse.recommenders.jayes.util;

import org.eclipse.recommenders.jayes.factor.arraywrapper.IArrayWrapper;

public class MathUtils {

    public static void exp(double[] vector) {
        for (int i = 0; i < vector.length; i++) {
            vector[i] = Math.exp(vector[i]);
        }
    }
	
	public static void exp(IArrayWrapper array){
		for(int i = 0; i < array.length(); i++){
			array.set(i, Math.exp(array.getDouble(i)));
		}
	}

    public static void log(double[] vector) {
        for (int i = 0; i < vector.length; i++) {
            vector[i] = Math.log(vector[i]);
        }
    }
	
    public static void log(IArrayWrapper vector) {
        for (int i = 0; i < vector.length(); i++) {
            vector.set(i, Math.log(vector.getDouble(i)));
        }
    }

    public static double[] normalize(double[] vector) {
        double normFactor = MathUtils.sum(vector);
        if (normFactor == 0) {
            throw new IllegalArgumentException("Cannot normalize a zero-Vector!");
        }
        for (int i = 0; i < vector.length; i++) {
            vector[i] /= normFactor;
        }
        return vector;
    }

    public static double sum(double[] vector) {
        double result = 0;
        for (double d : vector) {
            result += d;
        }
        return result;
    }

    /**
     * computes c := a / b , but avoids division by zero. In the context of the
     * JTA, this is valid because it holds that b == 0 -> a == 0
     * 
     * @param a
     * @param b
     * @param c
     */
    public static void secureDivide(double[] a, double[] b, double[] c) {
        for (int i = 0; i < a.length; i++) {
            if (b[i] != 0) {
                c[i] = a[i] / b[i];
            }
        }
    }

    public static void secureSubtract(double[] a, double[] b, double[] c) {
        for (int i = 0; i < a.length; i++) {
            if (b[i] != Double.NEGATIVE_INFINITY) {
                c[i] = a[i] - b[i];
            }
        }
    }

    public static double[] normalizeLog(double[] vector) {
        double normFactor = MathUtils.logsumexp(vector);
        if (normFactor == Double.NEGATIVE_INFINITY) {
            throw new IllegalArgumentException("Cannot normalize a zero-Vector!");
        }
        for (int i = 0; i < vector.length; i++) {
            vector[i] -= normFactor;
        }
        return vector;
    }

    private static double logsumexp(double[] vector) {
        double max = absMax(vector);
        double sum = 0;
        for (double d : vector) {
            sum += Math.exp(d - max);
        }
        sum = max + Math.log(sum);
        return sum;
    }

    /**
     * 
     * @return the value that differs most from zero
     */
    private static double absMax(double[] vector) {
        double result = 0;
        for (double d : vector) {
            if (d != Double.NEGATIVE_INFINITY && Math.abs(d) > Math.abs(result)) {
                result = d;
            }
        }
        return result;
    }

    public static int product(int[] vector) {
        return productOfRange(vector,0,vector.length);
    }
    
    /**
     * product of the values in the array, from index start (inclusive) to end (exclusive)
     * @throws ArrayIndexOutOfBoundsException if end >= vector.length
     */
    public static int productOfRange(int[] vector, int start, int end){
        int result = 1;
        for(int i = start; i < end; i++){
            result *= vector[i];
        }
        return result;
    }
    
    public static int scalarProduct(int[] v1, int[] v2){
    	if(v1.length != v2.length){
    		throw new IllegalArgumentException("vectors differ in length");
    	}
    	int result = 0;
    	for(int i = 0; i < v1.length; i++){
    		result += v1[i] * v2[i];
    	}
    	return result;
    }

}
