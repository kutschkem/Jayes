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
package org.eclipse.recommenders.jayes.util.arraywrapper;

public interface IArrayWrapper extends Cloneable, Iterable<Number>{
	
	public void setArray(double... array);
	public void setArray(float... array);
	public double[] toDoubleArray();
	public float[] toFloatArray();
	public void set(int index, double d);
	public void set(int index, float d);
	public void mulAssign(int index, double d);
	public void mulAssign(int index, float d);
	public void mulAssign(int index, IArrayWrapper arg, int argIndex);
	public void addAssign(int index, double d);
	public void addAssign(int index, float d);
	public void addAssign(int index, IArrayWrapper arg, int argIndex);
	public double getDouble(int index);
	public float getFloat(int index);
	public int length();

	public void copy(double... array);

	public void copy(float... array);
	public void copy(IArrayWrapper array);
	public void fill(double d);
	public void fill(float d);
	public void arrayCopy(IArrayWrapper src, int srcOffset, int destOffset, int length);
	public IArrayWrapper clone();
	public void newArray(int capacity);
	/**
	 * @return size of a single array element in bytes
	 */
	public int sizeOfElement();
}
