package org.eclipse.recommenders.tests.jayes.testgen;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.Collection;
import java.util.Map;

import org.eclipse.recommenders.jayes.BayesNet;
import org.eclipse.recommenders.jayes.BayesNode;
import org.eclipse.recommenders.jayes.testgen.scenario.impl.SampledScenarioGenerator;
import org.eclipse.recommenders.tests.jayes.util.NetExamples;
import org.junit.Test;

public class ScenarioGenerationTest {

    private static final int SEED = 1337;

    @Test
    public void testReproducibility() {
        SampledScenarioGenerator generator = new SampledScenarioGenerator();
        generator.seed(SEED);

        BayesNet net = NetExamples.testNet1();
        generator.setBN(net);

        Collection<Map<BayesNode, String>> scenarios = generator.generate(100);

        generator.seed(SEED);
        Collection<Map<BayesNode, String>> scenarios2 = generator.generate(100);

        assertEquals(100, scenarios.size());
        assertEquals(100, scenarios.size());

        assertEquals(scenarios, scenarios2);
        assertThat(scenarios, is(not(sameInstance(scenarios2))));
    }

}
