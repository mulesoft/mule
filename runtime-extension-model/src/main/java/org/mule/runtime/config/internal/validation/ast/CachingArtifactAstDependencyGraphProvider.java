/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.internal.validation.ast;

import static com.github.benmanes.caffeine.cache.Caffeine.newBuilder;

import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.graph.api.ArtifactAstDependencyGraph;
import org.mule.runtime.ast.graph.api.ArtifactAstDependencyGraphFactory;
import org.mule.runtime.ast.graph.api.ArtifactAstDependencyGraphProvider;

import com.github.benmanes.caffeine.cache.Cache;


/**
 * An {@link ArtifactAstDependencyGraphProvider} that caches the {@link ArtifactAstDependencyGraphProvider}.
 *
 * TODO W-13931931: verify if it makes sense to have a bean in the registry to be used apart from the use case for AST validations
 * in W-12421187
 *
 * @since 4.6.0
 */
public class CachingArtifactAstDependencyGraphProvider implements ArtifactAstDependencyGraphProvider {

  private Cache<ArtifactAst, ArtifactAstDependencyGraph> dependencyGraphCache = newBuilder().build();

  @Override
  public ArtifactAstDependencyGraph get(ArtifactAst fullArtifactAst) {
    return dependencyGraphCache.get(fullArtifactAst, ArtifactAstDependencyGraphFactory::generateFor);
  }
}
