/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.deployment.internal.processor;

import static org.mule.runtime.api.util.MuleSystemProperties.FORCE_PARSE_CONFIG_XMLS_ON_DEPLOYMENT_PROPERTY;

import static java.lang.Boolean.getBoolean;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.deployment.model.api.artifact.ArtifactConfigurationProcessor;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContext;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContextConfiguration;

import org.slf4j.Logger;

class FallbackArtifactConfigurationProcessor implements ArtifactConfigurationProcessor {

  private static final Logger LOGGER = getLogger(FallbackArtifactConfigurationProcessor.class);

  private final FallbackAllowedArtifactConfigurationProcessor primary;
  private final ArtifactConfigurationProcessor fallback;

  public FallbackArtifactConfigurationProcessor(FallbackAllowedArtifactConfigurationProcessor primary,
                                                ArtifactConfigurationProcessor fallback) {
    this.primary = primary;
    this.fallback = fallback;
  }

  @Override
  public ArtifactContext createArtifactContext(ArtifactContextConfiguration artifactContextConfiguration)
      throws ConfigurationException {

    if (getBoolean(FORCE_PARSE_CONFIG_XMLS_ON_DEPLOYMENT_PROPERTY)) {
      return fallback.createArtifactContext(artifactContextConfiguration);
    }

    if (!primary.check(artifactContextConfiguration)) {
      return fallback.createArtifactContext(artifactContextConfiguration);
    }

    try {
      ArtifactContext createdArtifactContext = primary.createArtifactContext(artifactContextConfiguration);
      LOGGER.debug("Successfully created context with " + primary.toString() + " for deployment "
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

      try {
        return fallback.createArtifactContext(artifactContextConfiguration);
      } catch (ConfigurationException eFallback) {
        eFallback.addSuppressed(e);
        throw eFallback;
      }
    }
  }
}
