/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
