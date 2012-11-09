package org.eclipse.recommenders.jayes.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ArrayWrapperFlyWeight implements Iterable<ArrayWrapper> {
	
	
	private Map<ArrayWrapper,ArrayWrapper> flyweight = new HashMap<ArrayWrapper,ArrayWrapper>();

	public ArrayWrapper getInstance(ArrayWrapper array){
		if(! flyweight.containsKey(array)){
			flyweight.put(array, array);
		}
		return flyweight.get(array);
	}
	
    @Override
    public Iterator<ArrayWrapper> iterator() {
        return flyweight.keySet().iterator();
    }
	
}
