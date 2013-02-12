/*******************************************************************************
 * Copyright (c) 2013 Michael Kutschke.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Michael Kutschke - initial API and implementation
 ******************************************************************************/
package org.eclipse.recommenders.tests.jayes;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItems;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.recommenders.jayes.util.Graph;
import org.eclipse.recommenders.jayes.util.triangulation.MinFillIn;
import org.junit.Test;

public class MinFillInTest {

    /**
     * <pre>
     * 1-3
     * |/
     * 0--2
     * 
     * where 3 has higher weight just for predictability
     * </pre>
     */
    @Test
    public void testMinFillIn() {
        Graph graph = new Graph();
        graph.initialize(4);
        graph.addEdge(0, 1);
        graph.addEdge(1, 3);
        graph.addEdge(3, 0);
        graph.addEdge(0, 2);

        double[] weights = new double[] {
                0d, 0d, 1d, 0d
        };

        MinFillIn minFillIn = new MinFillIn(graph, weights);
        Iterator<List<Integer>> it = minFillIn.iterator();
        List<Integer> first = it.next();
        //eliminate 1
        assertThat(first, hasItems(0, 1, 3));
        assertThat(first.size(), is(3));
        List<Integer> second = it.next();
        //eliminate 3
        assertThat(second, hasItems(0, 3));
        assertThat(second.size(), is(2));
        List<Integer> third = it.next();
        //eliminate 0 - here because eliminating it before would cause fillIn
        assertThat(third, hasItems(0, 2));
        assertThat(third.size(), is(2));
        List<Integer> fourth = it.next();
        //eliminate 2 - here because it has higher weight
        assertThat(fourth, is(Arrays.asList(2)));

        assertFalse(it.hasNext()); //sanity check
    }

}
