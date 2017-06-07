/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.repository.internal;

import org.mule.runtime.module.reboot.api.MuleContainerBootstrapUtils;
import org.mule.runtime.module.repository.api.RepositoryService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepositoryServiceFactory {

  /**
   * System property key to specify a custom repository folder. By default the container will use $MULE_HOME/lib/repository
   */
  public static final String MULE_REPOSITORY_FOLDER_PROPERTY = "mule.repository.folder";

  /**
   * System property key to specify the remote repositories to use. Multiple values must be comma separated.
   * <p>
   * If no value is provided then the repository will be disabled causing a {@code RepositoryServiceDisabledException} if any
   * method is called.
   */
  public static final String MULE_REMOTE_REPOSITORIES_PROPERTY = "mule.repository.repositories";

  private static final String REPOSITORY_FOLDER = "repository";
  private static final String DEFAULT_REPOSITORY_TYPE = "default";

  private static final Logger logger = LoggerFactory.getLogger(DefaultRepositoryService.class);

  public RepositoryService createRepositoryService() {
    RepositorySystem repositorySystem = new SpiRepositorySystemFactory().createRepositorySystem();
    File repositoryFolder = createRepositoryFolderIfDoesNotExists();
    List<RemoteRepository> remoteRepositories = collectRemoteRepositories();
    DefaultRepositorySystemSession repositorySystemSession = new DefaultRepositorySystemSession();
    repositorySystemSession.setLocalRepositoryManager(repositorySystem
        .newLocalRepositoryManager(repositorySystemSession, new LocalRepository(repositoryFolder)));
    return new DefaultRepositoryService(repositorySystem, repositorySystemSession, remoteRepositories);
  }

  private List<RemoteRepository> collectRemoteRepositories() {
    String[] remoteRepositoriesArray = System.getProperty(MULE_REMOTE_REPOSITORIES_PROPERTY, "").split(",");
    List<RemoteRepository> remoteRepositories = new ArrayList<>();
    for (String remoteRepository : remoteRepositoriesArray) {
      if (!remoteRepository.trim().equals("")) {
        remoteRepositories
            .add(new RemoteRepository.Builder(remoteRepository, DEFAULT_REPOSITORY_TYPE, remoteRepository.trim()).build());
      }
    }
    return remoteRepositories;
  }

  private File createRepositoryFolderIfDoesNotExists() {
    File repositoryFolder = resolveRepositoryFolder();
    createRepositoryFolder(repositoryFolder);
    return repositoryFolder;
  }

  private void createRepositoryFolder(File repositoryFolder) {
    if (!repositoryFolder.exists() && !repositoryFolder.mkdirs()) {
      throw new RuntimeException("Could not create dependencies folder with path " + repositoryFolder.getAbsolutePath());
    }
  }

  private File resolveRepositoryFolder() {
    String userDefinedDependenciesFolder = System.getProperty(MULE_REPOSITORY_FOLDER_PROPERTY);
    File repositoryFolder;
    if (userDefinedDependenciesFolder != null) {
      repositoryFolder = new File(userDefinedDependenciesFolder);
    } else {
      repositoryFolder = new File(MuleContainerBootstrapUtils.getMuleLibDir(), REPOSITORY_FOLDER);
    }
    if (logger.isDebugEnabled()) {
      logger.debug("Using dependencies folder " + repositoryFolder.getAbsolutePath());
    }
    return repositoryFolder;
  }

}
