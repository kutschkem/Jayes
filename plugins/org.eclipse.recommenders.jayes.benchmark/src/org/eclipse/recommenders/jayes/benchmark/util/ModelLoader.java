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
package org.eclipse.recommenders.jayes.benchmark.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.maven.repository.internal.MavenRepositorySystemSession;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.recommenders.commons.bayesnet.BayesianNetwork;
import org.eclipse.recommenders.jayes.BayesNet;
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

import com.google.common.collect.Lists;

public class ModelLoader {

    private static final String REMOTE_MODEL_REPO = "http://download.eclipse.org/recommenders/models/juno/";

    private List<BayesianNetwork> networks;
    private List<BayesNet> jayesNets;
    private static final Logger logger = LoggerFactory.getLogger(ModelLoader.class);

    public ModelLoader(String modelArtifact) throws ArtifactResolutionException, ClassNotFoundException,
            ComponentLookupException, PlexusContainerException, IOException {
        networks = loadModels(modelArtifact);
        logger.info("successfully loaded " + networks.size() + " models");
        jayesNets = Lists.transform(networks, new BayesNetConverter());
    }

    private List<BayesianNetwork> loadModels(String modelArtifact) throws ComponentLookupException,
            PlexusContainerException, ArtifactResolutionException, IOException, ClassNotFoundException {
        RepositorySystem reposys = new DefaultPlexusContainer().lookup(RepositorySystem.class);

        // setup session
        DefaultRepositorySystemSession session = new MavenRepositorySystemSession();
        LocalRepository localRepo = new LocalRepository("target/local-repo");
        session.setLocalRepositoryManager(reposys.newLocalRepositoryManager(localRepo));

        // resolve artifact
        DefaultArtifact artifact = new DefaultArtifact(modelArtifact);
        RemoteRepository remote = new RemoteRepository("remote-models", "default", REMOTE_MODEL_REPO);

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
            // everything should get closed by the ZipInputStream
            ObjectInputStream oin = new ObjectInputStream(stream);
            models.add((BayesianNetwork) oin.readObject());
            stream.closeEntry();
        }
        stream.close();
        return models;
    }

    public List<BayesNet> getJayesNetworks() {
        return this.jayesNets;
    }

    public List<BayesianNetwork> getIntermediateNetworks() {
        return this.networks;
    }

}
