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

public final class AddressCalc {

    private AddressCalc() {

    }

    public static int realAddr(int[] dimensions, int[] address) {
        int result = address[0];
        for (int i = 1; i < dimensions.length; i++) {
            result *= dimensions[i];
            result += address[i];
        }
        return result;
    }

    public static int[] virtualAddr(int[] dimensions, int address) {
        int[] result = new int[dimensions.length];
        for (int i = dimensions.length - 1; i >= 0; i--) {
            result[i] = address % dimensions[i];
            address /= dimensions[i];
        }
        return result;
    }

    public static void incrementMultiDimensionalCounter(final int[] counter, final int[] dimensions, final int dim) {
        counter[dim]++;
        if (counter[dim] == dimensions[dim]) {
            // overflow, assume the counter was valid before and less than the
            // maximal counter
            counter[dim] = 0;
            incrementMultiDimensionalCounter(counter, dimensions, dim - 1);
        }
    }

}
