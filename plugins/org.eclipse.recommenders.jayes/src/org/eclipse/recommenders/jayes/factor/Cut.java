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
package org.eclipse.recommenders.jayes.factor;

import org.eclipse.recommenders.jayes.util.MathUtils;

/**
 * represents a cut through a decision tree as described in
 * "Static and dynamic speedup techniques for the Junction Tree Algorithm"
 * (Kutschke, 2011) (my Bachelor's thesis). Serves for exactly selecting those
 * entries in the probability matrix of a factor that are relevant. It does so
 * in linear time and space, to the number of dimensions of a factor.
 * 
 * @author Michael Kutschke
 * 
 */
public class Cut implements Cloneable {

    private final AbstractFactor factor;
    private int start;
    private int stepSize;
    private int length;

    private int subtreeStepsize;

    private int rootDimension;
    private int leafDimension;

    // the subtree(s); only one because of the inherent regularities of the
    // decision tree
    private Cut subCut;

    public Cut(AbstractFactor factor) {
        this.factor = factor;
    }

    public void initialize() {
        length = MathUtils.product(this.factor.getDimensions());
        start = 0;
        stepSize = 1;
        rootDimension = 0;
        if(length > 1){
        subtreeStepsize = length / this.factor.getDimensions()[0];
        }else{ //treat zero-dimensional factors specially
        	subtreeStepsize = 0;
        }
        leafDimension = this.factor.getDimensions().length - 1;
        subCut = null;
        leafCut();
        rootCut();
        createSubcut();
    }

    @Override
    public Cut clone() {
        try {
            return (Cut) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void rootCut() {
        while (rootDimension < leafDimension && this.factor.selections[rootDimension] != -1) {
            descendSelectedDimension();
        }
        while (rootDimension < leafDimension && this.factor.selections[rootDimension + 1] == -1) {
            descendUnselectedDimension();
        }
    }

    private void descendSelectedDimension() {
        length /= this.factor.dimensions[rootDimension];
        start += subtreeStepsize * this.factor.selections[rootDimension];
        descendUnselectedDimension();
    }

    private void descendUnselectedDimension() {
        rootDimension++;
        subtreeStepsize /= this.factor.dimensions[rootDimension];
    }

    private void leafCut() {
        while (leafDimension >= 0 && this.factor.selections[leafDimension] != -1) {
            ascendSelectedDimension();
        }
    }

    private void ascendSelectedDimension() {
        start += this.factor.selections[leafDimension] * stepSize;
        stepSize *= this.factor.dimensions[leafDimension];
        leafDimension--;
    }

    private void createSubcut() {
        if (needsSplit()) {
            subCut = null; // avoid circularity in object graph
            subCut = clone();
            subCut.descendUnselectedDimension();
            subCut.length = subtreeStepsize;
            subCut.rootCut(); // no leaf cut
            subCut.createSubcut();
        }
    }

    /**
     * the Cut needs to further split if and only if there is an additional
     * selection between root and leaf
     * 
     * @return
     */
    private boolean needsSplit() {
        if (length < subtreeStepsize)
            return false;
        for (int i = rootDimension; i < leafDimension; i++)
            if (this.factor.selections[i] != -1)
                return true;
        return false;
    }

    public int getStart() {
        return start;
    }
    
    public int getEnd(){
        return start + length;
    }

    public int getStepSize() {
        return stepSize;
    }

    public int getLength() {
        return length;
    }

    public int getSubtreeStepsize() {
        return subtreeStepsize;
    }

    public Cut getSubCut() {
        return subCut;
    }

}
