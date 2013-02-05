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
package org.eclipse.recommenders.jayes.benchmark;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.recommenders.commons.bayesnet.BayesianNetwork;
import org.eclipse.recommenders.jayes.BayesNet;
import org.eclipse.recommenders.jayes.benchmark.util.BayesNetConverter;
import org.eclipse.recommenders.jayes.benchmark.util.ModelLoader;
import org.eclipse.recommenders.jayes.io.XDSLReader;
import org.eclipse.recommenders.jayes.io.XDSLWriter;
import org.eclipse.recommenders.jayes.io.XMLBIFReader;
import org.eclipse.recommenders.jayes.io.XMLBIFWriter;

import com.google.caliper.SimpleBenchmark;

public class IOBenchmark extends SimpleBenchmark {
    private List<BayesianNetwork> networks;
    private List<BayesNet> jayesNets;

    public IOBenchmark() throws Exception {
        ModelLoader modelLoader = new ModelLoader("jre:jre:zip:call:1.0.0");
        this.networks = modelLoader.getIntermediateNetworks();
        this.jayesNets = modelLoader.getJayesNetworks();
    }

    public List<BayesNet> timeJavaDeserialization(int rep) throws IOException, ClassNotFoundException {
        List<BayesNet> l = new ArrayList<BayesNet>();
        BayesNetConverter converter = new BayesNetConverter();
        for (int i = 0; i < rep; i++) {
            for (BayesianNetwork network : networks) {
                byte[] serialized = writeNetwork(network);
                BayesianNetwork net = readNetwork(serialized);
                l.add(converter.transform(net));
            }
        }
        return l;
    }

    private BayesianNetwork readNetwork(byte[] serialized) throws IOException, ClassNotFoundException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(serialized);
        ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
        BayesianNetwork net = (BayesianNetwork) objectInputStream.readObject();
        objectInputStream.close();
        return net;
    }

    private byte[] writeNetwork(BayesianNetwork network) throws IOException {
        ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();
        ObjectOutputStream objectStream = new ObjectOutputStream(byteArrayStream);
        objectStream.writeObject(network);
        objectStream.close();
        byte[] serialized = byteArrayStream.toByteArray();
        return serialized;
    }

    public List<BayesNet> timeXdslDeserialization(int repetitions) throws Exception {
        List<BayesNet> dummy = new ArrayList<BayesNet>();
        for (int i = 0; i < repetitions; i++) {
            for (BayesNet net : jayesNets) {
                XDSLWriter wrt = new XDSLWriter();
                XDSLReader rdr = new XDSLReader();
                dummy.add(rdr.readFromString(wrt.write(net)));
            }
        }
        return dummy;
    }

    public List<BayesNet> timeXmlBifDeserialization(int repetitions) throws Exception {
        List<BayesNet> dummy = new ArrayList<BayesNet>();
        for (int i = 0; i < repetitions; i++) {
            for (BayesNet net : jayesNets) {
                XMLBIFWriter wrt = new XMLBIFWriter();
                XMLBIFReader rdr = new XMLBIFReader();
                dummy.add(rdr.readFromString(wrt.write(net)));
            }
        }
        return dummy;
    }
}
