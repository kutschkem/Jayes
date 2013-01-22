package org.eclipse.recommenders.tests.jayes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;

import java.util.Arrays;

import org.eclipse.recommenders.jayes.BayesNet;
import org.eclipse.recommenders.jayes.inference.junctionTree.JunctionTree;
import org.eclipse.recommenders.jayes.inference.junctionTree.JunctionTreeBuilder;
import org.eclipse.recommenders.tests.jayes.util.NetExamples;
import org.junit.Test;

public class JunctionTreeBuilderTest {

    @Test
    public void testOptimizeStructure() {
        // optimizeStructure should add the cluster {a}, even though there are other clusters containing "a"

        BayesNet net = NetExamples.treeNet();
        JunctionTree jtree = JunctionTreeBuilder.fromNet(net);

        assertEquals(4, jtree.getClusters().size());
        assertEquals(3, jtree.getSepSets().size());
        assertThat(jtree.getClusters(), hasItem(Arrays.asList(net.getNode("a").getId())));
    }

}
