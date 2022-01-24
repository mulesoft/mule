/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal.processor;

import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.deployment.model.api.artifact.ArtifactConfigurationProcessor;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContext;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContextConfiguration;

public class FallbackArtifactConfigurationProcessor implements ArtifactConfigurationProcessor {

  private final ArtifactConfigurationProcessor primary;
  private final ArtifactConfigurationProcessor fallback;

  public FallbackArtifactConfigurationProcessor(ArtifactConfigurationProcessor primary, ArtifactConfigurationProcessor fallback) {
    this.primary = primary;
    this.fallback = fallback;
  }

  @Override
  public ArtifactContext createArtifactContext(ArtifactContextConfiguration artifactContextConfiguration)
      throws ConfigurationException {

    if (/* sysprop */true) {
      return fallback.createArtifactContext(artifactContextConfiguration);
    } else {
      try {
        return primary.createArtifactContext(artifactContextConfiguration);
      } catch (ConfigurationException e) {
        return fallback.createArtifactContext(artifactContextConfiguration);
      }
    }
  }
}
