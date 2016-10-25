/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.api.artifact;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.config.bootstrap.ArtifactType;
import org.mule.runtime.core.registry.SpiServiceRegistry;
import org.mule.runtime.dsl.api.config.ArtifactConfiguration;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation of this interface are responsible of processing the configuration files of a mule artifact and create an
 * {@link ArtifactContext}.
 * 
 * @since 4.0
 */
public interface ArtifactConfigurationProcessor {

  /**
   * Discovers a {@link ArtifactConfigurationProcessor} from the classpath. One and only one must be present in the classpath.
   * 
   * @return an {@link ArtifactConfigurationProcessor} discovered from the classpath.
   */
  static ArtifactConfigurationProcessor discover() {
    return new SpiServiceRegistry().lookupProvider(ArtifactConfigurationProcessor.class);
  }

  /**
   * Creates an {@link ArtifactContext} for an artifact based on the configuraiton files of the artifact.
   * 
   * @param muleContext the {@link MuleContext} of the artifact.
   * @param configResources the list of configuration files of the artifact
   * @param artifactProperties the artifact properties
   * @param artifactType the artifact type
   * @return an {@link ArtifactContext}
   * @throws ConfigurationException if there was a problem processing the configuration of the artifact.
   */
  ArtifactContext createArtifactContext(MuleContext muleContext, String[] configResources,
                                        ArtifactConfiguration artifactConfiguration, Map<String, String> artifactProperties,
                                        ArtifactType artifactType, boolean enableLazyInitialization,
                                        List<MuleContextServiceConfigurator> serviceConfigurators,
                                        Optional<MuleContext> parentContext)
      throws ConfigurationException;

}
