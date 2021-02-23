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
import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.equalsIdentifier;
import static org.mule.runtime.ast.api.validation.Validation.Level.ERROR;

import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;

/**
 * Referenced error types cannot be empty or null
 */
public class RaiseErrorTypeReferencesPresent extends AbstractErrorTypesValidation {

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
  public Optional<String> validate(ComponentAst component, ArtifactAst artifact) {
    final String errorTypeString = component.getParameter("type").getResolvedRawValue();

    if (StringUtils.isEmpty(errorTypeString)) {
      return of("type cannot be an empty string or null in " + compToLoc(component));
    } else {
      return empty();
    }
  }

  private String compToLoc(ComponentAst component) {
    return "[" + component.getMetadata().getFileName().orElse("unknown") + ":"
        + component.getMetadata().getStartLine().orElse(-1) + "]";
  }

}
