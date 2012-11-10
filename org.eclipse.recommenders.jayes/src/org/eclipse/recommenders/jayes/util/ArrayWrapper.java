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

public interface ArrayWrapper extends Cloneable {
	
	public void set(double[] array);
	public void set(float[] array);
	public double[] getDouble();
	public float[] getFloat();
	public void assign(int index, double d);
	public void assign(int index, float d);
	public void mulAssign(int index, double d);
	public void mulAssign(int index, float d);
	public void mulAssign(int index, ArrayWrapper arg, int argIndex);
	public void addAssign(int index, double d);
	public void addAssign(int index, float d);
	public void addAssign(int index, ArrayWrapper arg, int argIndex);
	public double getDouble(int index);
	public float getFloat(int index);
	public int length();
	public void copy(double[] array);
	public void copy(float[] array);
	public void copy(ArrayWrapper array);
	public void fill(double d);
	public void fill(float d);
	public void arrayCopy(ArrayWrapper src, int srcOffset, int destOffset, int length);
	public ArrayWrapper clone();
	public void newArray(int capacity);
	/**
	 * @return size of a single array element in bytes
	 */
	public int sizeOfElement();
}
