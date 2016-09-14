/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal;

import static java.lang.String.format;
import static org.mule.runtime.core.config.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.util.Preconditions.checkState;
import org.mule.runtime.config.spring.dsl.api.config.ArtifactConfiguration;
import org.mule.runtime.core.api.registry.ServiceRegistry;
import org.mule.runtime.module.deployment.internal.connectivity.artifact.TemporaryArtifact;
import org.mule.runtime.module.deployment.internal.connectivity.artifact.TemporaryArtifactBuilder;
import org.mule.runtime.module.deployment.internal.connectivity.artifact.TemporaryArtifactBuilderFactory;
import org.mule.runtime.module.repository.api.BundleDescriptor;
import org.mule.runtime.module.repository.api.RepositoryService;
import org.mule.runtime.core.api.connectivity.ConnectivityTestingService;
import org.mule.runtime.module.tooling.api.connectivity.ConnectivityTestingServiceBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation for {@code ConnectivityTestingServiceBuilder}.
 *
 * @since 4.0
 */
class DefaultConnectivityTestingServiceBuilder implements ConnectivityTestingServiceBuilder {

  private static final String EXTENSION_BUNDLE_TYPE = "zip";
  private final RepositoryService repositoryService;
  private final TemporaryArtifactBuilderFactory artifactBuilderFactory;
  private ServiceRegistry serviceRegistry;
  private List<BundleDescriptor> bundleDescriptors = new ArrayList<>();
  private ArtifactConfiguration artifactConfiguration;
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
  public ConnectivityTestingServiceBuilder addExtension(String groupId, String artifactId, String artifactVersion) {
    this.bundleDescriptors.add(new BundleDescriptor.Builder().setGroupId(groupId).setArtifactId(artifactId)
        .setType(EXTENSION_BUNDLE_TYPE).setVersion(artifactVersion).build());
    return this;
  }

  /**
   * {@inheritDoc}
   */
  public ConnectivityTestingServiceBuilder setArtifactConfiguration(ArtifactConfiguration artifactConfiguration) {
    this.artifactConfiguration = artifactConfiguration;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectivityTestingService build() {
    checkState(artifactConfiguration != null, "artifact configuration cannot be null");
    checkState(!bundleDescriptors.isEmpty(), "no extensions were configured");
    TemporaryArtifact temporaryArtifact = buildArtifact();
    return new DefaultConnectivityTestingService(temporaryArtifact);
  }

  private TemporaryArtifact buildArtifact() {
    if (temporaryArtifact != null) {
      return temporaryArtifact;
    }

    TemporaryArtifactBuilder temporaryArtifactBuilder = artifactBuilderFactory.newBuilder()
        .setArtifactConfiguration(artifactConfiguration);

    bundleDescriptors.stream()
        .forEach(bundleDescriptor -> temporaryArtifactBuilder
            .addArtifactPluginFile(repositoryService.lookupBundle(bundleDescriptor)));
    temporaryArtifact = temporaryArtifactBuilder.build();

    return temporaryArtifact;
  }

}
