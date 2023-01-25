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
import org.mule.runtime.core.api.config.ConfigurationException;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Aids with the generation of filtered artifact ASTs in the context of lazy initialization of components.
 */
class ComponentInitializationArtifactAstGenerator {

  /**
   * A callback for performing validations over the minimal artifact AST.
   * <p>
   * Unlike a regular {@link Consumer}, this one can throw a {@link ConfigurationException}.
   */
  @FunctionalInterface
  public interface ArtifactAstValidator {

    void validate(ArtifactAst artifactAst) throws ConfigurationException;
  }

  private final ArtifactAstDependencyGraph graph;

  /**
   * Creates the generator instance.
   *
   * @param graph The {@link ArtifactAstDependencyGraph} to use for extracting the minimal AST from a predicate.
   */
  ComponentInitializationArtifactAstGenerator(ArtifactAstDependencyGraph graph) {
    this.graph = graph;
  }

  /**
   * Creates the minimal artifact AST that satisfies the given {@code componentInitializationRequest} and taking into account the
   * current {@code componentInitializationState}.
   * <p>
   * A minimal artifact AST in this context is the one that includes the requested components and all of their dependencies but
   * excluding any that may be already initialized.
   *
   * @param componentInitializationRequest The {@link ComponentInitializationRequest} to satisfy.
   * @param componentInitializationState   The current {@link ComponentInitializationState} to take into account.
   * @param astValidator                   A validator to call with the minimal artifact AST. Already initialized components are
   *                                       still included at this stage.
   * @return The minimal (and potentially also filtered) artifact AST.
   * @throws ConfigurationException If the validation fails.
   */
  public ArtifactAst getMinimalArtifactAst(ComponentInitializationRequest componentInitializationRequest,
                                           ComponentInitializationState componentInitializationState,
                                           ArtifactAstValidator astValidator)
      throws ConfigurationException {
    Predicate<ComponentAst> minimalApplicationFilter = getFilterForMinimalArtifactAst(componentInitializationRequest);
    ArtifactAst minimalAst = buildAndValidateMinimalApplicationModel(minimalApplicationFilter, astValidator);
    return filterResultingMinimalAst(minimalAst, componentInitializationRequest, componentInitializationState);
  }

  private ArtifactAst buildAndValidateMinimalApplicationModel(Predicate<ComponentAst> basePredicate,
                                                              ArtifactAstValidator astValidator)
      throws ConfigurationException {
    final ArtifactAst minimalApplicationModel = buildMinimalApplicationModel(basePredicate);
    astValidator.validate(minimalApplicationModel);
    return minimalApplicationModel;
  }

  private ArtifactAst buildMinimalApplicationModel(Predicate<ComponentAst> basePredicate) {
    return graph.minimalArtifactFor(basePredicate);
  }

  private Predicate<ComponentAst> getFilterForMinimalArtifactAst(ComponentInitializationRequest componentInitializationRequest) {
    if (componentInitializationRequest.isKeepPreviousRequested()) {
      return componentInitializationRequest.getComponentFilter();
    } else {
      return componentInitializationRequest.getComponentFilter().or(this::isAlwaysEnabledComponent);
    }
  }

  private ArtifactAst filterResultingMinimalAst(ArtifactAst artifactAst,
                                                ComponentInitializationRequest componentInitializationRequest,
                                                ComponentInitializationState componentInitializationState) {
    if (componentInitializationRequest.isKeepPreviousRequested()) {
      return new FilteredArtifactAst(artifactAst, comp -> !componentInitializationState.isComponentAlreadyInitialized(comp));
    } else {
      return artifactAst;
    }
  }

  private boolean isAlwaysEnabledComponent(ComponentAst componentAst) {
    return componentAst.getModel(HasStereotypeModel.class)
        .map(stm -> stm.getStereotype() != null && stm.getStereotype().isAssignableTo(APP_CONFIG))
        .orElse(false);
  }
}
