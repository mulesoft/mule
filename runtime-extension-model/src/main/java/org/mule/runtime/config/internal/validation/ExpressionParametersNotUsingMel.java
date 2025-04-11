/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.currentElemement;
import static org.mule.runtime.ast.api.validation.Validation.Level.ERROR;
import static org.mule.runtime.ast.api.validation.ValidationResultItem.create;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.getGroupAndParametersPairs;

import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.ast.api.validation.Validation;
import org.mule.runtime.ast.api.validation.ValidationResultItem;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Validates that no stale {@code mel} usages exist in expressions.
 *
 * @since 4.7
 */
public class ExpressionParametersNotUsingMel implements Validation {

  @Override
  public String getName() {
    return "Expression has 'mel:' prefix";
  }

  @Override
  public String getDescription() {
    return "Expression has 'mel:' prefix";
  }

  @Override
  public Predicate<List<ComponentAst>> applicable() {
    return currentElemement(c -> c.getModel(ParameterizedModel.class)
        .map(pmzd -> pmzd.getParameterGroupModels().stream()
            .anyMatch(pmg -> pmg.getParameterModels().stream()
                .anyMatch(pm -> {
                  ComponentParameterAst param = c.getParameter(pmg.getName(), pm.getName());
                  return param != null && param.getValue().isLeft();
                })))
        .orElse(false));
  }

  @Override
  public List<ValidationResultItem> validateMany(ComponentAst component, ArtifactAst artifact) {
    return component.getModel(ParameterizedModel.class)
        .map(pmz -> getGroupAndParametersPairs(pmz)
            .map(gnp -> component.getParameter(gnp.getFirst().getName(), gnp.getSecond().getName()))
            .filter(param -> param != null && param.getValue().isLeft())
            .flatMap(param -> validateNoMelExpression(component, param, param.getValue().getLeft())
                .map(Stream::of).orElseGet(Stream::empty))
            .collect(toList()))
        .orElse(emptyList());
  }

  protected final Optional<ValidationResultItem> validateNoMelExpression(ComponentAst component, ComponentParameterAst param,
                                                                         String expression) {
    if (expression.startsWith("mel:")) {
      return of(create(component, param, this, "MEL expressions are no longer supported."));
    } else {
      return empty();
    }
  }

  @Override
  public Level getLevel() {
    return ERROR;
  }

}
