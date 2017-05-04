/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el;

import static java.util.Collections.unmodifiableMap;
import static org.mule.runtime.api.metadata.DataType.STRING;
import static org.mule.runtime.api.metadata.DataType.fromType;
import static org.mule.runtime.core.api.el.ExpressionManager.DEFAULT_EXPRESSION_POSTFIX;
import static org.mule.runtime.core.api.el.ExpressionManager.DEFAULT_EXPRESSION_PREFIX;
import static org.mule.runtime.core.config.i18n.CoreMessages.expressionEvaluationFailed;
import static org.mule.runtime.core.el.DefaultExpressionManager.DW_PREFIX;
import static org.mule.runtime.core.el.DefaultExpressionManager.PREFIX_EXPR_SEPARATOR;

import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.DefaultExpressionLanguageFactoryService;
import org.mule.runtime.api.el.ExpressionExecutionException;
import org.mule.runtime.api.el.ExpressionLanguage;
import org.mule.runtime.api.el.ValidationResult;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.i18n.I18nMessageFactory;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.el.ExtendedExpressionLanguageAdaptor;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.core.el.context.AppContext;
import org.mule.runtime.core.el.context.MuleInstanceContext;
import org.mule.runtime.core.el.context.ServerContext;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;

import javax.inject.Inject;

public class DataWeaveExpressionLanguageAdaptor implements ExtendedExpressionLanguageAdaptor {

  public static final String PAYLOAD = "payload";
  public static final String DATA_TYPE = "dataType";
  public static final String ATTRIBUTES = "attributes";
  public static final String ERROR = "error";
  public static final String ID = "id";
  public static final String CORRELATION_ID = "correlationId";
  public static final String VARIABLES = "variables";
  public static final String PROPERTIES = "properties";
  public static final String PARAMETERS = "parameters";
  public static final String FLOW = "flow";
  public static final String SERVER = "server";
  public static final String MULE = "mule";
  public static final String APP = "app";

  private ExpressionLanguage expressionExecutor;
  private MuleContext muleContext;

  @Inject
  public DataWeaveExpressionLanguageAdaptor(MuleContext muleContext) {
    try {
      this.expressionExecutor = muleContext.getRegistry().lookupObject(DefaultExpressionLanguageFactoryService.class).create();
    } catch (RegistrationException e) {
      throw new MuleRuntimeException(I18nMessageFactory.createStaticMessage("Unable to obtain expression executor."), e);
    }
    this.muleContext = muleContext;

    registerGlobalBindings();
  }

  private void registerGlobalBindings() {
    BindingContext.Builder contextBuilder = BindingContext.builder();
    contextBuilder.addBinding(MULE,
                              new TypedValue<>(new MuleInstanceContext(muleContext), fromType(MuleInstanceContext.class)));
    contextBuilder.addBinding(SERVER, new TypedValue<>(new ServerContext(), fromType(ServerContext.class)));
    contextBuilder.addBinding(APP, new TypedValue<>(new AppContext(muleContext), fromType(AppContext.class)));
    addGlobalBindings(contextBuilder.build());
  }


  /**
   * Registers the given {@link BindingContext} as global.
   *
   * @param bindingContext the context to register
   */
  @Override
  public void addGlobalBindings(BindingContext bindingContext) {
    expressionExecutor.addGlobalBindings(bindingContext);
  }

  @Override
  public TypedValue evaluate(String expression, Event event, BindingContext context) {
    BindingContext.Builder contextBuilder = bindingContextBuilderFor(event, context);
    return sanitizeAndEvaluate(expression, exp -> expressionExecutor.evaluate(exp, contextBuilder.build()));
  }

  @Override
  public TypedValue evaluate(String expression, DataType expectedOutputType, Event event, BindingContext context)
      throws ExpressionRuntimeException {
    BindingContext.Builder contextBuilder = bindingContextBuilderFor(event, context);
    return sanitizeAndEvaluate(expression, exp -> expressionExecutor.evaluate(exp, expectedOutputType, contextBuilder.build()));
  }

  @Override
  public TypedValue evaluate(String expression, DataType expectedOutputType, Event event, FlowConstruct flowConstruct,
                             BindingContext context)
      throws ExpressionRuntimeException {
    BindingContext.Builder contextBuilder = bindingContextBuilderFor(event, context);
    addFlowBindings(flowConstruct, contextBuilder);
    return sanitizeAndEvaluate(expression, exp -> expressionExecutor.evaluate(exp, expectedOutputType, contextBuilder.build()));
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
    return sanitizeAndEvaluate(expression, exp -> expressionExecutor.evaluate(exp, contextBuilder.build()));
  }

  @Override
  public ValidationResult validate(String expression) {
    return expressionExecutor.validate(sanitize(expression));
  }

  @Override
  public Iterator<TypedValue<?>> split(String expression, int bachSize, Event event, FlowConstruct flowConstruct,
                                       BindingContext bindingContext)
      throws ExpressionRuntimeException {
    BindingContext.Builder contextBuilder = bindingContextBuilderFor(event, bindingContext);
    addFlowBindings(flowConstruct, contextBuilder);
    return sanitizeAndEvaluate(expression, exp -> expressionExecutor.split(exp, bachSize, contextBuilder.build()));
  }

  @Override
  public Iterator<TypedValue<?>> split(String expression, int bachSize, Event event, BindingContext bindingContext)
      throws ExpressionRuntimeException {
    BindingContext.Builder contextBuilder = bindingContextBuilderFor(event, bindingContext);
    return sanitizeAndEvaluate(expression, exp -> expressionExecutor.split(exp, bachSize, contextBuilder.build()));
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

  /**
   * Sanitizes the expression by removing the expression brackets and then evaluates it, handling any exceptions accordingly. All
   * evaluations should be done in this way.
   *
   * @param expression the expression to sanitize before running
   * @param evaluation the function to evaluate the expression with
   * @param <T> the type that the function returns
   * @return the result of the evaluation
   */
  private <T> T sanitizeAndEvaluate(String expression, Function<String, T> evaluation) {
    try {
      return evaluation.apply(sanitize(expression));
    } catch (ExpressionExecutionException e) {
      throw new ExpressionRuntimeException(expressionEvaluationFailed(e.getMessage(), expression), e);
    }
  }

  private BindingContext.Builder addFlowBindings(FlowConstruct flow, BindingContext.Builder contextBuilder) {
    if (flow != null) {
      contextBuilder.addBinding(FLOW, new TypedValue<>(new FlowVariablesAccessor(flow.getName()),
                                                       fromType(FlowVariablesAccessor.class)));
    }
    return contextBuilder;
  }

  private void addEventBindings(Event event, BindingContext.Builder contextBuilder) {
    if (event != null) {
      contextBuilder.addBinding(ID, new TypedValue<>(event.getContext().getId(), STRING));
      contextBuilder.addBinding(CORRELATION_ID, new TypedValue<>(event.getContext().getCorrelationId(), STRING));
      Map<String, TypedValue> flowVars = new HashMap<>();
      event.getVariableNames().forEach(name -> {
        TypedValue value = event.getVariable(name);
        flowVars.put(name, value);
        contextBuilder.addBinding(name, value);
      });
      contextBuilder.addBinding(VARIABLES,
                                new TypedValue<>(unmodifiableMap(flowVars), fromType(flowVars.getClass())));
      contextBuilder.addBinding(PROPERTIES,
                                new TypedValue<>(unmodifiableMap(event.getProperties()),
                                                 fromType(event.getProperties().getClass())));
      contextBuilder.addBinding(PARAMETERS,
                                new TypedValue<>(unmodifiableMap(event.getParameters()),
                                                 fromType(event.getParameters().getClass())));

      Message message = event.getMessage();
      contextBuilder.addBinding(ATTRIBUTES, message.getAttributes());
      contextBuilder.addBinding(PAYLOAD, message.getPayload());
      contextBuilder.addBinding(DATA_TYPE, new TypedValue<>(message.getPayload().getDataType(), fromType(DataType.class)));
      Error error = event.getError().isPresent() ? event.getError().get() : null;
      contextBuilder.addBinding(ERROR, new TypedValue<>(error, fromType(Error.class)));
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

    if (sanitizedExpression.startsWith(DW_PREFIX + PREFIX_EXPR_SEPARATOR)) {
      sanitizedExpression = sanitizedExpression.substring((DW_PREFIX + PREFIX_EXPR_SEPARATOR).length());
    }
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
