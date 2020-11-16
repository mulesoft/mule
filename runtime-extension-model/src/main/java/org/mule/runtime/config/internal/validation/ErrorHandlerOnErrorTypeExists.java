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

import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Referenced error types do exist in the context of the artifact
 */
public class ErrorHandlerOnErrorTypeExists extends AbstractErrorTypesValidation {

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
    return currentElemement(comp -> (comp.getIdentifier().equals(ON_ERROR_IDENTIFIER)
        || comp.getIdentifier().equals(ON_ERROR_PROPAGATE_IDENTIFIER)
        || comp.getIdentifier().equals(ON_ERROR_CONTINUE_IDENTIFIER))
        && comp.getParameter("type") != null
        && comp.getParameter("type").getResolvedRawValue() != null);
  }

  @Override
  public Optional<String> validate(ComponentAst onErrorModel, ArtifactAst artifact) {
    final Optional<ErrorType> errorType = lookup(onErrorModel, "type", artifact);
    if (!errorType.isPresent()) {
      return of(format("Could not find error '%s' used in %s", errorType, compToLoc(onErrorModel)));
    }

    return empty();
  }

  private String compToLoc(ComponentAst component) {
    return "[" + component.getMetadata().getFileName().orElse("unknown") + ":"
        + component.getMetadata().getStartLine().orElse(-1) + "]";
  }

}
