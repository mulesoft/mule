/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.repository.internal;

import static org.mule.runtime.module.repository.internal.RepositoryServiceFactory.MULE_REMOTE_REPOSITORIES_PROPERTY;

import static java.util.Optional.of;

import org.mule.maven.client.api.BundleDependenciesResolutionException;
import org.mule.maven.client.api.MavenClient;
import org.mule.maven.client.api.model.RemoteRepository;
import org.mule.maven.pom.parser.api.model.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.repository.api.BundleNotFoundException;
import org.mule.runtime.module.repository.api.RepositoryConnectionException;
import org.mule.runtime.module.repository.api.RepositoryService;
import org.mule.runtime.module.repository.api.RepositoryServiceDisabledException;

import java.io.File;
import java.util.List;

/**
 * Default implementation for {@code RepositoryService}.
 *
 * @since 4.0
 */
public class DefaultRepositoryService implements RepositoryService {

  private final File repositoryFolder;
  private final List<RemoteRepository> remoteRepositories;
  private final MavenClient mavenClient;

  public DefaultRepositoryService(MavenClient mavenClient, File repositoryFolder, List<RemoteRepository> remoteRepositories) {
    this.mavenClient = mavenClient;
    this.repositoryFolder = repositoryFolder;
    this.remoteRepositories = remoteRepositories;
  }

  @Override
  public File lookupBundle(BundleDependency bundleDependency) {
    try {
      if (remoteRepositories.isEmpty()) {
        throw new RepositoryServiceDisabledException("Repository service has not been configured so it's disabled. "
            + "To enable it you must configure the set of repositories to use using the system property: "
            + MULE_REMOTE_REPOSITORIES_PROPERTY);
      }

      org.mule.maven.pom.parser.api.model.BundleDependency resolvedBundleDependency =
          mavenClient.resolveBundleDescriptor(muleToMavenDescriptor(bundleDependency.getDescriptor()), of(repositoryFolder),
                                              remoteRepositories);

      return new File(resolvedBundleDependency.getBundleUri().getPath());
    } catch (BundleDependenciesResolutionException e) {
      if (e.isArtifactNotFound()) {
        throw new BundleNotFoundException(e);
      } else {
        throw new RepositoryConnectionException("There was a problem connecting to one of the repositories", e);
      }
    }
  }

  private BundleDescriptor muleToMavenDescriptor(org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor bundleDescriptor) {
    return new org.mule.maven.pom.parser.api.model.BundleDescriptor.Builder()
        .setGroupId(bundleDescriptor.getGroupId())
        .setArtifactId(bundleDescriptor.getArtifactId())
        .setVersion(bundleDescriptor.getVersion())
        .setBaseVersion(bundleDescriptor.getBaseVersion() != null ? bundleDescriptor.getBaseVersion()
            : bundleDescriptor.getVersion())
        .setType(bundleDescriptor.getType())
        .setClassifier(bundleDescriptor.getClassifier().orElse(null))
        .build();
  }

}
