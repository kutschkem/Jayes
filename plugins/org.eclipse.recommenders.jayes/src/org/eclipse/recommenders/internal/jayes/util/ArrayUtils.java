/**
 * Copyright (c) 2011 Michael Kutschke. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * Michael Kutschke - initial API and implementation.
 */
package org.eclipse.recommenders.internal.jayes.util;

import java.lang.reflect.Array;
import java.util.List;

public final class ArrayUtils {

    private ArrayUtils() {

    }

    public static <T extends Number> Object unboxArray(final T[] array) {
        final Class<?> primitiveClass = getPrimitiveClass(array.getClass().getComponentType());
        final Object arr = Array.newInstance(primitiveClass, array.length);
        for (int i = 0; i < array.length; i++) {
            Array.set(arr, i, array[i]);
        }
        return arr;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Number> T[] boxArray(final Object primitiveArray) {
        if (!primitiveArray.getClass().isArray()) {
            throw new IllegalArgumentException("not an array");
        }
        final Class<? extends T> primitiveClass = (Class<? extends T>) getWrapperClass(primitiveArray.getClass()
                .getComponentType());
        final Object arr = Array.newInstance(primitiveClass,
                Array.getLength(primitiveArray));
        for (int i = 0; i < Array.getLength(primitiveArray); i++) {
            Array.set(arr, i, Array.get(primitiveArray, i));
        }
        return (T[]) arr;
    }

    private static Class<?> getWrapperClass(final Class<?> componentType) {
        if (int.class.isAssignableFrom(componentType)) {
            return Integer.class;
        }
        if (double.class.isAssignableFrom(componentType)) {
            return Double.class;
        }
        throw new UnsupportedOperationException("Mapping not implemented");
    }

    private static Class<?> getPrimitiveClass(final Class<?> componentType) {
        if (Integer.class.isAssignableFrom(componentType)) {
            return int.class;
        }
        if (Double.class.isAssignableFrom(componentType)) {
            return double.class;
        }
        throw new UnsupportedOperationException("Mapping not implemented");
    }

    public static float[] toFloatArray(double[] array) {
        float[] result = new float[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = (float) array[i];
        }
        return result;
    }

    public static double[] toDoubleArray(float[] array) {
        double[] result = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i];
        }
        return result;
    }

    public static int[] toIntArray(List<? extends Number> ints) {
        int[] result = new int[ints.size()];
        int i = 0;
        for (Number j : ints) {
            result[i] = j.intValue();
            i++;
        }
        return result;
    }

}
