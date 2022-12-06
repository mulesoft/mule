/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.currentElemement;
import static org.mule.runtime.ast.api.validation.ValidationResultItem.create;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.message.ErrorType;
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
public class ErrorHandlerOnErrorTypeExists extends AbstractErrorTypesValidation {

  public ErrorHandlerOnErrorTypeExists(Optional<FeatureFlaggingService> featureFlaggingService,
                                       boolean ignoreParamsWithProperties) {
    super(featureFlaggingService, ignoreParamsWithProperties);
  }

  @Override
  public String getName() {
    return "Error Type references exist";
  }

  @Override
  public String getDescription() {
    return "Referenced error types do exist in the context of the artifact.";
  }

  @Override
  public Predicate<List<ComponentAst>> applicable() {
    return currentElemement(comp -> (comp.getIdentifier().equals(ON_ERROR_IDENTIFIER)
        || comp.getIdentifier().equals(ON_ERROR_PROPAGATE_IDENTIFIER)
        || comp.getIdentifier().equals(ON_ERROR_CONTINUE_IDENTIFIER))
        && isErrorTypePresent(comp));
  }

  @Override
  public Optional<ValidationResultItem> validate(ComponentAst onErrorComponent, ArtifactAst artifact) {
    final ComponentParameterAst errorTypeParam = getErrorTypeParam(onErrorComponent);
    for (String type : errorTypeParam.getResolvedRawValue().split(",")) {
      final ComponentIdentifier parsedErrorType = parseErrorType(type.trim());

      if ("*".equals(parsedErrorType.getNamespace()) || "*".equals(parsedErrorType.getName())) {
        // skip validation for matchers with wildcards
        return empty();
      }

      final Optional<ErrorType> errorType = artifact.getErrorTypeRepository().lookupErrorType(parsedErrorType);
      if (!errorType.isPresent()) {
        return of(create(onErrorComponent, errorTypeParam, this, format("Could not find error '%s'", type.trim())));
      }
    }

    return empty();
  }

}
