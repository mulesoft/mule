/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el;

import static org.mule.runtime.api.metadata.DataType.fromType;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.expressionEvaluationFailed;
import static org.mule.runtime.core.api.el.ExpressionManager.DEFAULT_EXPRESSION_POSTFIX;
import static org.mule.runtime.core.api.el.ExpressionManager.DEFAULT_EXPRESSION_PREFIX;
import static org.mule.runtime.core.el.DefaultExpressionManager.DW_PREFIX;
import static org.mule.runtime.core.el.DefaultExpressionManager.PREFIX_EXPR_SEPARATOR;
import static org.mule.runtime.internal.el.BindingContextUtils.PAYLOAD;
import static org.mule.runtime.internal.el.BindingContextUtils.addEventBindings;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.DefaultExpressionLanguageFactoryService;
import org.mule.runtime.api.el.ExpressionExecutionException;
import org.mule.runtime.api.el.ExpressionLanguage;
import org.mule.runtime.api.el.ValidationResult;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.i18n.I18nMessageFactory;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExtendedExpressionLanguageAdaptor;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.core.el.context.DataWeaveArtifactContext;
import org.mule.runtime.core.el.context.MuleInstanceContext;
import org.mule.runtime.core.el.context.ServerContext;

import java.util.Iterator;
import java.util.function.Function;

import javax.inject.Inject;

public class DataWeaveExpressionLanguageAdaptor implements ExtendedExpressionLanguageAdaptor {

  public static final String FLOW = "flow";
  public static final String SERVER = "server";
  public static final String MULE = "mule";
  public static final String APP = "app";

  private ExpressionLanguage expressionExecutor;
  private MuleContext muleContext;

  public static DataWeaveExpressionLanguageAdaptor create(MuleContext muleContext) {
    try {
      return new DataWeaveExpressionLanguageAdaptor(muleContext, muleContext.getRegistry()
          .lookupObject(DefaultExpressionLanguageFactoryService.class));
    } catch (RegistrationException e) {
      throw new MuleRuntimeException(I18nMessageFactory.createStaticMessage("Unable to obtain expression executor."), e);
    }
  }

  @Inject
  public DataWeaveExpressionLanguageAdaptor(MuleContext muleContext, DefaultExpressionLanguageFactoryService service) {
    this.expressionExecutor = service.create();
    this.muleContext = muleContext;
    registerGlobalBindings();
  }

  private void registerGlobalBindings() {
    BindingContext.Builder contextBuilder = BindingContext.builder();
    contextBuilder.addBinding(MULE,
                              new TypedValue<>(new MuleInstanceContext(muleContext), fromType(MuleInstanceContext.class)));
    contextBuilder.addBinding(SERVER, new TypedValue<>(new ServerContext(), fromType(ServerContext.class)));
    contextBuilder
        .addBinding(APP, new TypedValue<>(new DataWeaveArtifactContext(muleContext), fromType(DataWeaveArtifactContext.class)));
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
    String sanitized = sanitize(expression);
    if (isPayloadExpression(sanitized)) {
      return event.getMessage().getPayload();
    } else {
      BindingContext.Builder contextBuilder = bindingContextBuilderFor(event, context);
      return evaluate(sanitized, exp -> expressionExecutor.evaluate(exp, contextBuilder.build()));
    }
  }

  private boolean isPayloadExpression(String sanitized) {
    return sanitized.equals(PAYLOAD);
  }

  @Override
  public TypedValue evaluate(String expression, DataType expectedOutputType, Event event, BindingContext context)
      throws ExpressionRuntimeException {
    BindingContext.Builder contextBuilder = bindingContextBuilderFor(event, context);
    return sanitizeAndEvaluate(expression, exp -> expressionExecutor.evaluate(exp, expectedOutputType, contextBuilder.build()));
  }

  @Override
  public TypedValue evaluate(String expression, DataType expectedOutputType, Event event, ComponentLocation componentLocation,
                             BindingContext context, boolean failOnNull)
      throws ExpressionRuntimeException {
    BindingContext.Builder contextBuilder = bindingContextBuilderFor(event, context);
    addFlowBindings(componentLocation, contextBuilder);
    return sanitizeAndEvaluate(expression, exp -> expressionExecutor.evaluate(exp, expectedOutputType, contextBuilder.build()));
  }

  @Override
  public TypedValue evaluate(String expression, Event event, ComponentLocation componentLocation, BindingContext bindingContext) {
    return evaluate(expression, event, null, componentLocation, bindingContext);
  }

  @Override
  public TypedValue evaluate(String expression, Event event, Event.Builder eventBuilder, ComponentLocation componentLocation,
                             BindingContext context) {
    String sanitized = sanitize(expression);
    if (isPayloadExpression(sanitized)) {
      return event != null ? event.getMessage().getPayload()
          : context != null ? context.lookup(PAYLOAD).orElse(null) : null;
    } else {
      BindingContext.Builder contextBuilder = bindingContextBuilderFor(event, context);
      addFlowBindings(componentLocation, contextBuilder);
      return evaluate(sanitized, exp -> expressionExecutor.evaluate(exp, contextBuilder.build()));
    }
  }

  @Override
  public ValidationResult validate(String expression) {
    return expressionExecutor.validate(sanitize(expression));
  }

  @Override
  public Iterator<TypedValue<?>> split(String expression, Event event, ComponentLocation componentLocation,
                                       BindingContext bindingContext)
      throws ExpressionRuntimeException {
    BindingContext.Builder contextBuilder = bindingContextBuilderFor(event, bindingContext);
    addFlowBindings(componentLocation, contextBuilder);
    return sanitizeAndEvaluate(expression, exp -> expressionExecutor.split(exp, contextBuilder.build()));
  }

  @Override
  public Iterator<TypedValue<?>> split(String expression, Event event, BindingContext bindingContext)
      throws ExpressionRuntimeException {
    BindingContext.Builder contextBuilder = bindingContextBuilderFor(event, bindingContext);
    return sanitizeAndEvaluate(expression, exp -> expressionExecutor.split(exp, contextBuilder.build()));
  }

  @Override
  public void enrich(String expression, Event event, Event.Builder eventBuilder, ComponentLocation componentLocation,
                     Object object) {
    throw new UnsupportedOperationException("Enrichment is not allowed, yet.");
  }

  @Override
  public void enrich(String expression, Event event, Event.Builder eventBuilder, ComponentLocation componentLocation,
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
    return evaluate(sanitize(expression), evaluation);
  }

  private <T> T evaluate(String expression, Function<String, T> evaluation) {
    try {
      return evaluation.apply(expression);
    } catch (ExpressionExecutionException e) {
      throw new ExpressionRuntimeException(expressionEvaluationFailed(e.getMessage(), expression), e);
    }
  }

  private BindingContext.Builder addFlowBindings(ComponentLocation componentLocation, BindingContext.Builder contextBuilder) {
    if (componentLocation != null) {
      contextBuilder.addBinding(FLOW, new TypedValue<>(new FlowVariablesAccessor(componentLocation.getRootContainerName()),
                                                       fromType(FlowVariablesAccessor.class)));
    }
    return contextBuilder;
  }

  private BindingContext.Builder bindingContextBuilderFor(Event event, BindingContext context) {
    if (event != null) {
      return BindingContext.builder(addEventBindings(event, context));
    } else {
      return BindingContext.builder(context);
    }
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
