/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.currentElemement;
import static org.mule.runtime.ast.api.util.MuleAstUtils.hasPropertyPlaceholder;
import static org.mule.runtime.ast.api.validation.Validation.Level.WARN;
import static org.mule.runtime.ast.api.validation.ValidationResultItem.create;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.ast.api.validation.ValidationResultItem;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Referenced error types do exist in the context of the artifact
 */
public class ErrorHandlerOnErrorTypeNonPropertyValue extends AbstractErrorValidation {

  private final boolean enabled;

  public ErrorHandlerOnErrorTypeNonPropertyValue(boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  public String getName() {
    return "Error Type references fixed";
  }

  @Override
  public String getDescription() {
    return "Referenced error types are fixed.";
  }

  @Override
  public Level getLevel() {
    return WARN;
  }

  @Override
  public Predicate<List<ComponentAst>> applicable() {
    if (enabled) {
      return currentElemement(comp -> (comp.getIdentifier().equals(ON_ERROR_IDENTIFIER)
          || comp.getIdentifier().equals(ON_ERROR_PROPAGATE_IDENTIFIER)
          || comp.getIdentifier().equals(ON_ERROR_CONTINUE_IDENTIFIER))
          && isErrorTypePresentAndPropertyDependant(comp));
    } else {
      return c -> false;
    }
  }

  @Override
  public Optional<ValidationResultItem> validate(ComponentAst onErrorComponent, ArtifactAst artifact) {
    final ComponentParameterAst errorTypeParam = getErrorTypeParam(onErrorComponent);
    final String errorTypeRawValue = errorTypeParam.getRawValue();

    if (hasPropertyPlaceholder(errorTypeRawValue)) {
      return of(create(onErrorComponent, errorTypeParam, this,
                       "'" + onErrorComponent.getIdentifier().getName() + "' has 'type' '" + errorTypeRawValue
                           + "' which is resolved with a property and may cause the artifact to have different behavior on different environments."));
    } else {
      return empty();
    }
  }

}
