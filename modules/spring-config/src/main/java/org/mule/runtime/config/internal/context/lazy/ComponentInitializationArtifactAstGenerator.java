/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.context.lazy;

import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.APP_CONFIG;

import org.mule.runtime.api.meta.model.stereotype.HasStereotypeModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.graph.api.ArtifactAstDependencyGraph;
import org.mule.runtime.ast.internal.FilteredArtifactAst;

import java.util.function.Predicate;

/**
 * Aids with the generation of filtered artifact ASTs in the context of lazy initialization of components.
 */
class ComponentInitializationArtifactAstGenerator {

  private final ArtifactAstDependencyGraph graph;
  private final ComponentInitializationState componentInitializationState;

  /**
   * Creates the generator instance.
   *
   * @param graph                        The {@link ArtifactAstDependencyGraph} to use for extracting the minimal AST from a
   *                                     predicate.
   * @param componentInitializationState The current {@link ComponentInitializationState}.
   */
  ComponentInitializationArtifactAstGenerator(ArtifactAstDependencyGraph graph,
                                              ComponentInitializationState componentInitializationState) {
    this.graph = graph;
    this.componentInitializationState = componentInitializationState;
  }

  /**
   * Creates the minimal artifact AST that satisfies the given {@code componentInitializationRequest}.
   * <p>
   * A minimal artifact AST in this context is the one that includes the requested components and all of their dependencies.
   *
   * @param componentInitializationRequest The {@link ComponentInitializationRequest} to satisfy.
   * @return The minimal artifact AST that satisfies the given {@code componentInitializationRequest}.
   */
  public ArtifactAst getMinimalArtifactAstForRequest(ComponentInitializationRequest componentInitializationRequest) {
    Predicate<ComponentAst> minimalApplicationFilter = getFilterForMinimalArtifactAst(componentInitializationRequest);
    return graph.minimalArtifactFor(minimalApplicationFilter);
  }

  /**
   * @param artifactAst An {@link ArtifactAst}.
   * @return A filtered version of the given {@code artifactAst} where the already initialized components are excluded.
   */
  public ArtifactAst getArtifactAstExcludingAlreadyInitialized(ArtifactAst artifactAst) {
    return new FilteredArtifactAst(artifactAst, comp -> !componentInitializationState.isComponentAlreadyInitialized(comp));
  }

  private static Predicate<ComponentAst> getFilterForMinimalArtifactAst(ComponentInitializationRequest componentInitializationRequest) {
    if (componentInitializationRequest.isKeepPreviousRequested()) {
      return componentInitializationRequest.getComponentFilter();
    } else {
      return componentInitializationRequest.getComponentFilter()
          .or(ComponentInitializationArtifactAstGenerator::isAlwaysEnabledComponent);
    }
  }

  private static boolean isAlwaysEnabledComponent(ComponentAst componentAst) {
    return componentAst.getModel(HasStereotypeModel.class)
        .map(stm -> stm.getStereotype() != null && stm.getStereotype().isAssignableTo(APP_CONFIG))
        .orElse(false);
  }
}
