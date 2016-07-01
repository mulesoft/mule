/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.repository.interal;

import org.mule.runtime.module.reboot.MuleContainerBootstrapUtils;
import org.mule.runtime.module.repository.api.BundleDescriptor;
import org.mule.runtime.module.repository.api.BundleNotFoundException;
import org.mule.runtime.module.repository.api.RepositoryConnectionException;
import org.mule.runtime.module.repository.api.RepositoryService;
import org.mule.runtime.module.repository.api.RepositoryServiceDisabledException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.transfer.ArtifactNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultRepositoryService implements RepositoryService
{

    private static final String REPOSITORY_FOLDER = "repository";
    private static final String DEFAULT_REPOSITORY_TYPE = "default";

    private static final Logger logger = LoggerFactory.getLogger(DefaultRepositoryService.class);

    private final RepositorySystem repositorySystem;
    private final DefaultRepositorySystemSession repositorySystemSession;
    private List<RemoteRepository> remoteRepositories;
    private File dependenciesFolder;

    public DefaultRepositoryService()
    {
        repositorySystem = new SpiRepositorySystemFactory().createRepositorySystem();
        createRepositoryFolderIfDoesNotExists();
        collectRemoteRepositories();
        repositorySystemSession = new DefaultRepositorySystemSession();
        repositorySystemSession.setLocalRepositoryManager(repositorySystem.newLocalRepositoryManager(repositorySystemSession, new LocalRepository(dependenciesFolder)));
    }

    private void collectRemoteRepositories()
    {
        String remoteRepositories = System.getProperty(MULE_REMOTE_REPOSITORIES_PROPERTY, "");
        String[] remoteRepositoriesArray = remoteRepositories.split(",");
        this.remoteRepositories = new ArrayList<>();
        for (String remoteRepository : remoteRepositoriesArray)
        {
            if (!remoteRepositories.trim().equals(""))
            {
                this.remoteRepositories.add(new RemoteRepository.Builder(remoteRepository, DEFAULT_REPOSITORY_TYPE, remoteRepositories.trim()).build());
            }
        }
    }

    private void createRepositoryFolderIfDoesNotExists()
    {
        resolveDependenciesFolder();
        createDependenciesFolder();
    }

    private void createDependenciesFolder()
    {
        if (!dependenciesFolder.exists() && !dependenciesFolder.mkdirs())
        {
            throw new RuntimeException("Could not create dependencies folder with path " + dependenciesFolder.getAbsolutePath());
        }
    }

    private void resolveDependenciesFolder()
    {
        String userDefinedDependenciesFolder = System.getProperty(MULE_REPOSITORY_FOLDER_PROPERTY);
        if (userDefinedDependenciesFolder != null)
        {
            dependenciesFolder = new File(userDefinedDependenciesFolder);
        }
        else
        {
            dependenciesFolder = new File(MuleContainerBootstrapUtils.getMuleLibDir(), REPOSITORY_FOLDER);
        }
        if (logger.isDebugEnabled())
        {
            logger.debug("Using dependencies folder " + dependenciesFolder.getAbsolutePath());
        }
    }

    @Override
    public File lookupBundle(BundleDescriptor bundleDescriptor)
    {
        try
        {
            if (remoteRepositories.isEmpty())
            {
                throw new RepositoryServiceDisabledException("Repository service has not been configured so it's disabled. " +
                                                             "To enable it you must configure the set of repositories to use using the system property: " + MULE_REMOTE_REPOSITORIES_PROPERTY);
            }
            DefaultArtifact artifact = toArtifact(bundleDescriptor);
            ArtifactRequest getArtifactRequest = new ArtifactRequest();
            getArtifactRequest.setRepositories(remoteRepositories);
            getArtifactRequest.setArtifact(artifact);
            ArtifactResult artifactResult = repositorySystem.resolveArtifact(repositorySystemSession, getArtifactRequest);
            return artifactResult.getArtifact().getFile();
        }
        catch (ArtifactResolutionException e)
        {
            if (e.getCause() instanceof ArtifactNotFoundException)
            {
                throw new BundleNotFoundException("Couldn't find bundle " + bundleDescriptor.toString(), e);
            }
            else
            {
                throw new RepositoryConnectionException("There was a problem connecting to one of the repositories", e);

            }
        }
    }

    private DefaultArtifact toArtifact(BundleDescriptor bundleDescriptor)
    {
        return new DefaultArtifact(bundleDescriptor.getGroupId(), bundleDescriptor.getArtifactId(), "jar", bundleDescriptor.getVersion());
    }
}
