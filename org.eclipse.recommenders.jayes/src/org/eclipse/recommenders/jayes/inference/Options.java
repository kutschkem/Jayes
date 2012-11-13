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
