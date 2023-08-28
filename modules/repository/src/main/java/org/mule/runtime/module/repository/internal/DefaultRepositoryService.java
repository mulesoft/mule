/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.repository.internal;

import static org.mule.runtime.module.repository.internal.RepositoryServiceFactory.MULE_REMOTE_REPOSITORIES_PROPERTY;

import org.mule.maven.client.api.BundleDependenciesResolutionException;
import org.mule.maven.client.api.MavenClient;
import org.mule.maven.client.api.exception.BundleDependencyNotFoundException;
import org.mule.maven.pom.parser.api.model.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.repository.api.BundleNotFoundException;
import org.mule.runtime.module.repository.api.RepositoryConnectionException;
import org.mule.runtime.module.repository.api.RepositoryService;
import org.mule.runtime.module.repository.api.RepositoryServiceDisabledException;

import java.io.File;

/**
 * Default implementation for {@code RepositoryService}.
 *
 * @since 4.0
 */
public class DefaultRepositoryService implements RepositoryService {

  private final MavenClient mavenClient;

  public DefaultRepositoryService(MavenClient mavenClient) {
    this.mavenClient = mavenClient;
  }

  @Override
  public File lookupBundle(BundleDependency bundleDependency) {
    try {
      if (mavenClient.getMavenConfiguration().getMavenRemoteRepositories().isEmpty()) {
        throw new RepositoryServiceDisabledException("Repository service has not been configured so it's disabled. "
            + "To enable it you must configure the set of repositories to use using the system property: "
            + MULE_REMOTE_REPOSITORIES_PROPERTY);
      }

      org.mule.maven.pom.parser.api.model.BundleDependency resolvedBundleDependency =
          mavenClient.resolveBundleDescriptor(muleToMavenDescriptor(bundleDependency.getDescriptor()));

      return new File(resolvedBundleDependency.getBundleUri().getPath());
    } catch (BundleDependencyNotFoundException e) {
      throw new BundleNotFoundException(e);
    } catch (BundleDependenciesResolutionException e) {
      throw new RepositoryConnectionException("There was a problem connecting to one of the repositories", e);
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
