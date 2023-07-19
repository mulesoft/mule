/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.deployment.internal.processor;

import org.mule.runtime.deployment.model.api.artifact.ArtifactConfigurationProcessor;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContextConfiguration;

/**
 * Allows to check if a {@link ArtifactConfigurationProcessor} can be used for a given artifact.
 * 
 * @since 4.5
 */
interface FallbackAllowedArtifactConfigurationProcessor extends ArtifactConfigurationProcessor {

  /**
   * @return {@code true} if this {@link ArtifactConfigurationProcessor} implementation can handle the artifact for the provided
   *         {@code artifactContextConfiguration}.
   */
  boolean check(ArtifactContextConfiguration artifactContextConfiguration);

}
