/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.currentElemement;
import static org.mule.runtime.ast.api.validation.Validation.Level.ERROR;

import static java.util.Collections.emptyList;
import static java.util.Optional.empty;

import org.mule.runtime.api.el.ExpressionLanguage;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.validation.Validation;
import org.mule.runtime.ast.api.validation.ValidationResultItem;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Expressions in params must be syntactically valid.
 */
public class ExpressionsAreSyntacticallyValid implements Validation {

  private final ExpressionLanguage expressionManager;

  public ExpressionsAreSyntacticallyValid(ExpressionLanguage expressionManager) {
    this.expressionManager = expressionManager;
  }

  @Override
  public String getName() {
    return "Expressions in params must be syntactically valid";
  }

  @Override
  public String getDescription() {
    return "Valid expressions are provided for parameters.";
  }

  @Override
  public Level getLevel() {
    return ERROR;
  }

  @Override
  public Predicate<List<ComponentAst>> applicable() {
    return currentElemement(component -> component.getModel(ParameterizedModel.class)
        .map(pmz -> pmz.getAllParameterModels())
        .orElse(emptyList())
        .stream()
        .filter(pm -> !NOT_SUPPORTED.equals(pm.getExpressionSupport()))
        .findAny()
        .isPresent());
  }

  @Override
  public Optional<ValidationResultItem> validate(ComponentAst component, ArtifactAst artifact) {
    return empty();
    // return component.getModel(ParameterizedModel.class)
    // .map(pmz -> pmz.getAllParameterModels())
    // .orElse(emptyList())
    // .stream()
    // .filter(pm -> !NOT_SUPPORTED.equals(pm.getExpressionSupport()))
    // .map(pm -> component.getParameter(pm.getName()))
    // .filter(param -> {
    // if (param.getValue().isLeft()) {
    // String expr = param.getValue().getLeft();
    // if (!expressionManager.isValid(expr)) {
    // return true;
    // }
    // }
    // return false;
    // })
    // .map(param -> create(component, param, this,
    // format("The expression ('%s') given for parameter '%s' is not valid",
    // param.getRawValue(), param.getModel().getName())))
    // .findFirst();
  }

}
