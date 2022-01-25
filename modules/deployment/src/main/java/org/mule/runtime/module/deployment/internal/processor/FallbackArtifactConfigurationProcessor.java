/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal.processor;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.deployment.model.api.artifact.ArtifactConfigurationProcessor;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContext;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContextConfiguration;

import org.slf4j.Logger;

class FallbackArtifactConfigurationProcessor implements ArtifactConfigurationProcessor {

  private static final Logger LOGGER = getLogger(FallbackArtifactConfigurationProcessor.class);

  private final ArtifactConfigurationProcessor primary;
  private final ArtifactConfigurationProcessor fallback;

  public FallbackArtifactConfigurationProcessor(ArtifactConfigurationProcessor primary, ArtifactConfigurationProcessor fallback) {
    this.primary = primary;
    this.fallback = fallback;
  }

  @Override
  public ArtifactContext createArtifactContext(ArtifactContextConfiguration artifactContextConfiguration)
      throws ConfigurationException {
    try {
      ArtifactContext createdArtifactContext = primary.createArtifactContext(artifactContextConfiguration);
      LOGGER.warn("Successfully created context with " + primary.toString() + " for deployment "
          + createdArtifactContext.getMuleContext().getConfiguration().getId());
      return createdArtifactContext;
    } catch (ConfigurationException e) {
      String message = "Falling back to " + fallback.toString() + " for deployment "
          + artifactContextConfiguration.getMuleContext().getConfiguration().getId();
      if (LOGGER.isDebugEnabled()) {
        LOGGER.warn(message, e);
      } else {
        LOGGER.warn(message + ": " + e.toString());
      }

      return fallback.createArtifactContext(artifactContextConfiguration);
    }
  }
}
