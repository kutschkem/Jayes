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

public interface IArrayWrapper extends Cloneable, Iterable<Number>{
	
	void setArray(double... array);
	void setArray(float... array);
	double[] toDoubleArray();
	float[] toFloatArray();
	void set(int index, double d);
	void set(int index, float d);
	void mulAssign(int index, double d);
	void mulAssign(int index, float d);
	void mulAssign(int index, IArrayWrapper arg, int argIndex);
	void addAssign(int index, double d);
	void addAssign(int index, float d);
	void addAssign(int index, IArrayWrapper arg, int argIndex);
	double getDouble(int index);
	float getFloat(int index);
	int length();

	void copy(double... array);

	void copy(float... array);
	void copy(IArrayWrapper array);
	void fill(double d);
	void fill(float d);
	void arrayCopy(IArrayWrapper src, int srcOffset, int destOffset, int length);
	IArrayWrapper clone();
	void newArray(int capacity);
	/**
	 * @return size of a single array element in bytes
	 */
	int sizeOfElement();
}
