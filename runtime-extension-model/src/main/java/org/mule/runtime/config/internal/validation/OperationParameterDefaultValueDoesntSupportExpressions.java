/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
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

import static java.lang.String.format;

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

  private static final String DEFAULT_EXPRESSION_PREFIX = "#[";
  private static final String DEFAULT_EXPRESSION_SUFFIX = "]";

  private static final String OPERATION = "operation";
  private static final String OPTIONAL_PARAMETER = "optional-parameter";
  private static final ComponentIdentifier OPTIONAL_PARAMETER_IDENTIFIER =
      builder().namespace(OPERATION).name(OPTIONAL_PARAMETER).build();

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
    if (isExpression(defaultValue)) {
      return validationFailed(component);
    } else {
      return validationOk();
    }
  }

  private Optional<ValidationResultItem> validationFailed(ComponentAst component) {
    return component.getParameter(DEFAULT_GROUP_NAME, "name").getValue().getValue()
        .map(name -> create(component, this,
                            format("An expression was given for 'defaultValue' of the optional parameter '%s'", name)));
  }

  private static Optional<ValidationResultItem> validationOk() {
    return Optional.empty();
  }

  private static boolean isExpression(Optional<String> defaultValue) {
    if (!defaultValue.isPresent()) {
      return false;
    }

    String stringValue = defaultValue.get();
    return stringValue.startsWith(DEFAULT_EXPRESSION_PREFIX)
        && stringValue.endsWith(DEFAULT_EXPRESSION_SUFFIX);
  }
}
