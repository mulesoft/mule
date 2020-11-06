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
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.currentElemement;
import static org.mule.runtime.ast.api.validation.Validation.Level.ERROR;

import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.ast.api.validation.Validation;
import org.mule.runtime.extension.api.declaration.type.annotation.LiteralTypeAnnotation;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * No expressions are provided for parameters that do not support expressions.
 */
public class NoExpressionsInNoExpressionsSupportedParams implements Validation {

  @Override
  public String getName() {
    return "No expressions in no expressionsSupported params";
  }

  @Override
  public String getDescription() {
    return "No expressions are provided for parameters that do not support expressions.";
  }

  @Override
  public Level getLevel() {
    return ERROR;
  }

  @Override
  public Predicate<List<ComponentAst>> applicable() {
    return currentElemement(component -> component.getModel(ParameterizedModel.class).isPresent());
  }

  @Override
  public Optional<String> validate(ComponentAst component, ArtifactAst artifact) {
    for (ComponentParameterAst param : component.getParameters()) {
      if (NOT_SUPPORTED.equals(param.getModel().getExpressionSupport())
          && !param.getModel().getType().getAnnotation(LiteralTypeAnnotation.class).isPresent()
          && param.getValue().isLeft()) {
        return of(format("An expression value was given for parameter '%s' but it doesn't support expressions",
                         param.getModel().getName()));
      }
    }

    return empty();
  }

}
