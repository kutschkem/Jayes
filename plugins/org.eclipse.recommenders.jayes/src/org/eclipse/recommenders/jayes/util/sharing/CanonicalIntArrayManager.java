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
package org.eclipse.recommenders.jayes.util.sharing;

public class CanonicalIntArrayManager {

    private CanonicalIntArraySet registered = new CanonicalIntArraySet();

    public int[] getInstance(int[] intArr) {
        if (registered.contains(intArr)) {
            return registered.get(intArr);
        }
        registered.add(intArr);
        return intArr;
    }

}
