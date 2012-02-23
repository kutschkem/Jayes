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

public class DoubleArrayFlyWeight {

    private DeepDoubleArrayHashSet registered = new DeepDoubleArrayHashSet();

    public double[] getInstance(double[] doubleArr) {
        if (registered.contains(doubleArr)) {
            return registered.get(doubleArr);
        }
        registered.add(doubleArr);
        return doubleArr;
    }

    public int size() {
        return registered.size();
    }

}
