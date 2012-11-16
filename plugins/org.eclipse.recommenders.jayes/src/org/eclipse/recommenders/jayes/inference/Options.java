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
package org.eclipse.recommenders.jayes.inference;

/**
 * Options represent hints to or flags for the algorithms, potentially
 * used for optimizations. Be aware though that not all algorithms will/can benefit
 * from these hints.
 * @author michael
 *
 */
public class Options {

	private boolean areFloatsAllowed = false;

	public boolean areFloatsAllowed() {
		return areFloatsAllowed;
	}

	public void allowFloats(boolean areFloatsAllowed) {
		this.areFloatsAllowed = areFloatsAllowed;
	}
	
	
}
