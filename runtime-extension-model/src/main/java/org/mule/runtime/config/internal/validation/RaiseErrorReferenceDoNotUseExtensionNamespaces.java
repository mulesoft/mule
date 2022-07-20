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
import static org.mule.runtime.ast.api.validation.ValidationResultItem.create;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.config.FeatureFlaggingService;
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
 * Referenced error types do not use extension namespaces
 */
public class RaiseErrorReferenceDoNotUseExtensionNamespaces extends AbstractErrorTypesValidation {

  public RaiseErrorReferenceDoNotUseExtensionNamespaces(Optional<FeatureFlaggingService> featureFlaggingService) {
    super(featureFlaggingService);
  }

  @Override
  public String getName() {
    return "Error Type references don't use extension namespaces";
  }

  @Override
  public String getDescription() {
    return "Referenced error types do not use extension namespaces.";
  }

  @Override
  public Predicate<List<ComponentAst>> applicable() {
    return currentElemement(equalsIdentifier(RAISE_ERROR_IDENTIFIER)
        // there is already another validation for the presence of this param
        .and(component -> !StringUtils.isEmpty(getErrorTypeParam(component).getResolvedRawValue())));
  }

  @Override
  public Optional<ValidationResultItem> validate(ComponentAst component, ArtifactAst artifact) {
    final ComponentParameterAst errorTypeParam = getErrorTypeParam(component);
    final String errorTypeString = errorTypeParam.getResolvedRawValue();
    final Set<String> alreadyUsedErrorNamespaces = getAlreadyUsedErrorNamespaces(artifact);
    final ComponentIdentifier errorTypeId = parserErrorType(errorTypeString);

    String namespace = errorTypeId.getNamespace();
    if (alreadyUsedErrorNamespaces.contains(namespace) && !isAllowedBorrowedNamespace(namespace)) {
      return of(create(component, errorTypeParam, this,
                       format("Cannot use error type '%s': namespace already exists.", errorTypeString)));
    }

    return empty();
  }

  private static ComponentParameterAst getErrorTypeParam(ComponentAst component) {
    return component.getParameter(DEFAULT_GROUP_NAME, "type");
  }
}
