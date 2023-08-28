/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.internal.validation.ast;

import static org.mule.runtime.ast.graph.api.ArtifactAstDependencyGraphFactory.generateFor;

import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.graph.api.ArtifactAstDependencyGraph;
import org.mule.runtime.ast.graph.api.ArtifactAstDependencyGraphProvider;

/**
 * An {@link ArtifactAstDependencyGraphProvider} that caches the {@link ArtifactAstDependencyGraphProvider}.
 *
 * TODO W-13931931: Create a context for dependencies needed to be injected in deployment in W-12421187
 *
 * @since 4.6.0
 */
public class ReusableArtifactAstDependencyGraphProvider implements ArtifactAstDependencyGraphProvider {

  private final ArtifactAst artifactAst;

  private final ArtifactAstDependencyGraph artifactAstDependencyGraph;

  public ReusableArtifactAstDependencyGraphProvider(ArtifactAst artifactAst) {
    this.artifactAst = artifactAst;
    this.artifactAstDependencyGraph = generateFor(artifactAst);
  }

  @Override
  public ArtifactAstDependencyGraph get(ArtifactAst fullArtifactAst) {
    if (!artifactAst.equals(fullArtifactAst)) {
      throw new IllegalStateException("An incorrect artifactAST was provided for the creation of the graph.");
    }
    return artifactAstDependencyGraph;
  }
}
