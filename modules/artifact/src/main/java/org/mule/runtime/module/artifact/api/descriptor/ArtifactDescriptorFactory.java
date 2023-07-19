/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.artifact.api.descriptor;

import org.mule.api.annotation.NoImplement;

import java.io.File;
import java.util.Optional;
import java.util.Properties;

/**
 * Creates artifact descriptor for application plugins
 *
 * @param <T> type of created descriptors
 */
@NoImplement
public interface ArtifactDescriptorFactory<T extends ArtifactDescriptor> {


  /**
   * Creates an artifact descriptor from a folder.
   *
   * @param artifactFolder       an existing folder containing artifact files
   * @param deploymentProperties properties provided for the deployment process.
   * @return a non null descriptor
   * @throws ArtifactDescriptorCreateException if the factory is not able to create a descriptor from the folder.
   */
  T create(File artifactFolder, Optional<Properties> deploymentProperties) throws ArtifactDescriptorCreateException;
}
