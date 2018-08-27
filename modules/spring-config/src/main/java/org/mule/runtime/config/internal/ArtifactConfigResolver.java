/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import org.mule.runtime.config.api.dsl.processor.ArtifactConfig;

/**
 * Resolves the {@link ArtifactConfig} for the given {@link ArtifactConfigResolverContext context}.
 *
 * @since 4.1.4, 4.2.0
 */
public interface ArtifactConfigResolver {

  /**
   * Resolves an {@link ArtifactConfig} by parsing the loading all the configuration files and the imported ones.
   *
   * @param context the context to be used when resolving the artifact config.
   * @return {@link ArtifactConfig}
   */
  ArtifactConfig resolveArtifactConfig(ArtifactConfigResolverContext context);

}
