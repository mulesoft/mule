/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.currentElemement;
import static org.mule.runtime.ast.api.validation.Validation.Level.ERROR;
import static org.mule.runtime.extension.api.ExtensionConstants.ERROR_MAPPINGS_PARAMETER_NAME;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.extension.api.error.ErrorMapping;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Referenced error types do exist in the context of the artifact
 */
public class ErrorMappingSourceTypeReferencesExist extends AbstractErrorTypesValidation {

  @Override
  public String getName() {
    return "Error Type references exist";
  }

  @Override
  public String getDescription() {
    return "Referenced error types do exist in the context of the artifact.";
  }

  @Override
  public Level getLevel() {
    return ERROR;
  }

  @Override
  public Predicate<List<ComponentAst>> applicable() {
    return currentElemement(comp -> {
      final ComponentParameterAst errorMappingsAst = comp.getParameter(ERROR_MAPPINGS_PARAMETER_NAME);
      if (errorMappingsAst == null) {
        return false;
      }
      return errorMappingsAst != null && !((List<ErrorMapping>) errorMappingsAst.getValue().getRight()).isEmpty();
    });
  }

  @Override
  public Optional<String> validate(ComponentAst component, ArtifactAst artifact) {
    List<ErrorMapping> errorMappings =
        (List<ErrorMapping>) component.getParameter(ERROR_MAPPINGS_PARAMETER_NAME).getValue().getRight();

    for (ErrorMapping errorMapping : errorMappings) {
      final ComponentIdentifier errorTypeId = parserErrorType(errorMapping.getSource());

      final Optional<ErrorType> errorType = artifact.getErrorTypeRepository().lookupErrorType(errorTypeId);
      if (!errorType.isPresent()) {
        return of(format("Could not find error '%s' used in %s", errorMapping.getSource(), compToLoc(component)));
      }
    }

    return empty();
  }

  private String compToLoc(ComponentAst component) {
    return "[" + component.getMetadata().getFileName().orElse("unknown") + ":"
        + component.getMetadata().getStartLine().orElse(-1) + "]";
  }

}
