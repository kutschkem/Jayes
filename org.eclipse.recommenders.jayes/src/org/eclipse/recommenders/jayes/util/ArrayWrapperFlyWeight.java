/**
 * Copyright (c) 2012 Michael Kutschke.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Michael Kutschke - initial API and implementation.
 */
package org.eclipse.recommenders.jayes.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ArrayWrapperFlyWeight implements Iterable<IArrayWrapper> {
	
	
	private Map<IArrayWrapper,IArrayWrapper> flyweight = new HashMap<IArrayWrapper,IArrayWrapper>();

	public IArrayWrapper getInstance(IArrayWrapper array){
		if(! flyweight.containsKey(array)){
			flyweight.put(array, array);
		}
		return flyweight.get(array);
	}
	
    @Override
    public Iterator<IArrayWrapper> iterator() {
        return flyweight.keySet().iterator();
    }
	
}
