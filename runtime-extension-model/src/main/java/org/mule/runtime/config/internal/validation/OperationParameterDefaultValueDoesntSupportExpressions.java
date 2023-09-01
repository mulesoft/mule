/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.currentElemement;
import static org.mule.runtime.ast.api.validation.Validation.Level.ERROR;
import static org.mule.runtime.ast.api.validation.ValidationResultItem.create;
import static org.mule.runtime.core.api.extension.provider.MuleOperationExtensionModelDeclarer.OPERATION_NAMESPACE;
import static org.mule.runtime.core.api.extension.provider.MuleOperationExtensionModelDeclarer.OPTIONAL_PARAMETER;
import static org.mule.runtime.core.internal.expression.util.ExpressionUtils.isExpression;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.ast.api.validation.Validation;
import org.mule.runtime.ast.api.validation.ValidationResultItem;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Expressions are forbidden for optional parameter default values.
 *
 * @since 4.5
 */
public class OperationParameterDefaultValueDoesntSupportExpressions implements Validation {

  private static final ComponentIdentifier OPTIONAL_PARAMETER_IDENTIFIER =
      builder().namespace(OPERATION_NAMESPACE).name(OPTIONAL_PARAMETER).build();

  @Override
  public String getName() {
    return "No expressions in optional parameter default value";
  }

  @Override
  public String getDescription() {
    return "Expressions are forbidden for optional parameter default values";
  }

  @Override
  public Level getLevel() {
    return ERROR;
  }

  @Override
  public Predicate<List<ComponentAst>> applicable() {
    return currentElemement(component -> component.getIdentifier().equals(OPTIONAL_PARAMETER_IDENTIFIER));
  }

  @Override
  public Optional<ValidationResultItem> validate(ComponentAst component, ArtifactAst artifact) {
    ComponentParameterAst defaultValueAst = component.getParameter(DEFAULT_GROUP_NAME, "defaultValue");
    Optional<String> defaultValue = defaultValueAst.getValue().getValue();
    if (isExpression(defaultValue.orElse(null))) {
      return validationFailed(component, getParameterName(component));
    } else {
      return validationOk();
    }
  }

  private static String getParameterName(ComponentAst component) {
    return component.getParameter(DEFAULT_GROUP_NAME, "name").getValue().<String>getValue()
        .orElseThrow(() -> new IllegalStateException(format("Parameter in location '%s' doesn't have name",
                                                            component.getLocation())));
  }

  private Optional<ValidationResultItem> validationFailed(ComponentAst component, String name) {
    return of(create(component, this, format("An expression was given for 'defaultValue' of the optional parameter '%s'", name)));
  }

  private static Optional<ValidationResultItem> validationOk() {
    return empty();
  }
}
