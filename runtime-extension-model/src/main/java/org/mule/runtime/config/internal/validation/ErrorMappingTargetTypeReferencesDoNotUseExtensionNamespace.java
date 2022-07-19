/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.ERROR_MAPPINGS;
import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.currentElemement;
import static org.mule.runtime.ast.api.validation.Validation.Level.ERROR;
import static org.mule.runtime.ast.api.validation.ValidationResultItem.create;
import static org.mule.runtime.extension.api.ExtensionConstants.ERROR_MAPPINGS_PARAMETER_NAME;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toSet;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.ast.api.validation.ValidationResultItem;
import org.mule.runtime.extension.api.error.ErrorMapping;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Referenced error types do not use extension namespaces
 */
public class ErrorMappingTargetTypeReferencesDoNotUseExtensionNamespace extends AbstractErrorTypesValidation {

  private static final Collection<String> allowedBorrowedNamespaces = new HashSet<>(3);

  static {
    allowedBorrowedNamespaces.add("THIS");
    allowedBorrowedNamespaces.add("MULE");
    allowedBorrowedNamespaces.add("TEST");
  }

  public ErrorMappingTargetTypeReferencesDoNotUseExtensionNamespace(Optional<FeatureFlaggingService> featureFlaggingService) {
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
  public Level getLevel() {
    return ERROR;
  }

  @Override
  public Predicate<List<ComponentAst>> applicable() {
    return currentElemement(AbstractErrorTypesValidation::errorMappingPresent);
  }

  @Override
  public Optional<ValidationResultItem> validate(ComponentAst component, ArtifactAst artifact) {
    final Set<String> alreadyUsedErrorNamespaces = artifact.dependencies().stream()
        .map(d -> d.getXmlDslModel().getPrefix().toUpperCase())
        .collect(toSet());

    ComponentParameterAst mappingsParameter = component.getParameter(ERROR_MAPPINGS, ERROR_MAPPINGS_PARAMETER_NAME);
    List<ErrorMapping> errorMappings = (List<ErrorMapping>) mappingsParameter.getValue().getRight();
    for (ErrorMapping errorMapping : errorMappings) {
      final String errorTypeString = errorMapping.getTarget();
      final ComponentIdentifier errorTypeId = parserErrorType(errorTypeString);

      String namespace = errorTypeId.getNamespace();
      if (alreadyUsedErrorNamespaces.contains(namespace) && !allowedBorrowedNamespaces.contains(namespace)) {
        return of(create(component, mappingsParameter, this,
                         format("Cannot use error type '%s': namespace already exists.", errorTypeString)));
      }
    }

    return empty();
  }

}
