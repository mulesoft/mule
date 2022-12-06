/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.ERROR_MAPPINGS;
import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.currentElemement;
import static org.mule.runtime.ast.api.validation.Validation.Level.WARN;
import static org.mule.runtime.ast.api.validation.ValidationResultItem.create;
import static org.mule.runtime.extension.api.ExtensionConstants.ERROR_MAPPINGS_PARAMETER_NAME;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.validation.ValidationResultItem;
import org.mule.runtime.extension.api.error.ErrorMapping;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Referenced error types do exist in the context of the artifact
 */
public class ErrorMappingSourceTypeReferencesNonPropertyValue extends AbstractErrorValidation {

  private final boolean enabled;

  public ErrorMappingSourceTypeReferencesNonPropertyValue(boolean enabled) {
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
      return currentElemement(((Predicate<ComponentAst>) this::errorMappingPresent));
    } else {
      return c -> false;
    }
  }

  @Override
  public Optional<ValidationResultItem> validate(ComponentAst component, ArtifactAst artifact) {
    for (ErrorMapping errorMapping : getErrorMappings(component)) {
      final String errorTypeRawValue = errorMapping.getSource();

      if (errorTypeRawValue.contains("${")) {
        return of(create(component,
                         component.getParameter(ERROR_MAPPINGS, ERROR_MAPPINGS_PARAMETER_NAME),
                         this,
                         "'" + component.getIdentifier().getName() + "' has 'type' '" + errorTypeRawValue
                             + "' which is resolved with a property and may cause the artifact to have different behavior on different environments."));
      } else {
        return empty();
      }
    }

    return empty();
  }

}
