/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.currentElemement;
import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.equalsIdentifier;
import static org.mule.runtime.ast.api.validation.Validation.Level.ERROR;
import static org.mule.runtime.ast.api.validation.ValidationResultItem.create;

import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.ast.api.validation.ValidationResultItem;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Referenced error types cannot be empty or null
 */
public class RaiseErrorTypeReferencesPresent extends AbstractErrorTypesValidation {

  private static final String TYPE_PARAMETER_VALIDATION_ERROR = "type cannot be an empty string or null ";

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
    final ComponentParameterAst errorTypeParam = component.getParameter("type");
    if (errorTypeParam == null) {
      return of(create(component, emptyList(), this, TYPE_PARAMETER_VALIDATION_ERROR));
    }

    final String errorTypeString = errorTypeParam.getResolvedRawValue();
    if (isEmpty(errorTypeString)) {
      return of(create(component, errorTypeParam, this, TYPE_PARAMETER_VALIDATION_ERROR));
    }

    return empty();
  }

}
