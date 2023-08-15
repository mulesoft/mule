/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
