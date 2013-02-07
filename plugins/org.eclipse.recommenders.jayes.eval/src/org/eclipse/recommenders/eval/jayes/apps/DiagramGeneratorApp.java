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

import static com.google.common.collect.Maps.newHashMap;
import static org.eclipse.recommenders.eval.jayes.util.CLIUtil.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.felix.service.command.Descriptor;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.recommenders.eval.jayes.DataPoint;
import org.eclipse.recommenders.eval.jayes.statistics.memory.JunctionTreeMemoryStatisticsProvider;
import org.eclipse.recommenders.eval.jayes.util.GuiceUtil;
import org.eclipse.recommenders.utils.gson.GsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Module;

public class DiagramGeneratorApp implements IApplication {

    private Map<String, String> libToColorMap = new HashMap<String, String>();

    private static final String[] colors = new String[] { "red", "blue", "green", "magenta" };

    private String sortByLibrary;
    private String outputData = "plotData.tsv";
    private String outputScript = "gnuPlot.p";
    private String root;
    private static final Logger logger = LoggerFactory.getLogger(DiagramGeneratorApp.class);
    private Map<String, Module> guiceModules = newHashMap();

    @Override
    public Object start(IApplicationContext context) throws Exception {
        String[] args = (String[]) context.getArguments().get(OSGi_CMD_ARGS);
        logger.debug(Arrays.toString(args));
        loadDiagramConfig(findArg(args, CONFIG_PARAM));

        generateDiagrams();
        return 0;
    }

    @Descriptor("")
    public void loadDiagramConfig(String configFile) throws IOException, ClassNotFoundException,
            InstantiationException, IllegalAccessException {
        Properties properties = new Properties();
        properties.load(getClass().getClassLoader().getResourceAsStream(configFile));

        Iterable<String> moduleNames = Splitter.on(',').trimResults().split(properties.getProperty(MODULES));
        initializeModuleMap(moduleNames, GuiceUtil.loadModules(moduleNames));

        root = properties.getProperty(OUTPUTDIR);
        sortByLibrary = GuiceUtil.getNamedInstance(guiceModules.get(properties.getProperty("diagrams.sortBy")),
                String.class, JunctionTreeMemoryStatisticsProvider.SPECIFIER);

        configureColors();
    }

    private void configureColors() {
        int colorIndex = 0;
        for (Module module : guiceModules.values()) {
            if (colorIndex >= colors.length) {
                break;
            }
            String library = GuiceUtil.getNamedInstance(module, String.class,
                    JunctionTreeMemoryStatisticsProvider.SPECIFIER);
            libToColorMap.put(library, colors[colorIndex]);
            colorIndex++;
        }
    }

    private void initializeModuleMap(Iterable<String> names, List<Module> modules) {
        Iterator<String> it1 = names.iterator();
        Iterator<Module> it2 = modules.iterator();
        while (it1.hasNext()) {
            guiceModules.put(it1.next(), it2.next());
        }
    }

    @Descriptor("")
    public void generateDiagrams() throws IOException {
        File dir = new File(root);

        for (File child : dir.listFiles()) {
            if (child.isDirectory())
                try {
                    generateDiagram(child);
                } catch (IOException ex) { //we want the application to continue with other subdirectories
                    ex.printStackTrace();
                }
        }
    }

    private void generateDiagram(File dir) throws IOException {
        logger.info("generating Diagram for " + dir.getName());
        File evalFile = new File(dir, EVALUATION_DATA_FILE);
        if (!evalFile.exists()) {
            return;
        }

        List<DataPoint> points = GsonUtil.deserialize(evalFile, new TypeToken<List<DataPoint>>() {
        }.getType());
        if (points.size() == 0)
            return;

        if (sortByLibrary != null) {
            Collections.sort(points, new Comparator<DataPoint>() {

                @Override
                public int compare(DataPoint o1, DataPoint o2) {
                    return (int) (o1.getTime(sortByLibrary) - o2.getTime(sortByLibrary));
                }
            });
        }
        writeDataAsTSV(points, dir);
        writeGnuplotScript(dir);
    }

    private void writeDataAsTSV(List<DataPoint> points, File dir) throws IOException {
        File f = new File(dir, outputData);
        // write header
        BufferedWriter wrt = new BufferedWriter(new FileWriter(f));
        for (String lib : libToColorMap.keySet()) {
            wrt.append(lib.replaceAll("\\s", "_"));
            wrt.append('\t');
        }
        wrt.newLine();

        for (DataPoint p : points) {
            for (String lib : libToColorMap.keySet()) {
                if (p.getLibs().contains(lib)) {
                    wrt.append(String.valueOf(p.getTime(lib) / Math.pow(10, 6)));
                } else {
                    wrt.append('0');
                }
                wrt.append('\t');
            }
            wrt.newLine();
        }
        wrt.close();
    }

    private void writeGnuplotScript(File dir) throws IOException {
        String[] libs = libToColorMap.keySet().toArray(new String[0]);
        File plott = new File(dir, outputScript);
        BufferedWriter wrt = new BufferedWriter(new FileWriter(plott));
        wrt.append("#!/usr/bin/gnuplot -persist");
        wrt.newLine();
        wrt.append("set ylabel \"ms\" ; ");
        wrt.append("set xlabel \"testcase\" ; ");
        wrt.append(String.format("set title \"%s\" ; ", dir.getName()));
        wrt.newLine();
        wrt.append("set key outside below");
        wrt.newLine();
        wrt.append("set multiplot layout 1,2");
        wrt.newLine();
        wrt.append(String.format("plot x , '%s' using 1:2 with points lt -1", outputData));
        wrt.newLine();
        wrt.append("plot");
        for (int i = 0; i < libs.length; i++) {
            if (i != 0)
                wrt.append(" ,");
            wrt.append(String.format(" '%s' using %d title '%s' with points pt %d lc rgb '%s'", outputData, i + 1,
                    libs[i].toString(), i + 1, libToColorMap.get(libs[i])));
        }
        wrt.newLine();
        wrt.append("unset multiplot");
        wrt.close();
        plott.setExecutable(true);

    }

    @Override
    public void stop() {

    }

}
