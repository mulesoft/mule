/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.classloading;

import static org.mule.runtime.ast.api.validation.Validation.Level.ERROR;
import static org.mule.runtime.ast.api.validation.ValidationResultItem.create;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.validation.Validation;
import org.mule.runtime.ast.api.validation.ValidationResultItem;
import org.mule.runtime.ast.graph.api.ArtifactAstDependencyGraphProvider;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class ArtifactAstDependencyGraphProviderValidation implements Validation {

  private final ArtifactAstDependencyGraphProvider artifactAstDependencyGraphProvider;

  public ArtifactAstDependencyGraphProviderValidation(ArtifactAstDependencyGraphProvider artifactAstDependencyGraphProvider) {
    this.artifactAstDependencyGraphProvider = artifactAstDependencyGraphProvider;
  }

  @Override
  public String getName() {
    return "ArtifactAstDependencyGraphProvider Validation";
  }

  @Override
  public String getDescription() {
    return "Verify that the validation receives an ArtifactAstDependencyGraphProvider";
  }

  @Override
  public Level getLevel() {
    return ERROR;
  }

  @Override
  public Predicate<List<ComponentAst>> applicable() {
    return comp -> true;
  }

  @Override
  public Optional<ValidationResultItem> validate(ComponentAst component, ArtifactAst artifact) {
    if (artifactAstDependencyGraphProvider == null) {
      return of(create(component, this, "artifactAstDependencyGraphProvider was not set"));
    }

    return empty();
  }
}
