/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el.v2;

import static java.util.Collections.unmodifiableMap;
import static java.util.ServiceLoader.load;
import static org.mule.runtime.api.metadata.DataType.fromType;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.ExpressionExecutor;
import org.mule.runtime.api.el.ExpressionLanguage;
import org.mule.runtime.api.el.ValidationResult;
import org.mule.runtime.api.message.Attributes;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.message.MuleEvent;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.metadata.DefaultTypedValue;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MuleExpressionLanguage implements ExpressionLanguage {

  public static final String ATTRIBUTES = "attributes";
  public static final String PAYLOAD = "payload";
  public static final String ERROR = "error";
  public static final String VARIABLES = "variables";

  private ExpressionExecutor expressionExecutor;
  private BindingContext globalBindingContext;

  public MuleExpressionLanguage() {
    Iterator<ExpressionExecutor> executors = load(ExpressionExecutor.class).iterator();
    while (executors.hasNext()) {
      //TODO: MULE-10410 - define how to handle dw and mvel at the same time
      this.expressionExecutor = executors.next();
      break;
    }
    // TODO: MULE-10765 - Define global bindings
    this.globalBindingContext = BindingContext.builder().build();
  }

  @Override
  public TypedValue evaluate(String expression) {
    return evaluate(expression, BindingContext.builder().build());
  }

  @Override
  public TypedValue evaluate(String expression, BindingContext context, MuleEvent event) {
    BindingContext.Builder contextBuilder = BindingContext.builder();
    contextBuilder.addAll(context);
    addEventBindings(event, contextBuilder);
    return evaluate(expression, contextBuilder.build());
  }

  @Override
  public TypedValue evaluate(String expression, BindingContext context) {
    return expressionExecutor.evaluate(expression, context);
  }

  @Override
  public TypedValue evaluate(String expression, DataType expectedOutputType) {
    return evaluate(expression, expectedOutputType, BindingContext.builder().build());
  }

  @Override
  public TypedValue evaluate(String expression, DataType expectedOutputType, BindingContext context, MuleEvent event) {
    BindingContext.Builder contextBuilder = BindingContext.builder();
    contextBuilder.addAll(context);
    addEventBindings(event, contextBuilder);
    return evaluate(expression, expectedOutputType, contextBuilder.build());
  }

  @Override
  public TypedValue evaluate(String expression, DataType expectedOutputType, BindingContext context) {
    return expressionExecutor.evaluate(expression, expectedOutputType, context);
  }

  @Override
  public ValidationResult validate(String expression) {
    return expressionExecutor.validate(expression);
  }

  private void addEventBindings(MuleEvent event, BindingContext.Builder contextBuilder) {
    Message message = event.getMessage();
    Attributes attributes = message.getAttributes();
    contextBuilder.addBinding(ATTRIBUTES, new DefaultTypedValue(attributes, fromType(attributes.getClass())));
    contextBuilder.addBinding(PAYLOAD, message.getPayload());
    Error error = event.getError().isPresent() ? event.getError().get() : null;
    contextBuilder.addBinding(ERROR, new DefaultTypedValue(error, fromType(Error.class)));
    Map<String, TypedValue> flowVars = new HashMap<>();
    event.getVariableNames().forEach(name -> flowVars.put(name, event.getVariable(name)));
    contextBuilder.addBinding(VARIABLES,
                              new DefaultTypedValue(unmodifiableMap(flowVars), DataType.fromType(flowVars.getClass())));
  }

}
