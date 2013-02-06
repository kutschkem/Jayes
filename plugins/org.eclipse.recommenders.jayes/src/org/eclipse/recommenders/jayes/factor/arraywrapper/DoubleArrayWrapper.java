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
package org.eclipse.recommenders.jayes.factor.arraywrapper;

import java.util.Arrays;
import java.util.Iterator;

import org.eclipse.recommenders.internal.jayes.util.ArrayUtils;

public class DoubleArrayWrapper implements IArrayWrapper {

	private double[] array;
	
	public DoubleArrayWrapper(double... array) {
		this.array = array;
	}
	
	@Override
	public void setArray(double... array) {
		this.array = array;

	}

	@Override
	public void setArray(float... array) {
		setArray(ArrayUtils.toDoubleArray(array));

	}

	@Override
	public double[] toDoubleArray() {
		return array;
	}

	@Override
	public float[] toFloatArray() {
		return ArrayUtils.toFloatArray(array);
	}

	@Override
	public void set(int index, double d) {
		array[index] = d;
	}

	@Override
	public void set(int index, float d) {
		set(index,(double)d);
	}

	@Override
	public void mulAssign(int index, double d) {
		array[index] *= d;
	}

	@Override
	public void mulAssign(int index, float d) {
		array[index] *= d;
	}

	@Override
	public void mulAssign(int index, IArrayWrapper arg, int argIndex) {
		array[index] *= arg.getDouble(argIndex);
	}

	@Override
	public void addAssign(int index, double d) {
		array[index] += d;
	}

	@Override
	public void addAssign(int index, float d) {
		array[index] += d;
	}

	@Override
	public void addAssign(int index, IArrayWrapper arg, int argIndex) {
		array[index] += arg.getDouble(argIndex);
	}

	@Override
	public double getDouble(int index) {
		return array[index];
	}

	@Override
	public float getFloat(int index) {
		return (float) array[index];
	}

	@Override
	public int length() {
		return array.length;
	}

	@Override
	public void copy(double... array) {
		setArray(array.clone());
	}

	@Override
	public void copy(float... array) {
		setArray(ArrayUtils.toDoubleArray(array));

	}

	@Override
	public void copy(IArrayWrapper array) {
		copy(array.toDoubleArray());
	}

	@Override
	public void fill(double d) {
		Arrays.fill(array, d);
	}

	@Override
	public void fill(float d) {
		Arrays.fill(array, (double)d);
	}

	public DoubleArrayWrapper clone(){
		try {
			DoubleArrayWrapper result = (DoubleArrayWrapper) super.clone();
			result.array = array.clone();
			return result;
		} catch (CloneNotSupportedException e) {
			throw new AssertionError("This should not happen");
		}
	}

	@Override
	public void arrayCopy(IArrayWrapper src, int srcOffset, int destOffset,
			int length) {
		System.arraycopy(src.toDoubleArray(),srcOffset,array,destOffset,length);
		
	}

	@Override
	public void newArray(int capacity) {
		this.array = new double[capacity];
		
	}

	@Override
	public int sizeOfElement() {
		return 8;
	}

	@Override
	public Iterator<Number> iterator() {
		return new Iterator<Number>(){
			
			private int index = 0;

			@Override
			public boolean hasNext() {
				return index < array.length;
			}

			@Override
			public Number next() {
				index++;
				return array[index-1];
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
			
		};
	}

}
