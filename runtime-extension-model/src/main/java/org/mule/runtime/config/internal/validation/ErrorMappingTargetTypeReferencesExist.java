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
import org.mule.runtime.ast.api.validation.ValidationResultItem;
import org.mule.runtime.extension.api.error.ErrorMapping;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Referenced error types do exist in the context of the artifact
 */
public class ErrorMappingTargetTypeReferencesExist extends AbstractErrorTypesValidation {

  public ErrorMappingTargetTypeReferencesExist(Optional<FeatureFlaggingService> featureFlaggingService,
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
  public Level getLevel() {
    return ERROR;
  }

  @Override
  public Predicate<List<ComponentAst>> applicable() {
    return currentElemement(((Predicate<ComponentAst>) this::errorMappingPresent)
        .and(comp -> isIgnoreParamsWithProperties()
            ? errorMappingTargetNotPropertyDependant(comp)
            : true));
  }

  @Override
  public Optional<ValidationResultItem> validate(ComponentAst component, ArtifactAst artifact) {
    final Set<String> errorNamespaces = artifact.dependencies().stream()
        .map(d -> d.getXmlDslModel().getPrefix().toUpperCase())
        .collect(toSet());

    for (ErrorMapping errorMapping : getErrorMappings(component)) {
      final String errorTypeString = errorMapping.getTarget();
      final ComponentIdentifier errorTypeId = parseErrorType(errorTypeString);

      if (errorNamespaces.contains(errorTypeId.getNamespace())) {
        return validateErrorTypeId(component, component.getParameter(ERROR_MAPPINGS, ERROR_MAPPINGS_PARAMETER_NAME), artifact,
                                   this,
                                   errorMapping.getTarget(), errorTypeId);
      } else if (artifact.getParent()
          .map(p -> p.getErrorTypeRepository().getErrorNamespaces().contains(errorTypeId.getNamespace()))
          .orElse(false)) {
        return of(create(component, component.getParameter(ERROR_MAPPINGS, ERROR_MAPPINGS_PARAMETER_NAME), this,
                         format("Cannot use error type '%s': namespace already exists.", errorMapping.getTarget())));
      }
    }

    return empty();
  }

}
