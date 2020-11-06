/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.currentElemement;
import static org.mule.runtime.ast.api.validation.Validation.Level.ERROR;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.ANY_IDENTIFIER;
import static org.mule.runtime.extension.api.ExtensionConstants.ERROR_MAPPINGS_PARAMETER_NAME;

import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.validation.Validation;
import org.mule.runtime.extension.api.error.ErrorMapping;
import org.mule.runtime.extension.internal.property.NoErrorMappingModelProperty;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Mapping for 'ANY' or empty is not repeated.
 */
public class SourceErrorMappingAnyNotRepeated implements Validation {

  @Override
  public String getName() {
    return "Source error-mapping ANY not repeated";
  }

  @Override
  public String getDescription() {
    return "Mapping for 'ANY' or empty is not repeated.";
  }

  @Override
  public Level getLevel() {
    return ERROR;
  }

  @Override
  public Predicate<List<ComponentAst>> applicable() {
    return currentElemement(component -> component.getModel(OperationModel.class)
        .map(opModel -> !opModel.getModelProperty(NoErrorMappingModelProperty.class).isPresent()
            && component.getParameter(ERROR_MAPPINGS_PARAMETER_NAME) != null)
        .orElse(false));
  }

  @Override
  public Optional<String> validate(ComponentAst component, ArtifactAst artifact) {
    return component.getParameter(ERROR_MAPPINGS_PARAMETER_NAME).<List<ErrorMapping>>getValue()
        .reduce(l -> empty(),
                mappings -> {
                  if (mappings.stream()
                      .filter(this::isErrorMappingWithSourceAny)
                      .count() > 1) {
                    return of("Only one mapping for 'ANY' or an empty source type is allowed.");
                  } else {
                    return empty();
                  }
                });
  }

  private boolean isErrorMappingWithSourceAny(ErrorMapping model) {
    return ANY_IDENTIFIER.equals(model.getSource());
  }

}
