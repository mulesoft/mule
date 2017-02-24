/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal;

import static org.mule.runtime.api.util.Preconditions.checkState;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.MULE_PLUGIN_CLASSIFIER;
import org.mule.runtime.api.app.declaration.ArtifactDeclaration;
import org.mule.runtime.core.api.connectivity.ConnectivityTestingService;
import org.mule.runtime.core.api.registry.ServiceRegistry;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.module.deployment.impl.internal.artifact.TemporaryArtifact;
import org.mule.runtime.module.deployment.impl.internal.artifact.TemporaryArtifactBuilder;
import org.mule.runtime.module.deployment.impl.internal.artifact.TemporaryArtifactBuilderFactory;
import org.mule.runtime.module.artifact.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.descriptor.BundleDescriptor;
import org.mule.runtime.module.repository.api.RepositoryService;
import org.mule.runtime.module.tooling.api.connectivity.ConnectivityTestingServiceBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation for {@code ConnectivityTestingServiceBuilder}.
 *
 * @since 4.0
 */
class DefaultConnectivityTestingServiceBuilder implements ConnectivityTestingServiceBuilder {

  private static final String JAR_BUNDLE_TYPE = "jar";
  private final RepositoryService repositoryService;
  private final TemporaryArtifactBuilderFactory artifactBuilderFactory;
  private ServiceRegistry serviceRegistry;
  private List<BundleDependency> bundleDependencies = new ArrayList<>();
  private List<BundleDependency> extensionsBundleDependencies = new ArrayList<>();
  private ArtifactDeclaration artifactDeclaration;
  private TemporaryArtifact temporaryArtifact;

  DefaultConnectivityTestingServiceBuilder(RepositoryService repositoryService,
                                           TemporaryArtifactBuilderFactory artifactBuilderFactory,
                                           ServiceRegistry serviceRegistry) {
    this.artifactBuilderFactory = artifactBuilderFactory;
    this.repositoryService = repositoryService;
    this.serviceRegistry = serviceRegistry;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectivityTestingServiceBuilder addDependency(String groupId, String artifactId, String artifactVersion) {
    BundleDescriptor bundleDescriptor =
        new BundleDescriptor.Builder().setGroupId(groupId).setArtifactId(artifactId).setVersion(artifactVersion)
            .setType(JAR_BUNDLE_TYPE).build();
    this.bundleDependencies
        .add(new BundleDependency.Builder().setDescriptor(bundleDescriptor).build());
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectivityTestingServiceBuilder addExtension(String groupId, String artifactId, String artifactVersion) {
    BundleDescriptor bundleDescriptor =
        new BundleDescriptor.Builder().setGroupId(groupId).setArtifactId(artifactId).setVersion(artifactVersion)
            .setType(JAR_BUNDLE_TYPE).setClassifier(MULE_PLUGIN_CLASSIFIER).build();
    this.extensionsBundleDependencies
        .add(new BundleDependency.Builder().setDescriptor(bundleDescriptor).build());
    return this;
  }

  /**
   * {@inheritDoc}
   */
  public ConnectivityTestingServiceBuilder setArtifactDeclaration(ArtifactDeclaration artifactDeclaration) {
    this.artifactDeclaration = artifactDeclaration;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectivityTestingService build() {
    checkState(artifactDeclaration != null, "artifact configuration cannot be null");
    checkState(!extensionsBundleDependencies.isEmpty(), "no extensions were configured");
    TemporaryArtifact temporaryArtifact = buildArtifact();
    return new TemporaryArtifactConnectivityTestingService(temporaryArtifact);
  }

  private TemporaryArtifact buildArtifact() {
    if (temporaryArtifact != null) {
      return temporaryArtifact;
    }

    TemporaryArtifactBuilder temporaryArtifactBuilder = artifactBuilderFactory.newBuilder()
        .setArtifactDeclaration(artifactDeclaration);

    extensionsBundleDependencies.stream()
        .forEach(bundleDescriptor -> temporaryArtifactBuilder
            .addArtifactPluginFile(repositoryService.lookupBundle(bundleDescriptor)));
    bundleDependencies.stream()
        .forEach(bundleDescriptor -> temporaryArtifactBuilder
            .addArtifactLibraryFile(repositoryService.lookupBundle(bundleDescriptor)));
    temporaryArtifact = temporaryArtifactBuilder.build();

    return temporaryArtifact;
  }

}
