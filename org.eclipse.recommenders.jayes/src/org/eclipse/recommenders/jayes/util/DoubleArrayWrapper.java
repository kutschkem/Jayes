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

import java.util.Arrays;

public class DoubleArrayWrapper implements ArrayWrapper {

	private double[] array;
	
	public DoubleArrayWrapper(double[] array){
		this.array = array;
	}
	
	@Override
	public void set(double[] array) {
		this.array = array;

	}

	@Override
	public void set(float[] array) {
		set(ArrayUtils.toDoubleArray(array));

	}

	@Override
	public double[] getDouble() {
		return array;
	}

	@Override
	public float[] getFloat() {
		return ArrayUtils.toFloatArray(array);
	}

	@Override
	public void assign(int index, double d) {
		array[index] = d;
	}

	@Override
	public void assign(int index, float d) {
		assign(index,(double)d);
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
	public void mulAssign(int index, ArrayWrapper arg, int argIndex) {
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
	public void addAssign(int index, ArrayWrapper arg, int argIndex) {
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
	public void copy(double[] array) {
		set(array.clone());
	}

	@Override
	public void copy(float[] array) {
		set(ArrayUtils.toDoubleArray(array));

	}

	@Override
	public void copy(ArrayWrapper array) {
		copy(array.getDouble());
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
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public void arrayCopy(ArrayWrapper src, int srcOffset, int destOffset,
			int length) {
		System.arraycopy(src.getDouble(),0,array,0,length);
		
	}

	@Override
	public void newArray(int capacity) {
		this.array = new double[capacity];
		
	}

	@Override
	public int sizeOfElement() {
		return 8;
	}

}
