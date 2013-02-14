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
package org.eclipse.recommenders.jayes.factor.opcache;

/**
 * This class is to be used when divisions are a performance-critical part of the code, and the sequence of divisions is
 * such that many subsequent divisions will have the same result (using the same divisor).
 */
public class DivisionCache implements IOperationCache {

    private int divisor;
    private int cachedBlockStart;// the smallest value for which the cached result is valid
    private int cachedResult;

    public DivisionCache(int divisor) {
        this.divisor = divisor;
        cachedBlockStart = 0;
        cachedResult = 0;
    }

    @Override
    public int apply(int arg) {
        if (divisor == 1) {
            return arg;
        }
        if (isInCache(arg)) {
            return cachedResult;
        }
        cachedResult = arg / divisor;
        cachedBlockStart = cachedResult * divisor;
        return cachedResult;
    }

    public boolean isInCache(int arg) {
        return arg >= cachedBlockStart && arg - cachedBlockStart < divisor;
    }

}
