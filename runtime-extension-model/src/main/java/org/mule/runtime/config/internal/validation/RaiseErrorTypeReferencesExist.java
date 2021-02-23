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
import static java.util.stream.Collectors.toSet;
import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.currentElemement;
import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.equalsIdentifier;
import static org.mule.runtime.ast.api.validation.Validation.Level.ERROR;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Referenced error types do exist in the context of the artifact
 */
public class RaiseErrorTypeReferencesExist extends AbstractErrorTypesValidation {

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
    return currentElemement(equalsIdentifier(RAISE_ERROR_IDENTIFIER)
        // there is already another validation for the presence of this param
        .and(component -> component.getParameter("type").getResolvedRawValue() != null));
  }

  @Override
  public Optional<String> validate(ComponentAst component, ArtifactAst artifact) {
    final String errorTypeString = component.getParameter("type").getResolvedRawValue();

    final Set<String> errorNamespaces = artifact.dependencies().stream()
        .map(d -> d.getXmlDslModel().getPrefix().toUpperCase())
        .collect(toSet());

    final ComponentIdentifier errorTypeId = parserErrorType(errorTypeString);

    if (errorNamespaces.contains(errorTypeId.getNamespace())) {
      final Optional<ErrorType> errorType = artifact.getErrorTypeRepository().lookupErrorType(errorTypeId);

      if (errorType.isPresent()) {
        return empty();
      }

      if (CORE_PREFIX.toUpperCase().equals(errorTypeId.getNamespace())) {
        return of(format("There's no MULE error named '%s' in %s.", errorTypeId.getName(), compToLoc(component)));
      } else {
        return of(format("Could not find error '%s' used in %s", errorTypeString, compToLoc(component)));
      }
    } else if (artifact.getParent()
        .map(p -> p.getErrorTypeRepository().getErrorNamespaces().contains(errorTypeId.getNamespace()))
        .orElse(false)) {
      return of(format("Cannot use error type '%s': namespace already exists. Used in %s", errorTypeString,
                       compToLoc(component)));
    }

    return empty();
  }

  private String compToLoc(ComponentAst component) {
    return "[" + component.getMetadata().getFileName().orElse("unknown") + ":"
        + component.getMetadata().getStartLine().orElse(-1) + "]";
  }

}
