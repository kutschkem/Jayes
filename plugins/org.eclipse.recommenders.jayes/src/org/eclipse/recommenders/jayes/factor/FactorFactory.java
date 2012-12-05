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
package org.eclipse.recommenders.jayes.factor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.recommenders.jayes.BayesNet;
import org.eclipse.recommenders.jayes.util.ArrayUtils;
import org.eclipse.recommenders.jayes.util.MathUtils;
import org.eclipse.recommenders.jayes.util.arraywrapper.DoubleArrayWrapper;
import org.eclipse.recommenders.jayes.util.arraywrapper.IArrayWrapper;

public class FactorFactory {
	
	protected BayesNet net;
	private int logThreshold = Integer.MAX_VALUE;
	private IArrayWrapper prototype = new DoubleArrayWrapper(new double[1]);
	
	public void setArrayType(IArrayWrapper prototype){
		IArrayWrapper clone = prototype.clone();
		clone.newArray(1); 
		this.prototype = clone;
	}
	
	public void setReferenceNetwork(BayesNet net){
		this.net = net;
	}
	
	/**
	 * factors bigger (in the number of variables) than the threshold are computed on the log-scale.
	 * Validate different values if you encounter numerical instabilities.
	 * 
	 * @param logThreshold
	 */
	public void setLogThreshold(int threshold){
		this.logThreshold = threshold;
	}
	
	/**
	 * creates a factor, the class of which is dependent on different criteria defined in
	 * the concrete subclasses. The default behavior is to return a DenseFactor.
	 * @param vars
	 * @param multiplicationPartners
	 * @return
	 */
	public AbstractFactor create(List<Integer> vars, List<AbstractFactor> multiplicationPartners){
		
		final int[] dimensions = getDimensionSizes(vars);

		if(SparseFactor.isSuitable(MathUtils.product(dimensions), multiplicationPartners)){
			SparseFactor f = new SparseFactor();
			initializeFactor(vars, dimensions, f);
			f.sparsify(multiplicationPartners);
			return f;
		}else{
			DenseFactor f2 = new DenseFactor();
			initializeFactor(vars,dimensions,f2);
			return f2;
		}
	}

	private void initializeFactor(List<Integer> vars,
			final int[] dimensions, AbstractFactor f) {
		f.setValues(prototype.clone());
		f.setDimensions(dimensions);
		f.setDimensionIDs(ArrayUtils.toIntArray(vars));
		if (vars.size() > getLogThreshold()) {
			f.setLogScale(true);
		}
	}

	private int[] getDimensionSizes(List<Integer> vars) {
		final List<Integer> dimensions = new ArrayList<Integer>();
		for (final Integer dim : vars) {
			dimensions.add(net.getNode(dim).getOutcomeCount());
		}
		return ArrayUtils.toIntArray(dimensions);
	}

	private int getLogThreshold() {
		return logThreshold;
	}
	
	public static FactorFactory defaultFactory(){
		return new FactorFactory();
	}

}
