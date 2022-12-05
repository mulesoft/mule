/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.api.config.MuleRuntimeFeature.ENFORCE_ERROR_TYPES_VALIDATION;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.ERROR_MAPPINGS;
import static org.mule.runtime.ast.api.validation.Validation.Level.ERROR;
import static org.mule.runtime.ast.api.validation.Validation.Level.WARN;
import static org.mule.runtime.ast.api.validation.ValidationResultItem.create;
import static org.mule.runtime.extension.api.ExtensionConstants.ERROR_MAPPINGS_PARAMETER_NAME;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;

import static java.lang.String.format;
import static java.util.Locale.getDefault;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toSet;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.util.IdentifierParsingUtils;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.ast.api.validation.Validation;
import org.mule.runtime.ast.api.validation.ValidationResultItem;
import org.mule.runtime.extension.api.error.ErrorMapping;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Ensures consistent access to the {@link ErrorTypeRepository} from validations.
 */
public abstract class AbstractErrorTypesValidation implements Validation {

  private static final String CORE_ERROR_NAMESPACE = CORE_PREFIX.toUpperCase(getDefault());
  protected static final String RAISE_ERROR = "raise-error";

  protected static final String ON_ERROR = "on-error";
  protected static final String ON_ERROR_PROPAGATE = "on-error-propagate";
  protected static final String ON_ERROR_CONTINUE = "on-error-continue";

  protected static final ComponentIdentifier RAISE_ERROR_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(RAISE_ERROR).build();

  protected static final ComponentIdentifier ON_ERROR_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(ON_ERROR).build();
  protected static final ComponentIdentifier ON_ERROR_PROPAGATE_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(ON_ERROR_PROPAGATE).build();
  protected static final ComponentIdentifier ON_ERROR_CONTINUE_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(ON_ERROR_CONTINUE).build();

  private final Optional<FeatureFlaggingService> featureFlaggingService;
  private final boolean waiveUnresolvedPropertiesOnParams;

  public AbstractErrorTypesValidation(Optional<FeatureFlaggingService> featureFlaggingService,
                                      boolean waiveUnresolvedPropertiesOnParams) {
    this.featureFlaggingService = featureFlaggingService;
    this.waiveUnresolvedPropertiesOnParams = waiveUnresolvedPropertiesOnParams;
  }

  @Override
  public Level getLevel() {
    return featureFlaggingService.map(ffs -> ffs.isEnabled(ENFORCE_ERROR_TYPES_VALIDATION)).orElse(true)
        ? ERROR
        : WARN;
  }

  protected boolean isErrorTypePresent(ComponentAst component) {
    if (waiveUnresolvedPropertiesOnParams) {
      if (getErrorTypeParam(component).getRawValue().contains("${")) {
        return false;
      }
    }

    return !isEmpty(getErrorTypeParam(component).getResolvedRawValue());
  }

  protected boolean isErrorTypePresentAndPropertyDependant(ComponentAst component) {
    String errorTypeString = getErrorTypeParam(component).getRawValue();
    return !isEmpty(errorTypeString)
        && errorTypeString.contains("${");
  }

  protected ComponentParameterAst getErrorTypeParam(ComponentAst component) {
    return component.getParameter(DEFAULT_GROUP_NAME, "type");
  }

  protected boolean errorMappingPresent(ComponentAst operationComponent) {
    if (!operationComponent.getModel(ParameterizedModel.class).isPresent()) {
      return false;
    }
    final ComponentParameterAst errorMappingsParam = getErrorMappingsParameter(operationComponent);
    return errorMappingsParam != null && errorMappingsParam.getValue().getValue().isPresent();
  }

  protected boolean errorMappingSourceNotPropertyDependant(ComponentAst operationComponent) {
    if (waiveUnresolvedPropertiesOnParams) {
      return ((List<ErrorMapping>) getErrorMappingsParameter(operationComponent).getValue().getRight())
          .stream()
          .noneMatch(errorMapping -> errorMapping.getSource().contains("${"));
    }
    return true;
  }

  protected boolean errorMappingTargetNotPropertyDependant(ComponentAst operationComponent) {
    if (waiveUnresolvedPropertiesOnParams) {
      return ((List<ErrorMapping>) getErrorMappingsParameter(operationComponent).getValue().getRight())
          .stream()
          .noneMatch(errorMapping -> errorMapping.getTarget().contains("${"));
    }
    return true;
  }

  protected boolean errorMappingSourcePropertyDependant(ComponentAst operationComponent) {
    return ((List<ErrorMapping>) getErrorMappingsParameter(operationComponent).getValue().getRight())
        .stream()
        .anyMatch(errorMapping -> errorMapping.getSource().contains("${"));
  }

  protected boolean errorMappingTargetPropertyDependant(ComponentAst operationComponent) {
    return ((List<ErrorMapping>) getErrorMappingsParameter(operationComponent).getValue().getRight())
        .stream()
        .anyMatch(errorMapping -> errorMapping.getTarget().contains("${"));
  }

  protected static List<ErrorMapping> getErrorMappings(ComponentAst component) {
    return (List<ErrorMapping>) getErrorMappingsParameter(component).getValue().getRight();
  }

  protected static ComponentParameterAst getErrorMappingsParameter(ComponentAst component) {
    return component.getParameter(ERROR_MAPPINGS, ERROR_MAPPINGS_PARAMETER_NAME);
  }

  protected static Optional<ValidationResultItem> validateErrorTypeId(ComponentAst component, ComponentParameterAst parameter,
                                                                      ArtifactAst artifact,
                                                                      Validation validation, String errorTypeString,
                                                                      final ComponentIdentifier errorTypeId) {
    final Optional<ErrorType> errorType = artifact.getErrorTypeRepository().lookupErrorType(errorTypeId);

    if (!errorType.isPresent()) {
      if (CORE_ERROR_NAMESPACE.equals(errorTypeId.getNamespace())) {
        return of(create(component, parameter, validation,
                         format("There's no MULE error named '%s'.", errorTypeId.getName())));
      } else {
        return of(create(component, parameter, validation,
                         format("Could not find error '%s'.", errorTypeId.getName())));
      }
    }

    return empty();
  }

  protected static Optional<ErrorType> lookup(ComponentAst component, ArtifactAst artifact) {
    return artifact.getErrorTypeRepository()
        .lookupErrorType(parseErrorType(component.getParameter(ERROR_MAPPINGS, "type").getResolvedRawValue()));
  }

  protected static ComponentIdentifier parseErrorType(String stringRepresentation) {
    return IdentifierParsingUtils.parseErrorType(stringRepresentation, CORE_ERROR_NAMESPACE);
  }

  protected static boolean isAllowedBorrowedNamespace(String namespace) {
    return CORE_ERROR_NAMESPACE.equals(namespace);
  }

  protected static Set<String> getAlreadyUsedErrorNamespaces(ArtifactAst artifact) {
    return artifact.dependencies().stream()
        .map(dependency -> dependency.getXmlDslModel().getPrefix().toUpperCase(getDefault()))
        .collect(toSet());
  }
}
