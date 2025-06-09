/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.artifact;

import static org.mule.runtime.module.artifact.activation.internal.deployable.AbstractDeployableProjectModelBuilder.defaultDeployableProjectModelBuilder;
import static org.mule.runtime.module.deployment.impl.internal.artifact.ArtifactFactoryUtils.validateArtifactLicense;

import static java.util.Optional.empty;

import org.mule.runtime.api.memory.management.MemoryManagementService;
import org.mule.runtime.deployment.model.api.DeployableArtifact;
import org.mule.runtime.deployment.model.api.artifact.ArtifactConfigurationProcessor;
import org.mule.runtime.module.artifact.activation.api.deployable.DeployableProjectModel;
import org.mule.runtime.module.artifact.api.descriptor.DeployableArtifactDescriptor;
import org.mule.runtime.module.license.api.LicenseValidator;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.Properties;

/**
 * Abstract class for {@link DeployableArtifact} factories.
 * <p/>
 * Handles license validation for the artifact plugins.
 *
 * @param <D> the type of the {@link DeployableArtifactDescriptor}
 * @param <T> the type of the {@link DeployableArtifact}
 * @since 4.z
 */
public abstract class AbstractDeployableArtifactFactory<D extends DeployableArtifactDescriptor, T extends DeployableArtifact<D>>
    implements ArtifactFactory<D, T> {

  private final LicenseValidator licenseValidator;
  private final MemoryManagementService memoryManagementService;
  private final ArtifactConfigurationProcessor artifactConfigurationProcessor;

  /**
   * Creates a new {@link AbstractDeployableArtifactFactory}
   *
   * @param licenseValidator               the license validator to use for plugins.
   * @param memoryManagementService        the memory management service.
   * @param artifactConfigurationProcessor the processor to use for building the application model.
   */
  public AbstractDeployableArtifactFactory(LicenseValidator licenseValidator,
                                           MemoryManagementService memoryManagementService,
                                           ArtifactConfigurationProcessor artifactConfigurationProcessor) {
    this.licenseValidator = licenseValidator;
    this.memoryManagementService = memoryManagementService;
    this.artifactConfigurationProcessor = artifactConfigurationProcessor;
  }

  @Override
  public T createArtifact(File artifactDir, Optional<Properties> properties) throws IOException {
    T artifact = doCreateArtifact(artifactDir, properties);
    validateArtifactLicense(artifact.getArtifactClassLoader().getClassLoader(), artifact.getArtifactPlugins(), licenseValidator);
    return artifact;
  }

  /**
   * Creates an instance of {@link DeployableArtifact}
   *
   * @param artifactDir the artifact deployment directory.
   * @param properties  deployment properties
   * @return the created artifact.
   * @throws IOException if there was a problem reading the content of the artifact.
   */
  protected abstract T doCreateArtifact(File artifactDir, Optional<Properties> properties) throws IOException;

  /**
   * Creates the artifact descriptor of the artifact.
   *
   * @param artifactLocation     the artifact location
   * @param deploymentProperties the artifact deployment properties
   * @return the artifact descriptor
   */
  public abstract DeployableArtifactDescriptor createArtifactDescriptor(File artifactLocation,
                                                                        Optional<Properties> deploymentProperties);

  /**
   * @return the memory management service.
   */
  public MemoryManagementService getMemoryManagementService() {
    return memoryManagementService;
  }

  /**
   * @return the processor to use for building the application model.
   */
  public ArtifactConfigurationProcessor getArtifactConfigurationProcessor() {
    return artifactConfigurationProcessor;
  }

  /**
   * Creates a {@link DeployableProjectModel} representing the structure of the artifact in {@code artifactLocation}.
   *
   * @param artifactLocation the artifact location.
   * @param isDomain         whether the artifact is a domain.
   * @return the {@link DeployableProjectModel} representing the structure of the artifact.
   */
  protected DeployableProjectModel createDeployableProjectModel(File artifactLocation, boolean isDomain) {
    return defaultDeployableProjectModelBuilder(artifactLocation, empty(), isDomain).build();
  }

}
