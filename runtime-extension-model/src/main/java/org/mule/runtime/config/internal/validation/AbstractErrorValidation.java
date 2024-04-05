/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.ERROR_MAPPINGS;
import static org.mule.runtime.ast.api.util.MuleAstUtils.hasPropertyPlaceholder;
import static org.mule.runtime.extension.api.ExtensionConstants.ERROR_MAPPINGS_PARAMETER_NAME;
import static org.mule.runtime.config.internal.dsl.utils.DslConstants.CORE_PREFIX;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.ast.api.validation.Validation;
import org.mule.runtime.extension.api.error.ErrorMapping;

import java.util.List;

public abstract class AbstractErrorValidation implements Validation {

  protected static final String ON_ERROR = "on-error";
  protected static final String ON_ERROR_PROPAGATE = "on-error-propagate";
  protected static final String ON_ERROR_CONTINUE = "on-error-continue";
  protected static final String RAISE_ERROR = "raise-error";

  protected static final ComponentIdentifier ON_ERROR_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(ON_ERROR).build();
  protected static final ComponentIdentifier ON_ERROR_PROPAGATE_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(ON_ERROR_PROPAGATE).build();
  protected static final ComponentIdentifier ON_ERROR_CONTINUE_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(ON_ERROR_CONTINUE).build();
  protected static final ComponentIdentifier RAISE_ERROR_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(RAISE_ERROR).build();

  protected boolean isErrorTypePresentAndPropertyDependant(ComponentAst component) {
    String errorTypeString = getErrorTypeParam(component).getRawValue();
    return !isEmpty(errorTypeString)
        && hasPropertyPlaceholder(errorTypeString);
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
    return ((List<ErrorMapping>) getErrorMappingsParameter(operationComponent).getValue().getRight())
        .stream()
        .noneMatch(errorMapping -> hasPropertyPlaceholder(errorMapping.getSource()));
  }

  protected boolean errorMappingTargetNotPropertyDependant(ComponentAst operationComponent) {
    return ((List<ErrorMapping>) getErrorMappingsParameter(operationComponent).getValue().getRight())
        .stream()
        .noneMatch(errorMapping -> hasPropertyPlaceholder(errorMapping.getTarget()));
  }

  protected boolean errorMappingSourcePropertyDependant(ComponentAst operationComponent) {
    return ((List<ErrorMapping>) getErrorMappingsParameter(operationComponent).getValue().getRight())
        .stream()
        .anyMatch(errorMapping -> hasPropertyPlaceholder(errorMapping.getSource()));
  }

  protected boolean errorMappingTargetPropertyDependant(ComponentAst operationComponent) {
    return ((List<ErrorMapping>) getErrorMappingsParameter(operationComponent).getValue().getRight())
        .stream()
        .anyMatch(errorMapping -> hasPropertyPlaceholder(errorMapping.getTarget()));
  }

  protected static List<ErrorMapping> getErrorMappings(ComponentAst component) {
    return (List<ErrorMapping>) getErrorMappingsParameter(component).getValue().getRight();
  }

  protected static ComponentParameterAst getErrorMappingsParameter(ComponentAst component) {
    return component.getParameter(ERROR_MAPPINGS, ERROR_MAPPINGS_PARAMETER_NAME);
  }

}
