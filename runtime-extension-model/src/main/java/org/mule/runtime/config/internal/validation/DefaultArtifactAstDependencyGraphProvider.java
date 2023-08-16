/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.runtime.ast.graph.api.ArtifactAstDependencyGraphFactory.generateFor;

import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.graph.api.ArtifactAstDependencyGraph;
import org.mule.runtime.ast.graph.api.ArtifactAstDependencyGraphProvider;

/**
 * A default implementation for the {@link ArtifactAstDependencyGraphProvider}.
 */
public class DefaultArtifactAstDependencyGraphProvider implements ArtifactAstDependencyGraphProvider {

  @Override
  public ArtifactAstDependencyGraph get(ArtifactAst fullArtifactAst) {
    return generateFor(fullArtifactAst);
  }
}
