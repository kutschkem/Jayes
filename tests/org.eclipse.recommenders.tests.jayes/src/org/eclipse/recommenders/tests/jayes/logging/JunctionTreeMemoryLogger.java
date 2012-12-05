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
package org.eclipse.recommenders.tests.jayes.logging;

import java.util.HashSet;
import java.util.IdentityHashMap;

import org.eclipse.recommenders.jayes.factor.AbstractFactor;
import org.eclipse.recommenders.jayes.util.MathUtils;
import org.eclipse.recommenders.jayes.util.Pair;
import org.eclipse.recommenders.jayes.util.arraywrapper.IArrayWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JunctionTreeMemoryLogger {
	
	private JTATestAdapter jta;
	private Logger logger = LoggerFactory.getLogger("org.eclipse.recommenders.jayes");

	public JunctionTreeMemoryLogger(JTATestAdapter jta){
		this.jta = jta;
	}
	
	public void logMemorySavingsFromFlyweights() {
		this.logMemorySavingsFromFlyweightPatternForInitalizations();
		this.logMemorySavingsFromFlyweightPatternForPreparedOperations();
	}
	
	private void logMemorySavingsFromFlyweightPatternForInitalizations() {
		IdentityHashMap<IArrayWrapper,IArrayWrapper> instances = new IdentityHashMap<IArrayWrapper, IArrayWrapper>();
		int factorSizes = 0;
		for (final AbstractFactor pot : jta.getNodePotentials()) {
			factorSizes += pot.getValues().length();
			instances.put(pot.getValues(), pot.getValues());
		}

		for (final AbstractFactor sep : jta.getSepsets().values()) {
			factorSizes += sep.getValues().length();
			instances.put(sep.getValues(),sep.getValues());
		}

		int flyweightsize = 0;
		for (IArrayWrapper d : instances.keySet()) {
			flyweightsize += d.length();
		}

		logger.info("initializations, original size: " + factorSizes);
		logger.info("initializations, flyweight size: " + flyweightsize);
		logger.info("ratio: " + ((double) flyweightsize / factorSizes));
	}

	private void logMemorySavingsFromFlyweightPatternForPreparedOperations() {
		IdentityHashMap<int[],int[]> instances = new IdentityHashMap<int[], int[]>();
		int factorSizes = 0;
		for (int[] intArr : jta.getPreparedMultiplications().values()) {
			factorSizes += intArr.length;
			instances.put(intArr,intArr);
		}

		for (int[] prep : jta.getPreparedQueries()) {
			factorSizes += prep.length;
			instances.put(prep, prep);
		}

		int flyweightsize = 0;
		for (int[] d : instances.keySet()) {
			flyweightsize += d.length;
		}

		logger.info("prepared ops, original size: " + factorSizes);
		logger.info("prepared ops, flyweight size: " + flyweightsize);
		logger.info("prepared ops, ratio flyw./orig.: " + ((double) flyweightsize / factorSizes));
	}
	
	public void logSparsenessInfo() {
		int denseLength = 0;
		int sparseLength = 0;
		for (AbstractFactor f : jta.getNodePotentials()) {
			denseLength += MathUtils.product(f.getDimensions());//the length that a dense factor would have
			sparseLength += f.getValues().length() + f.getOverhead();
		}
		
		logger.info("dense factor size: " + denseLength);
		logger.info("sparse factor size: " + sparseLength);
		logger.info("ratio: " + ((double) sparseLength / denseLength));
	}

	
	public void logCompleteMemoryInfo() {
		long size = estimateMemoryConsumption();

		logger.info("approx. total size of the model, in bytes: "
				+ size);
	}

	private long estimateMemoryConsumption() {
		long size = 0; // size in bytes
		for (AbstractFactor f : jta.getNodePotentials()) {
			size += f.getValues().length() * f.getValues().sizeOfElement();
			size += f.getDimensions().length * 4;
			size += f.getDimensionIDs().length * 4;
			size += f.getOverhead();
		}
		for (AbstractFactor f : jta.getSepsets().values()) {
			size += f.getValues().length() * f.getValues().sizeOfElement();
			size += f.getDimensions().length * 4;
			size += f.getDimensionIDs().length * 4;
		}
		HashSet<int[]> check = new HashSet<int[]>();
		for (int[] p : jta.getPreparedMultiplications().values()) {
			if (!check.contains(p)) {
				check.add(p);
				size += p.length * 4;
			}
		}
		for (int[] p : jta.getPreparedQueries()) {
			if (!check.contains(p)) {
				check.add(p);
				size += p.length * 4;
			}
		}
		HashSet<IArrayWrapper> check2 = new HashSet<IArrayWrapper>();
		for (Pair<AbstractFactor, IArrayWrapper> p : jta.getInitializations()) {
			if (!check2.contains(p.getSecond())) {
				check2.add(p.getSecond());
				size += p.getSecond().length() * p.getSecond().sizeOfElement();
			}
		}
		return size;
	}
	
}
