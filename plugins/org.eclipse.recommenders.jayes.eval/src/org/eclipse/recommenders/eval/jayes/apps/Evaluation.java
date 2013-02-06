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
package org.eclipse.recommenders.eval.jayes.apps;

import static com.google.common.collect.Lists.newArrayList;
import static org.eclipse.recommenders.eval.jayes.util.CLIUtil.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.zip.ZipFile;

import org.apache.felix.service.command.Descriptor;
import org.apache.maven.repository.internal.DefaultServiceLocator;
import org.apache.maven.repository.internal.MavenRepositorySystemSession;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.recommenders.eval.jayes.DataPoint;
import org.eclipse.recommenders.eval.jayes.DataPointGenerator;
import org.eclipse.recommenders.eval.jayes.statistics.DataPointStatistics;
import org.eclipse.recommenders.eval.jayes.statistics.IStatisticsProvider;
import org.eclipse.recommenders.eval.jayes.util.CLIUtil;
import org.eclipse.recommenders.eval.jayes.util.RecommenderModelLoader;
import org.eclipse.recommenders.internal.rcp.repo.ManualWagonProvider;
import org.eclipse.recommenders.jayes.BayesNet;
import org.eclipse.recommenders.jayes.BayesNode;
import org.eclipse.recommenders.jayes.inference.IBayesInferer;
import org.eclipse.recommenders.jayes.io.XDSLReader;
import org.eclipse.recommenders.jayes.io.XMLBIFReader;
import org.eclipse.recommenders.jayes.testgen.scenario.impl.SampledScenarioGenerator;
import org.eclipse.recommenders.jayes.util.NumericalInstabilityException;
import org.eclipse.recommenders.rdk.utils.Commands.CommandProvider;
import org.eclipse.recommenders.utils.gson.GsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.connector.wagon.WagonProvider;
import org.sonatype.aether.connector.wagon.WagonRepositoryConnectorFactory;
import org.sonatype.aether.repository.LocalRepository;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.ArtifactRequest;
import org.sonatype.aether.resolution.ArtifactResolutionException;
import org.sonatype.aether.resolution.ArtifactResult;
import org.sonatype.aether.spi.connector.RepositoryConnectorFactory;
import org.sonatype.aether.util.DefaultRepositorySystemSession;
import org.sonatype.aether.util.artifact.DefaultArtifact;

import com.google.common.base.Splitter;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.name.Names;

@CommandProvider
public class Evaluation implements IApplication {

    private File outputDirectory;
    private int iterations;
    private int numTestcases;
    private static final long seed = 1337 + 42;
    private static Logger logger = LoggerFactory.getLogger(Evaluation.class);

    private Map<String, IBayesInferer> inferer = new HashMap<String, IBayesInferer>();
    private List<IStatisticsProvider> statisticsProvider = newArrayList();
    private double observationProb;
    private String model;
    private String modelRepo;

    private final int repetitions = 1;
    private List<Module> guiceModules = newArrayList();
    private String modelRepoId;
    private String modelArtifact;

    public void injectEvaluationSubjects() {
        for (Module module : guiceModules) {
            Injector jayes = Guice.createInjector(module);
            inferer.put(jayes.getInstance(Key.get(String.class, Names.named("specifier"))),
                    jayes.getInstance(IBayesInferer.class));
            statisticsProvider.add(jayes.getInstance(IStatisticsProvider.class));
        }
    }

    @Descriptor("")
    public void runEvaluation() throws Exception {
        DataPointGenerator dataGen = new DataPointGenerator();

        for (Entry<String, IBayesInferer> inferenceEntry : inferer.entrySet()) {
            dataGen.addInferrer(inferenceEntry.getKey(), inferenceEntry.getValue());
        }

        BayesNet net = loadModel();

        dataGen.setNetwork(net);
        for (int iteration = 0; iteration < iterations; iteration++) {
            long time = System.currentTimeMillis();
            logger.info("Iteration " + iteration + " observProb= " + observationProb + " , " + model);
            try {
                List<DataPoint> points = evaluate(dataGen, observationProb, numTestcases, repetitions);
                printData(iteration, points);
                printStat(iteration, computeStatistics(points));
            } catch (NumericalInstabilityException exNumInst) {
                logger.warn("Configuration Unstable!");
            }
            logger.info("time taken: " + (System.currentTimeMillis() - time));
        }

    }

    public BayesNet loadModel() throws Exception {
        File artifact = resolveMavenArtifact();
        ZipFile zipFile = new ZipFile(artifact);
        InputStream str = zipFile.getInputStream(zipFile.getEntry(model));
        BayesNet net = loadNetwork(str);
        zipFile.close();
        return net;
    }

    private BayesNet loadNetwork(InputStream str) throws Exception {
        if (model.endsWith(".data")) {
            return RecommenderModelLoader.load(str);
        } else if (model.endsWith(".xdsl")) {
            XDSLReader xdslReader = new XDSLReader();
            xdslReader.setLegacyMode(true);
            return xdslReader.read(str);
        } else if (model.endsWith(".xml")) {
            return new XMLBIFReader().read(str);
        } else {
            throw new IllegalArgumentException("File name does not correspond to a supported data format");
        }
    }

    private File resolveMavenArtifact() throws ArtifactResolutionException, IOException {
        DefaultServiceLocator locator = new DefaultServiceLocator();
        locator.addService(WagonProvider.class, ManualWagonProvider.class);
        locator.addService(RepositoryConnectorFactory.class, WagonRepositoryConnectorFactory.class);

        RepositorySystem reposys = locator.getService(RepositorySystem.class);

        //setup session
        DefaultRepositorySystemSession session = new MavenRepositorySystemSession();
        LocalRepository localRepo = new LocalRepository("target/local-repo");
        session.setLocalRepositoryManager(reposys.newLocalRepositoryManager(localRepo));

        //resolve artifact
        DefaultArtifact artifact = new DefaultArtifact(modelArtifact);
        RemoteRepository remote = new RemoteRepository(modelRepoId, "default", modelRepo);

        ArtifactRequest request = new ArtifactRequest();
        request.addRepository(remote);
        request.setArtifact(artifact);

        ArtifactResult result = reposys.resolveArtifact(session, request);
        return result.getArtifact().getFile();

    }

    private void printData(int iteration, List<DataPoint> points)
            throws IOException {
        File evalFile = new File(outputDirectory, getEvaluationFolder(iteration) + "/evalData.txt");
        if (!evalFile.exists()) {
            evalFile.getParentFile().mkdirs();
            evalFile.createNewFile();
        }
        GsonUtil.serialize(points, evalFile);
    }

    public String getEvaluationFolder(int iteration) {
        return new File(model).getName() + observationProb + "-"
                + Integer.toHexString(iteration);
    }

    private void printStat(int iteration, Map<String, Number> stat)
            throws IOException {
        File statFile = new File(outputDirectory, getEvaluationFolder(iteration) + "/evalStat.txt");
        if (!statFile.exists()) {
            statFile.getParentFile().mkdirs();
            statFile.createNewFile();
        }
        GsonUtil.serialize(stat, statFile);
    }

    private List<DataPoint> evaluate(DataPointGenerator dataGen, double observationProb, int cases, int repeat) {
        SampledScenarioGenerator testGen = new SampledScenarioGenerator();
        testGen.setNetwork(dataGen.getNetwork());
        testGen.setEvidenceRate(observationProb);
        testGen.seed(seed);

        List<DataPoint> points = new ArrayList<DataPoint>();
        for (int i = 0; i < cases; i++) {
            Map<BayesNode, String> evidence = testGen.testcase();
            DataPoint point = dataGen.generate(evidence, repeat);
            points.add(point);
        }

        return points;
    }

    public Map<String, Number> computeStatistics(List<DataPoint> points) {
        DataPointStatistics statistics = new DataPointStatistics(inferer.keySet());
        Map<String, Number> stat = statistics.computeStatistics(points);
        applyStatisticsProviders(stat);
        return stat;
    }

    private void applyStatisticsProviders(Map<String, Number> stat) {
        for (IStatisticsProvider provider : statisticsProvider) {
            stat.putAll(provider.computeStatistics());
        }

    }

    @Override
    public Object start(IApplicationContext context) throws Exception {
        String[] args = (String[]) context.getArguments().get(OSGi_CMD_ARGS);
        logger.debug(Arrays.toString(args));
        String config = CLIUtil.findArg(args, CONFIG_PARAM);
        loadEvalConfiguration(config);

        runEvaluation();
        return 0;
    }

    @Descriptor("")
    public void loadEvalConfiguration(String config) throws IOException, ClassNotFoundException,
            InstantiationException, IllegalAccessException {
        Properties properties = new Properties();
        properties.load(getClass().getClassLoader().getResourceAsStream(config));

        modelRepo = properties.getProperty("evaluation.modelRepo");
        modelRepoId = properties.getProperty("evaluation.modelRepoId");
        modelArtifact = properties.getProperty("evaluation.modelArtifact");
        model = properties.getProperty("evaluation.model");
        observationProb = Double.valueOf(properties.getProperty("evaluation.observationRate"));
        iterations = Integer.valueOf(properties.getProperty("evaluation.iterations"));
        numTestcases = Integer.valueOf(properties.getProperty("evaluation.numTestcases"));
        outputDirectory = new File(properties.getProperty(OUTPUTDIR));

        loadModules(Splitter.on(',').trimResults().split(properties.getProperty("evaluation.modules")));

        injectEvaluationSubjects();
    }

    @SuppressWarnings("unchecked")
    private void loadModules(Iterable<String> split) throws ClassNotFoundException, InstantiationException,
            IllegalAccessException {
        for (String moduleName : split) {
            String fullname = "org.eclipse.recommenders.internal.jayes.eval.wiring." + moduleName + "Module";
            Class<? extends Module> clazz = (Class<? extends Module>) Class.forName(fullname);
            this.guiceModules.add(clazz.newInstance());
        }

    }

    @Override
    public void stop() {

    }
}
