/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el;

import static java.util.Collections.unmodifiableMap;
import static java.util.ServiceLoader.load;
import static org.mule.runtime.api.metadata.DataType.fromType;
import static org.mule.runtime.core.api.el.ExpressionManager.DEFAULT_EXPRESSION_POSTFIX;
import static org.mule.runtime.core.api.el.ExpressionManager.DEFAULT_EXPRESSION_PREFIX;
import static org.mule.runtime.core.config.i18n.CoreMessages.expressionEvaluationFailed;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.ExpressionExecutionException;
import org.mule.runtime.api.el.ExpressionExecutor;
import org.mule.runtime.api.el.ValidationResult;
import org.mule.runtime.api.message.Attributes;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.el.ExtendedExpressionLanguage;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

import javax.inject.Inject;

public class DataWeaveExpressionLanguage implements ExtendedExpressionLanguage {

  private static final Logger logger = getLogger(DataWeaveExpressionLanguage.class);
  public static final String PAYLOAD = "payload";
  public static final String DATA_TYPE = "dataType";
  public static final String ATTRIBUTES = "attributes";
  public static final String ERROR = "error";
  public static final String VARIABLES = "variables";
  public static final String FLOW = "flow";

  private ExpressionExecutor expressionExecutor;

  @Inject
  public DataWeaveExpressionLanguage(ExpressionExecutor expressionExecutor) {
    this.expressionExecutor = expressionExecutor;
  }

  /**
   * Registers the given {@link BindingContext} as global.
   *
   * @param bindingContext the context to register
   */
  public synchronized void registerGlobalContext(BindingContext bindingContext) {
    expressionExecutor.addGlobalBindings(bindingContext);
  }

  @Override
  public TypedValue evaluate(String expression, Event event, BindingContext context) {
    BindingContext.Builder contextBuilder = bindingContextBuilderFor(event, context);
    return evaluate(expression, contextBuilder.build());
  }

  @Override
  public TypedValue evaluate(String expression, Event event, FlowConstruct flowConstruct, BindingContext bindingContext) {
    return evaluate(expression, event, null, flowConstruct, bindingContext);
  }

  @Override
  public TypedValue evaluate(String expression, Event event, Event.Builder eventBuilder, FlowConstruct flowConstruct,
                             BindingContext context) {
    BindingContext.Builder contextBuilder = bindingContextBuilderFor(event, context);
    addFlowBindings(flowConstruct, contextBuilder);
    return evaluate(expression, contextBuilder.build());
  }

  @Override
  public ValidationResult validate(String expression) {
    return expressionExecutor.validate(sanitize(expression));
  }

  @Override
  public void enrich(String expression, Event event, Event.Builder eventBuilder, FlowConstruct flowConstruct, Object object) {
    throw new UnsupportedOperationException("Enrichment is not allowed, yet.");
  }

  @Override
  public void enrich(String expression, Event event, Event.Builder eventBuilder, FlowConstruct flowConstruct,
                     TypedValue value) {
    throw new UnsupportedOperationException("Enrichment is not allowed, yet.");
  }

  public boolean isEnabled() {
    return expressionExecutor != null;
  }

  private TypedValue evaluate(String expression, BindingContext context) {
    try {
      return expressionExecutor.evaluate(sanitize(expression), context);
    } catch (ExpressionExecutionException e) {
      throw new ExpressionRuntimeException(expressionEvaluationFailed(expression), e);
    }
  }

  private BindingContext.Builder addFlowBindings(FlowConstruct flow, BindingContext.Builder contextBuilder) {
    if (flow != null) {
      contextBuilder.addBinding(FLOW, new TypedValue(new FlowVariablesAccessor(flow.getName()),
                                                     fromType(FlowVariablesAccessor.class)));
    }
    return contextBuilder;
  }

  private void addEventBindings(Event event, BindingContext.Builder contextBuilder) {
    if (event != null) {
      Map<String, TypedValue> flowVars = new HashMap<>();
      event.getVariableNames().forEach(name -> {
        TypedValue value = event.getVariable(name);
        flowVars.put(name, value);
        contextBuilder.addBinding(name, value);
      });
      contextBuilder.addBinding(VARIABLES,
                                new TypedValue(unmodifiableMap(flowVars), fromType(flowVars.getClass())));
      Message message = event.getMessage();
      Attributes attributes = message.getAttributes();
      contextBuilder.addBinding(ATTRIBUTES, new TypedValue(attributes, fromType(attributes.getClass())));
      contextBuilder.addBinding(PAYLOAD, message.getPayload());
      contextBuilder.addBinding(DATA_TYPE, new TypedValue(message.getPayload().getDataType(), fromType(DataType.class)));
      Error error = event.getError().isPresent() ? event.getError().get() : null;
      contextBuilder.addBinding(ERROR, new TypedValue(error, fromType(Error.class)));
    }
  }

  private BindingContext.Builder bindingContextBuilderFor(Event event, BindingContext context) {
    BindingContext.Builder contextBuilder = BindingContext.builder(context);
    addEventBindings(event, contextBuilder);
    return contextBuilder;
  }

  private String sanitize(String expression) {
    String sanitizedExpression = expression.startsWith(DEFAULT_EXPRESSION_PREFIX)
        ? expression.substring(DEFAULT_EXPRESSION_PREFIX.length(), expression.length() - DEFAULT_EXPRESSION_POSTFIX.length())
        : expression;
    return sanitizedExpression;
  }

  private class FlowVariablesAccessor {

    private String name;

    public FlowVariablesAccessor(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

  }
}
