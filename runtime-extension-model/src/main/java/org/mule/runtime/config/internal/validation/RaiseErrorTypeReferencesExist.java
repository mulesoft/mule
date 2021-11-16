/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.currentElemement;
import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.equalsIdentifier;
import static org.mule.runtime.ast.api.validation.Validation.Level.WARN;
import static org.mule.runtime.ast.api.validation.ValidationResultItem.create;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toSet;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.ast.api.validation.ValidationResultItem;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;

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
    return WARN;
  }

  @Override
  public Predicate<List<ComponentAst>> applicable() {
    return currentElemement(equalsIdentifier(RAISE_ERROR_IDENTIFIER)
        // there is already another validation for the presence of this param
        .and(component -> !StringUtils.isEmpty(component.getParameter(DEFAULT_GROUP_NAME, "type").getResolvedRawValue())));
  }

  @Override
  public Optional<ValidationResultItem> validate(ComponentAst component, ArtifactAst artifact) {
    final ComponentParameterAst errorTypeParam = component.getParameter(DEFAULT_GROUP_NAME, "type");
    final String errorTypeString = errorTypeParam.getResolvedRawValue();

    final Set<String> errorNamespaces = artifact.dependencies().stream()
        .map(d -> d.getXmlDslModel().getPrefix().toUpperCase())
        .collect(toSet());

    final ComponentIdentifier errorTypeId = parserErrorType(errorTypeString);

    if (errorNamespaces.contains(errorTypeId.getNamespace())) {
      return validateErrorTypeId(component, errorTypeParam, artifact, this, errorTypeString, errorTypeId);
    } else if (artifact.getParent()
        .map(p -> p.getErrorTypeRepository().getErrorNamespaces().contains(errorTypeId.getNamespace()))
        .orElse(false)) {
      return of(create(component, errorTypeParam, this,
                       format("Cannot use error type '%s': namespace already exists.", errorTypeString)));
    }

    return empty();
  }

}
