/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
