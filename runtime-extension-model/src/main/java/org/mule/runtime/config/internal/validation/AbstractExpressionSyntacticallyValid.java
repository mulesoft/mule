/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.runtime.ast.api.validation.ValidationResultItem.create;
import static org.mule.runtime.config.internal.validation.ValidationUtils.locationToAdditionalData;

import org.mule.runtime.api.el.ExpressionLanguage;
import org.mule.runtime.api.el.validation.Severity;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.ast.api.validation.Validation;
import org.mule.runtime.ast.api.validation.ValidationResultItem;

import java.util.function.Supplier;
import java.util.stream.Stream;

public abstract class AbstractExpressionSyntacticallyValid implements Validation {

  protected final ExpressionLanguage expressionLanguage;
  private final Supplier<Level> level;
  private final Severity severity;

  public AbstractExpressionSyntacticallyValid(ExpressionLanguage expressionLanguage, Supplier<Level> level, Severity severity) {
    this.expressionLanguage = expressionLanguage;
    this.level = level;
    this.severity = severity;
  }

  protected final Stream<ValidationResultItem> validateExpression(ComponentAst component, ComponentParameterAst param,
                                                                  String expression) {
    return expressionLanguage.validate(expression).messages().stream()
        .filter(msg -> msg.getSeverity().equals(getSeverity()))
        .map(msg -> create(component, param, this, msg.getMessage(), locationToAdditionalData(msg.getLocation())));
  }

  @Override
  public final Level getLevel() {
    return level.get();
  }

  protected final Severity getSeverity() {
    return severity;
  }

}
