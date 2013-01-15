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
package org.eclipse.recommenders.jayes.io.benchmark;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.maven.repository.internal.MavenRepositorySystemSession;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.recommenders.commons.bayesnet.BayesianNetwork;
import org.eclipse.recommenders.jayes.BayesNet;
import org.eclipse.recommenders.jayes.io.XDSLReader;
import org.eclipse.recommenders.jayes.io.XDSLWriter;
import org.eclipse.recommenders.jayes.io.XMLBIFReader;
import org.eclipse.recommenders.jayes.io.XMLBIFWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.repository.LocalRepository;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.ArtifactRequest;
import org.sonatype.aether.resolution.ArtifactResolutionException;
import org.sonatype.aether.resolution.ArtifactResult;
import org.sonatype.aether.util.DefaultRepositorySystemSession;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.xml.sax.SAXException;

import com.google.caliper.SimpleBenchmark;
import com.google.common.collect.Lists;

public class IOBenchmark extends SimpleBenchmark
{

    private final String remoteModelRepo = "http://download.eclipse.org/recommenders/models/juno/";

    List<BayesianNetwork> networks;
    List<BayesNet> jayesNets;

    Logger logger = LoggerFactory.getLogger(IOBenchmark.class);

    public IOBenchmark() throws Exception {
        networks = loadModels();
        logger.info("successfully loaded " + networks.size() + " models");
        jayesNets = Lists.transform(networks, new BayesNetConverter());
    }

    private List<BayesianNetwork> loadModels() throws ComponentLookupException, PlexusContainerException,
            ArtifactResolutionException,
            IOException, ClassNotFoundException {
        RepositorySystem reposys = new DefaultPlexusContainer().lookup(RepositorySystem.class);

        //setup session
        DefaultRepositorySystemSession session = new MavenRepositorySystemSession();
        LocalRepository localRepo = new LocalRepository("target/local-repo");
        session.setLocalRepositoryManager(reposys.newLocalRepositoryManager(localRepo));

        //resolve artifact
        DefaultArtifact artifact = new DefaultArtifact("jre:jre:zip:call:1.0.0");
        RemoteRepository remote = new RemoteRepository("remote-models", "default", remoteModelRepo);

        ArtifactRequest request = new ArtifactRequest();
        request.addRepository(remote);
        request.setArtifact(artifact);

        ArtifactResult result = reposys.resolveArtifact(session, request);
        File models = result.getArtifact().getFile();

        return readModelFromZip(models);
    }

    private List<BayesianNetwork> readModelFromZip(File zip) throws IOException, ClassNotFoundException {
        ZipInputStream stream = new ZipInputStream(new FileInputStream(zip));
        List<BayesianNetwork> models = new ArrayList<BayesianNetwork>();
        ZipEntry entry;
        while ((entry = stream.getNextEntry()) != null) {
            if (entry.isDirectory()) {
                continue;
            }
            @SuppressWarnings("resource")
            //everything should get closed by the ZipInputStream
            ObjectInputStream oin = new ObjectInputStream(stream);
            models.add((BayesianNetwork) oin.readObject());
            stream.closeEntry();
        }
        stream.close();
        return models;
    }

    public List<BayesianNetwork> timeJavaDeserialization(int rep) throws IOException, ClassNotFoundException {
        List<BayesianNetwork> l = new ArrayList<BayesianNetwork>();
        for (int i = 0; i < rep; i++) {
            for (BayesianNetwork network : networks) {
                ByteArrayOutputStream sstr = new ByteArrayOutputStream();
                ObjectOutputStream ostr = new ObjectOutputStream(sstr);
                ostr.writeObject(network);
                ostr.close();
                byte[] serialized = sstr.toByteArray();

                ByteArrayInputStream istr = new ByteArrayInputStream(serialized);
                ObjectInputStream oistr = new ObjectInputStream(istr);
                l.add((BayesianNetwork) oistr.readObject());
                oistr.close();
            }
        }
        return l;
    }

    public List<BayesNet> timeXdslDeserialization(int rep) throws ParserConfigurationException, SAXException,
            IOException {
        List<BayesNet> l = new ArrayList<BayesNet>();
        for (int i = 0; i < rep; i++) {
            for (BayesNet net : jayesNets) {
                XDSLWriter wrt = new XDSLWriter();
                XDSLReader rdr = new XDSLReader();
                l.add(rdr.readFromString(wrt.write(net)));
            }
        }
        return l;
    }

    public List<BayesNet> timeXmlBifDeserialization(int rep) throws ParserConfigurationException, SAXException,
            IOException {
        List<BayesNet> l = new ArrayList<BayesNet>();
        for (int i = 0; i < rep; i++) {
            for (BayesNet net : jayesNets) {
                XMLBIFWriter wrt = new XMLBIFWriter();
                XMLBIFReader rdr = new XMLBIFReader();
                l.add(rdr.readFromString(wrt.write(net)));
            }
        }
        return l;
    }
}
