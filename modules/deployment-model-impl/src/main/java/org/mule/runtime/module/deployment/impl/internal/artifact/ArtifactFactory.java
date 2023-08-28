/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
