/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.repository.internal;

import static org.mule.runtime.module.repository.internal.RepositoryServiceFactory.MULE_REMOTE_REPOSITORIES_PROPERTY;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.repository.api.BundleNotFoundException;
import org.mule.runtime.module.repository.api.RepositoryConnectionException;
import org.mule.runtime.module.repository.api.RepositoryService;
import org.mule.runtime.module.repository.api.RepositoryServiceDisabledException;

import java.io.File;
import java.util.List;

import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.transfer.ArtifactNotFoundException;

/**
 * Default implementation for {@code RepositoryService}.
 *
 * @since 4.0
 */
public class DefaultRepositoryService implements RepositoryService {

  private final RepositorySystem repositorySystem;
  private final DefaultRepositorySystemSession repositorySystemSession;
  private final List<RemoteRepository> remoteRepositories;

  DefaultRepositoryService(RepositorySystem repositorySystem, DefaultRepositorySystemSession repositorySystemSession,
                           List<RemoteRepository> remoteRepositories) {
    this.repositorySystem = repositorySystem;
    this.repositorySystemSession = repositorySystemSession;
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
      DefaultArtifact artifact = toArtifact(bundleDependency);
      ArtifactRequest getArtifactRequest = new ArtifactRequest();
      getArtifactRequest.setRepositories(remoteRepositories);
      getArtifactRequest.setArtifact(artifact);
      ArtifactResult artifactResult = repositorySystem.resolveArtifact(repositorySystemSession, getArtifactRequest);
      return artifactResult.getArtifact().getFile();
    } catch (ArtifactResolutionException e) {
      if (e.getCause() instanceof ArtifactNotFoundException) {
        throw new BundleNotFoundException(e);
      } else {
        throw new RepositoryConnectionException("There was a problem connecting to one of the repositories", e);

      }
    }
  }

  private DefaultArtifact toArtifact(BundleDependency bundleDependency) {
    return new DefaultArtifact(bundleDependency.getDescriptor().getGroupId(), bundleDependency.getDescriptor().getArtifactId(),
                               bundleDependency.getDescriptor().getClassifier().orElse(null),
                               bundleDependency.getDescriptor().getType(),
                               bundleDependency.getDescriptor().getVersion());
  }
}
