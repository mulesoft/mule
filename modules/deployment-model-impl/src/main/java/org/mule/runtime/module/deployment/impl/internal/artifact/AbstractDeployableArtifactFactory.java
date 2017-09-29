/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.artifact;

import static org.mule.runtime.module.deployment.impl.internal.artifact.ArtifactFactoryUtils.validateArtifactLicense;
import org.mule.runtime.deployment.model.api.DeployableArtifact;
import org.mule.runtime.module.license.api.LicenseValidator;

import java.io.File;
import java.io.IOException;

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

  /**
   * Creates a new {@link AbstractDeployableArtifactFactory}
   * 
   * @param licenseValidator the license validator to use for plugins.
   */
  public AbstractDeployableArtifactFactory(LicenseValidator licenseValidator) {
    this.licenseValidator = licenseValidator;
  }

  @Override
  public final T createArtifact(File artifactDir) throws IOException {
    T artifact = doCreateArtifact(artifactDir);
    validateArtifactLicense(artifact.getArtifactClassLoader().getClassLoader(), artifact.getArtifactPlugins(), licenseValidator);
    return artifact;
  }

  /**
   * Creates an instance of {@link DeployableArtifact}
   * 
   * @param artifactDir the artifact deployment directory.
   * @return the created artifact.
   * @throws IOException if there was a problem reading the content of the artifact.
   */
  protected abstract T doCreateArtifact(File artifactDir) throws IOException;


}
