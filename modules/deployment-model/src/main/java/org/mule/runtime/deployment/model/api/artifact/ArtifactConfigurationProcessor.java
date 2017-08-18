/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.api.artifact;

import static java.lang.Thread.currentThread;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.registry.SpiServiceRegistry;

/**
 * Implementation of this interface are responsible of processing the configuration files of a Mule artifact and create an
 * {@link ArtifactContext}.
 * 
 * @since 4.0
 */
public interface ArtifactConfigurationProcessor {

  /**
   * Discovers a {@link ArtifactConfigurationProcessor} using SPI. One and only one must be present in the classpath.
   * 
   * @return an {@link ArtifactConfigurationProcessor} discovered using SPI.
   */
  static ArtifactConfigurationProcessor discover() {
    return new SpiServiceRegistry().lookupProvider(ArtifactConfigurationProcessor.class, currentThread().getContextClassLoader());
  }

  /**
   * Creates an {@link ArtifactContext} for an artifact based on the configuration files of the artifact.
   * 
   * @param artifactContextConfiguration the configuration of the artifact.
   * @return an {@link ArtifactContext}
   * @throws ConfigurationException if there was a problem processing the configuration of the artifact.
   */
  ArtifactContext createArtifactContext(ArtifactContextConfiguration artifactContextConfiguration)
      throws ConfigurationException;

}
