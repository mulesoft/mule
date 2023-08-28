/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.internal.model;

import org.mule.runtime.ast.api.ComponentAst;

import java.util.function.Predicate;

/**
 * Internal class to be used to initialize {@link ComponentAst} lazily.
 *
 * @since 4.1
 */
public interface ComponentModelInitializer {

  /**
   * Initializes the {@link <ComponentAst> componentModels} that match for the predicate.
   *
   * @param componentModelPredicate a {@link Predicate} for {@link ComponentAst} to be initialized.
   * @param applyStartPhase         boolean indicating if the Start phase should be applied to the created components
   */
  void initializeComponents(Predicate<ComponentAst> componentModelPredicate, boolean applyStartPhase);

}
