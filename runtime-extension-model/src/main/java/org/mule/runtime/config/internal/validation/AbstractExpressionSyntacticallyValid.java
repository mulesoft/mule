/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.runtime.ast.api.validation.ValidationResultItem.create;
import static org.mule.runtime.config.api.validation.ExpressionsSyntacticallyValidAdditionalDataKeys.LOCATION_END_POSITION_LINE;
import static org.mule.runtime.config.api.validation.ExpressionsSyntacticallyValidAdditionalDataKeys.LOCATION_END_POSITION_OFFSET;
import static org.mule.runtime.config.api.validation.ExpressionsSyntacticallyValidAdditionalDataKeys.LOCATION_START_POSITION_COLUMN;
import static org.mule.runtime.config.api.validation.ExpressionsSyntacticallyValidAdditionalDataKeys.LOCATION_START_POSITION_LINE;
import static org.mule.runtime.config.api.validation.ExpressionsSyntacticallyValidAdditionalDataKeys.LOCATION_START_POSITION_OFFSET;

import org.mule.runtime.api.el.ExpressionLanguage;
import org.mule.runtime.api.el.validation.Location;
import org.mule.runtime.api.el.validation.Severity;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.ast.api.validation.Validation;
import org.mule.runtime.ast.api.validation.ValidationResultItem;

import java.util.HashMap;
import java.util.Map;
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

  protected final Map<String, String> locationToAdditionalData(Location location) {
    Map<String, String> additionalData = new HashMap<>();

    additionalData.put(LOCATION_START_POSITION_LINE,
                       Integer.toString(location.getStartPosition().getLine()));
    additionalData.put(LOCATION_START_POSITION_COLUMN,
                       Integer.toString(location.getStartPosition().getColumn()));
    additionalData.put(LOCATION_START_POSITION_OFFSET,
                       Integer.toString(location.getStartPosition().getOffset()));
    additionalData.put(LOCATION_END_POSITION_LINE,
                       Integer.toString(location.getEndPosition().getLine()));
    additionalData.put(LOCATION_END_POSITION_LINE,
                       Integer.toString(location.getEndPosition().getColumn()));
    additionalData.put(LOCATION_END_POSITION_OFFSET,
                       Integer.toString(location.getEndPosition().getOffset()));

    return additionalData;

  }

  @Override
  public final Level getLevel() {
    return level.get();
  }

  protected final Severity getSeverity() {
    return severity;
  }

}
