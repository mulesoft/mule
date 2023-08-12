/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.internal.validation;

import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.graph.api.ArtifactAstDependencyGraph;
import org.mule.runtime.ast.graph.api.ArtifactAstDependencyGraphProvider;

import static org.mule.runtime.ast.graph.api.ArtifactAstDependencyGraphFactory.generateFor;


public class DefaultArtifactAstDependencyGraphProvider implements ArtifactAstDependencyGraphProvider {

  @Override
  public ArtifactAstDependencyGraph get(ArtifactAst fullArtifactAst) {
    return generateFor(fullArtifactAst);
  }
}
