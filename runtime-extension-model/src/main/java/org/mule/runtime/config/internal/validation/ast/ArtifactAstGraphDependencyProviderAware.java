/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation.ast;

import org.mule.runtime.ast.graph.api.ArtifactAstDependencyGraphProvider;

/**
 * A class that supports a {@link ArtifactAstDependencyGraphProvider}.
 *
 * @since 1.2.0
 */
public interface ArtifactAstGraphDependencyProviderAware {

  /**
   * sets a {@link ArtifactAstDependencyGraphProvider}.
   *
   * @param ArtifactAstDependencyGraphProvider the {@link ArtifactAstDependencyGraphProvider} to set.
   */
  void setArtifactAstDependencyGraphProvider(ArtifactAstDependencyGraphProvider artifactAstDependencyGraphProvider);
}
