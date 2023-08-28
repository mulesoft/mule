/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.api;

import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContext;

/**
 * {@link ConfigurationBuilder} specialization that can provide a new {@link ArtifactContextFactory}.
 *
 * @since 4.4
 */
public interface ArtifactContextFactory extends ConfigurationBuilder {

  /**
   *
   * @return a new {@link ArtifactContext} from the state of this implementation.
   */
  ArtifactContext createArtifactContext();

}
