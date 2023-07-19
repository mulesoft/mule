/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.currentElemement;
import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.equalsIdentifier;
import static org.mule.runtime.ast.api.validation.Validation.Level.ERROR;
import static org.mule.runtime.ast.api.validation.ValidationResultItem.create;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.ast.api.validation.ValidationResultItem;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;

/**
 * Referenced error types cannot be empty or null
 */
public class RaiseErrorTypeReferencesPresent extends AbstractErrorTypesValidation {

  public RaiseErrorTypeReferencesPresent(Optional<FeatureFlaggingService> featureFlaggingService) {
    super(featureFlaggingService, false);
  }

  @Override
  public String getName() {
    return "Error Type references present";
  }

  @Override
  public String getDescription() {
    return "Referenced error types cannot be empty or null.";
  }

  @Override
  public Level getLevel() {
    return ERROR;
  }

  @Override
  public Predicate<List<ComponentAst>> applicable() {
    return currentElemement(equalsIdentifier(RAISE_ERROR_IDENTIFIER));
  }

  @Override
  public Optional<ValidationResultItem> validate(ComponentAst component, ArtifactAst artifact) {
    final ComponentParameterAst errorTypeParam = getErrorTypeParam(component);
    final String errorTypeString = errorTypeParam.getResolvedRawValue();

    if (StringUtils.isEmpty(errorTypeString)) {
      return of(create(component, errorTypeParam, this, "type cannot be an empty string or null "));
    } else {
      return empty();
    }
  }

}
