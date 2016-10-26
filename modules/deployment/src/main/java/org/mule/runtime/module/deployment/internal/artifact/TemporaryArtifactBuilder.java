/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal.artifact;

import org.mule.runtime.dsl.api.config.ArtifactConfiguration;

import java.io.File;

/**
 * Builder for creating {@code ToolingArtifact}s
 *
 * @since 4.0
 */
public interface TemporaryArtifactBuilder {

  /**
   * Allows to add an extensions to be used in the tooling artifact. The file type must be zip and be an extension.
   *
   * @param artifactPluginFile a file pointer to an extension file
   * @return the builder
   */
  TemporaryArtifactBuilder addArtifactPluginFile(File artifactPluginFile);

  /**
   * Allows to configure the set of mule components to be used by the artifact.
   *
   * @param artifactConfiguration the mule configuration used by the artifact
   * @return the builder
   */
  TemporaryArtifactBuilder setArtifactConfiguration(ArtifactConfiguration artifactConfiguration);

  /**
   * Builds a {@code ToolingArtifact} with the provided configuration.
   * 
   * @return a {@code ToolingArtifact}
   */
  TemporaryArtifact build();

}
