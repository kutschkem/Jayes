/*******************************************************************************
 * Copyright (c) 2012 Michael Kutschke.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Michael Kutschke - initial API and implementation
 ******************************************************************************/
package org.eclipse.recommenders.tests.jayes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import org.eclipse.recommenders.jayes.factor.arraywrapper.DoubleArrayWrapper;
import org.eclipse.recommenders.jayes.util.sharing.CanonicalArrayWrapperManager;
import org.eclipse.recommenders.jayes.util.sharing.CanonicalIntArraySet;
import org.junit.Test;

public class ObjectSharingTest {
	
	@Test
	public void testCanonicalIntArraySet(){
		int[] a1 = new int[]{1,2,3};
		int[] a2 = new int[]{1,2,3};
		
		assumeTrue(a1 != a2);
		
		int[] a3 = new int[]{2,3,4};
		
		CanonicalIntArraySet sut = new CanonicalIntArraySet();
		
		sut.add(a1);
		sut.add(a2);
		sut.add(a3);
		
		assertSame(sut.get(a1),sut.get(a2));
		assertTrue(sut.get(a1) == a1);
		assertNotSame(sut.get(a1), sut.get(a3));
		
		assertEquals(sut.size(),2);
		sut.remove(a1);
		
		assertEquals(sut.size(),1);
		
	}
	
	@Test
	public void testCanonicalArrayWrapperManager(){
		DoubleArrayWrapper a1 = new DoubleArrayWrapper(1, 2, 3);
		DoubleArrayWrapper a2 = new DoubleArrayWrapper(1, 2, 3);
		
		assumeTrue(a1 != a2);
		
		DoubleArrayWrapper a3 = new DoubleArrayWrapper(2, 3, 4);
		
		CanonicalArrayWrapperManager sut = new CanonicalArrayWrapperManager();
				
		assertSame(sut.getInstance(a1),sut.getInstance(a2));
		assertTrue(sut.getInstance(a1) == a1);
		assertNotSame(sut.getInstance(a1), sut.getInstance(a3));
		
	}

}
