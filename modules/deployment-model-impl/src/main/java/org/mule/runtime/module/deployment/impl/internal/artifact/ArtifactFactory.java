/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.artifact;

import org.mule.runtime.module.artifact.api.Artifact;
import org.mule.runtime.module.artifact.api.descriptor.DeployableArtifactDescriptor;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.Properties;

/**
 * Generic Factory for an {@link Artifact}.
 */
public interface ArtifactFactory<D extends DeployableArtifactDescriptor, T extends Artifact<D>> {

  /**
   * @return the directory of the Artifact. Usually this directory contains the Artifact resources
   */
  File getArtifactDir();

  /**
   * Creates an Artifact
   *
   * @param artifactDir          directory where the artifact is located
   * @param deploymentProperties deployment properties for configuration management
   * @return the newly created Artifact
   */
  T createArtifact(File artifactLocation, Optional<Properties> deploymentProperties) throws IOException;

}
