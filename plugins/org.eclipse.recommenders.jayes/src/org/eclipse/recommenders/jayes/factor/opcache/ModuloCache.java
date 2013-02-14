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
 * This class is to be used when modulo operations are performance critical, and appear in sequence such that many
 * subsequent values would result in the same value, if divided by the divisor.
 */
public class ModuloCache implements IOperationCache {

    private int divisor;
    private DivisionCache subCache;
    int cachedSubtrahent;

    public ModuloCache(int divisor) {
        this.divisor = divisor;
        this.subCache = new DivisionCache(divisor);
        cachedSubtrahent = 0;
    }

    @Override
    public int apply(int arg) {
        if (subCache.isInCache(arg)) {
            return arg - cachedSubtrahent;
        }
        cachedSubtrahent = divisor * subCache.apply(arg);
        return arg - cachedSubtrahent;
    }

}
