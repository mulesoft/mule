/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.currentElemement;
import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.equalsIdentifier;
import static org.mule.runtime.ast.api.validation.Validation.Level.WARN;
import static org.mule.runtime.ast.api.validation.ValidationResultItem.create;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.validation.Validation;
import org.mule.runtime.ast.api.validation.ValidationResultItem;
import org.mule.runtime.extension.api.declaration.type.annotation.LiteralTypeAnnotation;

import java.util.List;
import java.util.function.Predicate;

/**
 * No expressions are provided for parameters that do not support expressions.
 */
public class NoExpressionsInNoExpressionsSupportedParams implements Validation {

  private static final String FLOW_REF_ELEMENT = "flow-ref";

  private static final String DEFAULT_EXPRESSION_PREFIX = "#[";
  private static final String DEFAULT_EXPRESSION_SUFFIX = "]";

  private static final ComponentIdentifier FLOW_REF_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(FLOW_REF_ELEMENT).build();

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
    return WARN;
  }

  @Override
  public Predicate<List<ComponentAst>> applicable() {
    return currentElemement(component -> component.getModel(ParameterizedModel.class).isPresent())
        // According to the extension model, flow-ref cannot be dynamic,
        // But this check is needed to avoid breaking on legacy cases that use dynamic flow-refs.
        .and(currentElemement(equalsIdentifier(FLOW_REF_IDENTIFIER).negate()));
  }

  @Override
  public List<ValidationResultItem> validateMany(ComponentAst component, ArtifactAst artifact) {
    return component.getParameters()
        .stream()
        .filter(param -> !param.getModel().isComponentId())
        .filter(param -> param.getValueOrResolutionError().isRight()
            && param.getResolvedRawValue() instanceof String)
        .filter(param -> NOT_SUPPORTED.equals(param.getModel().getExpressionSupport())
            && !param.getModel().getType().getAnnotation(LiteralTypeAnnotation.class).isPresent())
        .filter(param -> {
          final String stringValue = param.getResolvedRawValue();

          return stringValue.startsWith(DEFAULT_EXPRESSION_PREFIX)
              && stringValue.endsWith(DEFAULT_EXPRESSION_SUFFIX);
        })
        .map(param -> create(component, param, this,
                             format("An expression value was given for parameter '%s' but it doesn't support expressions",
                                    param.getModel().getName())))
        .collect(toList());
  }

}
