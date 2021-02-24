/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.api.meta.ExpressionSupport.REQUIRED;
import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.currentElemement;
import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.equalsIdentifier;
import static org.mule.runtime.ast.api.validation.Validation.Level.ERROR;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.ast.api.validation.Validation;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * No expressions are provided for parameters that do not support expressions.
 */
public class ExpressionsInRequiredExpressionsParams implements Validation {

  private static final String DEFAULT_EXPRESSION_PREFIX = "#[";
  private static final String DEFAULT_EXPRESSION_SUFFIX = "]";
  private static final String CONFIGURATION_NAME = "configuration";

  private static final ComponentIdentifier CONFIGURATION_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(CONFIGURATION_NAME).build();

  @Override
  public String getName() {
    return "Expression must be set in expressionsRequired params";
  }

  @Override
  public String getDescription() {
    return "Expressions are provided for parameters that require expressions.";
  }

  @Override
  public Level getLevel() {
    return ERROR;
  }

  @Override
  public Predicate<List<ComponentAst>> applicable() {
    return currentElemement(component -> component.getModel(ParameterizedModel.class).isPresent())
        // According to the extension model, no collections or target-value for foreach, parallel-foreach, etc...
        // must be defined by an expression, but this was not enforced. This check is needed to avoid breaking on
        // legacy cases
        .and(currentElemement(equalsIdentifier(CONFIGURATION_IDENTIFIER)));
  }

  @Override
  public Optional<String> validate(ComponentAst component, ArtifactAst artifact) {
    for (ComponentParameterAst param : component.getParameters()) {
      if (param.getValue().isRight() && param.getValue().getRight() instanceof String) {
        final String stringValue = (String) param.getValue().getRight();

        if (REQUIRED.equals(param.getModel().getExpressionSupport())
            && (!stringValue.startsWith(DEFAULT_EXPRESSION_PREFIX)
                || !stringValue.endsWith(DEFAULT_EXPRESSION_SUFFIX))) {
          return of(format("A static value was given for parameter '%s' but it requires a expression",
                           param.getModel().getName()));
        }
      }
    }

    return empty();
  }

}
