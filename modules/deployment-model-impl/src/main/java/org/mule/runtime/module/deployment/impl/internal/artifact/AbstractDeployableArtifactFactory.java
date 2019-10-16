/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.artifact;

import static org.mule.runtime.module.deployment.impl.internal.artifact.ArtifactFactoryUtils.validateArtifactLicense;

import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.deployment.model.api.DeployableArtifact;
import org.mule.runtime.deployment.model.api.DeployableArtifactDescriptor;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinitionProvider;
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
 * @param <T> the type of the {@link DeployableArtifact}
 * @since 4.
 */
public abstract class AbstractDeployableArtifactFactory<T extends DeployableArtifact> implements ArtifactFactory<T> {

  private LicenseValidator licenseValidator;
  private ComponentBuildingDefinitionProvider runtimeComponentBuildingDefinitionProvider;
  private LockFactory runtimeLockFactory;

  /**
   * Creates a new {@link AbstractDeployableArtifactFactory}
   * 
   * @param licenseValidator the license validator to use for plugins.
   * @param runtimeComponentBuildingDefinitionProvider provider for the runtime {@link org.mule.runtime.dsl.api.component.ComponentBuildingDefinition}s
   * @param runtimeLockFactory {@link LockFactory} for Runtime, a unique and shared lock factory to be used between different artifacts.
   */
  public AbstractDeployableArtifactFactory(LicenseValidator licenseValidator,
                                           ComponentBuildingDefinitionProvider runtimeComponentBuildingDefinitionProvider,
                                           LockFactory runtimeLockFactory) {
    this.licenseValidator = licenseValidator;
    this.runtimeComponentBuildingDefinitionProvider = runtimeComponentBuildingDefinitionProvider;
    this.runtimeLockFactory = runtimeLockFactory;
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
   * @param properties deployment properties
   * @return the created artifact.
   * @throws IOException if there was a problem reading the content of the artifact.
   */
  protected abstract T doCreateArtifact(File artifactDir, Optional<Properties> properties) throws IOException;

  /**
   * Creates the artifact descriptor of the artifact.
   *
   * @param artifactLocation the artifact location
   * @param deploymentProperties the artifact deployment properties
   * @return the artifact descriptor
   */
  public abstract DeployableArtifactDescriptor createArtifactDescriptor(File artifactLocation,
                                                                        Optional<Properties> deploymentProperties);

  /**
   * @return {@link ComponentBuildingDefinitionProvider} for runtime components.
   */
  public ComponentBuildingDefinitionProvider getRuntimeComponentBuildingDefinitionProvider() {
    return runtimeComponentBuildingDefinitionProvider;
  }

  /**
   * @return {@link LockFactory} associated to the Runtime.
   */
  public LockFactory getRuntimeLockFactory() {
    return runtimeLockFactory;
  }

}
