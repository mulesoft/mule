/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el.dataweave;

import static org.mule.runtime.api.el.BindingContextUtils.PAYLOAD;
import static org.mule.runtime.api.el.BindingContextUtils.addEventBuindingsToBuilder;
import static org.mule.runtime.api.el.BindingContextUtils.addFlowNameBindingsToBuilder;
import static org.mule.runtime.api.metadata.DataType.fromType;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_EXPRESSIONS_COMPILATION_FAIL_DEPLOYMENT;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.expressionEvaluationFailed;
import static org.mule.runtime.core.api.util.SystemUtils.getDefaultEncoding;
import static org.mule.runtime.core.internal.el.ExpressionLanguageUtils.isSanitizedPayload;
import static org.mule.runtime.core.internal.el.ExpressionLanguageUtils.sanitize;

import static java.lang.System.getProperty;

import org.mule.metadata.message.api.el.TypeBindings;
import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.BindingContext.Builder;
import org.mule.runtime.api.el.CompiledExpression;
import org.mule.runtime.api.el.DefaultExpressionLanguageFactoryService;
import org.mule.runtime.api.el.ExpressionCompilationException;
import org.mule.runtime.api.el.ExpressionExecutionException;
import org.mule.runtime.api.el.ExpressionLanguage;
import org.mule.runtime.api.el.ExpressionLanguageConfiguration;
import org.mule.runtime.api.el.ExpressionLanguageSession;
import org.mule.runtime.api.el.ValidationResult;
import org.mule.runtime.api.el.validation.ScopePhaseValidationMessages;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.internal.el.DefaultBindingContextBuilder;
import org.mule.runtime.core.internal.el.ExpressionLanguageSessionAdaptor;
import org.mule.runtime.core.internal.el.ExtendedExpressionLanguageAdaptor;
import org.mule.runtime.core.internal.el.IllegalCompiledExpression;
import org.mule.runtime.core.internal.el.context.MuleInstanceContext;
import org.mule.runtime.core.internal.el.context.ServerContext;

import java.util.Iterator;
import java.util.function.Function;

import javax.inject.Inject;

public class DataWeaveExpressionLanguageAdaptor implements ExtendedExpressionLanguageAdaptor, Disposable {

  public static final String SERVER = "server";
  public static final String MULE = "mule";
  public static final String APP = "app";

  private final ExpressionLanguage expressionExecutor;
  private final MuleContext muleContext;

  @Inject
  public DataWeaveExpressionLanguageAdaptor(MuleContext muleContext, Registry registry,
                                            DefaultExpressionLanguageFactoryService service,
                                            FeatureFlaggingService featureFlaggingService) {
    this.expressionExecutor = service.create(ExpressionLanguageConfiguration.builder()
        .defaultEncoding(getDefaultEncoding(muleContext))
        .featureFlaggingService(featureFlaggingService)
        .appId(muleContext.getConfiguration().getId())
        .minMuleVersion(muleContext.getConfiguration().getMinMuleVersion())
        .build());
    this.muleContext = muleContext;
    registerGlobalBindings(registry);
  }

  private void registerGlobalBindings(Registry registry) {
    BindingContext.Builder contextBuilder = BindingContext.builder();
    contextBuilder.addBinding(MULE,
                              new TypedValue<>(new MuleInstanceContext(muleContext),
                                               fromType(MuleInstanceContext.class)));
    contextBuilder.addBinding(SERVER,
                              new TypedValue<>(new ServerContext(),
                                               fromType(ServerContext.class)));
    contextBuilder.addBinding(APP,
                              new TypedValue<>(new DataWeaveArtifactContext(muleContext, registry),
                                               fromType(DataWeaveArtifactContext.class)));
    addGlobalBindings(contextBuilder instanceof DefaultBindingContextBuilder
        ? ((DefaultBindingContextBuilder) contextBuilder).flattenAndBuild()
        : contextBuilder.build());
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
  public TypedValue evaluate(String expression, CoreEvent event, BindingContext context) {
    String sanitized = sanitize(expression);
    if (isSanitizedPayload(sanitized)) {
      return event.getMessage().getPayload();
    } else {
      BindingContext newContext = bindingContextFor(null, event, context);
      return evaluate(sanitized, exp -> expressionExecutor.evaluate(exp, newContext));
    }
  }



  @Override
  public TypedValue evaluate(String expression, DataType expectedOutputType, CoreEvent event, BindingContext context)
      throws ExpressionRuntimeException {
    BindingContext newContext = bindingContextFor(null, event, context);
    return sanitizeAndEvaluate(expression, exp -> expressionExecutor.evaluate(exp, expectedOutputType, newContext));
  }

  @Override
  public TypedValue evaluate(String expression, DataType expectedOutputType, CoreEvent event,
                             ComponentLocation componentLocation,
                             BindingContext context, boolean failOnNull)
      throws ExpressionRuntimeException {
    BindingContext newContext = bindingContextFor(componentLocation, event, context);
    return sanitizeAndEvaluate(expression, exp -> expressionExecutor.evaluate(exp, expectedOutputType, newContext));
  }

  @Override
  public TypedValue evaluate(String expression, CoreEvent event, ComponentLocation componentLocation,
                             BindingContext bindingContext) {
    return evaluate(expression, event, null, componentLocation, bindingContext);
  }

  @Override
  public TypedValue evaluate(String expression, CoreEvent event, CoreEvent.Builder eventBuilder,
                             ComponentLocation componentLocation,
                             BindingContext context) {
    String sanitized = sanitize(expression);
    if (isSanitizedPayload(sanitized)) {
      return resolvePayload(event, context);
    } else {
      BindingContext newContext = bindingContextFor(componentLocation, event, context);
      return evaluate(sanitized, exp -> expressionExecutor.evaluate(exp, newContext));
    }
  }

  @Override
  public CompiledExpression compile(String expression, BindingContext bindingContext) {
    try {
      return expressionExecutor.compile(sanitize(expression), bindingContext);
    } catch (ExpressionCompilationException e) {
      if (badExpressionFailsDeployment()) {
        throw e;
      }
      return new IllegalCompiledExpression(expression, e);
    }
  }

  @Override
  public ScopePhaseValidationMessages collectScopePhaseValidationMessages(String script, String nameIdentifier,
                                                                          TypeBindings bindings) {
    return expressionExecutor.collectScopePhaseValidationMessages(script, nameIdentifier, bindings);
  }

  private boolean badExpressionFailsDeployment() {
    return getProperty(MULE_EXPRESSIONS_COMPILATION_FAIL_DEPLOYMENT) != null;
  }

  /**
   * This provides an optimization to avoid going to DW for evaluating just the payload, which is there at hand already.
   */
  protected static TypedValue resolvePayload(CoreEvent event, BindingContext context) {
    return event != null ? event.getMessage().getPayload()
        : context != null ? context.lookup(PAYLOAD).orElse(null) : null;
  }

  @Override
  public TypedValue<?> evaluateLogExpression(String expression, CoreEvent event, ComponentLocation componentLocation,
                                             BindingContext bindingContext)
      throws ExpressionRuntimeException {
    try {
      return expressionExecutor.evaluateLogExpression(sanitize(expression),
                                                      bindingContextFor(componentLocation, event, bindingContext));
    } catch (ExpressionExecutionException e) {
      throw new ExpressionRuntimeException(expressionEvaluationFailed(e.getMessage(), expression), e);
    }
  }

  @Override
  public ValidationResult validate(String expression) {
    return expressionExecutor.validate(sanitize(expression));
  }

  @Override
  public Iterator<TypedValue<?>> split(String expression, CoreEvent event, ComponentLocation componentLocation,
                                       BindingContext bindingContext)
      throws ExpressionRuntimeException {
    BindingContext context = bindingContextFor(componentLocation, event, bindingContext);
    return sanitizeAndEvaluate(expression, exp -> expressionExecutor.split(exp, context));
  }

  @Override
  public Iterator<TypedValue<?>> split(String expression, CoreEvent event, BindingContext bindingContext)
      throws ExpressionRuntimeException {
    BindingContext context = bindingContextFor(null, event, bindingContext);
    return sanitizeAndEvaluate(expression, exp -> expressionExecutor.split(exp, context));
  }

  /**
   * Sanitizes the expression by removing the expression brackets and then evaluates it, handling any exceptions accordingly. All
   * evaluations should be done in this way.
   *
   * @param expression the expression to sanitize before running
   * @param evaluation the function to evaluate the expression with
   * @param <T>        the type that the function returns
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

  private BindingContext bindingContextFor(ComponentLocation componentLocation, CoreEvent event, BindingContext context) {
    if (event == null && componentLocation == null) {
      return context;
    }

    Builder contextBuilder;
    if (event != null) {
      contextBuilder = addEventBuindingsToBuilder(event, context);
    } else {
      contextBuilder = BindingContext.builder(context);
    }
    if (componentLocation != null) {
      contextBuilder = addFlowNameBindingsToBuilder(componentLocation, contextBuilder);
    }
    return contextBuilder.build();
  }

  @Override
  public void dispose() {
    expressionExecutor.dispose();
  }

  @Override
  public ExpressionLanguageSessionAdaptor openSession(ComponentLocation location, CoreEvent event, BindingContext baseContext) {
    final BindingContext context = bindingContextFor(location, event, baseContext);
    ExpressionLanguageSession session = expressionExecutor.openSession(context);
    return new ExpressionLanguageSessionAdaptor() {

      @Override
      public TypedValue<?> evaluate(String expression) throws ExpressionRuntimeException {
        String sanitized = sanitize(expression);
        if (isSanitizedPayload(sanitized)) {
          return resolvePayload(event, context);
        }

        try {
          return session.evaluate(sanitized);
        } catch (ExpressionExecutionException e) {
          throw new ExpressionRuntimeException(expressionEvaluationFailed(e.getMessage(), sanitized), e);
        }
      }

      @Override
      public TypedValue<?> evaluate(String expression, long timeout) throws ExpressionRuntimeException {
        String sanitized = sanitize(expression);
        if (isSanitizedPayload(sanitized)) {
          return resolvePayload(event, context);
        }
        try {
          return session.evaluate(sanitized, timeout);
        } catch (ExpressionExecutionException e) {
          throw new ExpressionRuntimeException(expressionEvaluationFailed(e.getMessage(), sanitized), e);
        }
      }

      @Override
      public TypedValue<?> evaluate(String expression, DataType expectedOutputType) throws ExpressionRuntimeException {
        String sanitized = sanitize(expression);
        try {
          return session.evaluate(sanitized, expectedOutputType);
        } catch (ExpressionExecutionException e) {
          throw new ExpressionRuntimeException(expressionEvaluationFailed(e.getMessage(), sanitized), e);
        }
      }

      @Override
      public TypedValue<?> evaluateLogExpression(String expression) throws ExpressionRuntimeException {
        String sanitized = sanitize(expression);
        try {
          return session.evaluateLogExpression(sanitized);
        } catch (ExpressionExecutionException e) {
          throw new ExpressionRuntimeException(expressionEvaluationFailed(e.getMessage(), sanitized), e);
        }
      }

      @Override
      public Iterator<TypedValue<?>> split(String expression) {
        return session.split(sanitize(expression));
      }

      private RuntimeException handledException(CompiledExpression expression, Exception e) {
        if (expression instanceof IllegalCompiledExpression) {
          ExpressionCompilationException original = ((IllegalCompiledExpression) expression).getCompilationException();
          return new ExpressionRuntimeException(expressionEvaluationFailed(original.getMessage(), expression.expression()),
                                                original);
        }

        return new ExpressionRuntimeException(expressionEvaluationFailed(e.getMessage(), expression.expression()), e);
      }

      @Override
      public TypedValue<?> evaluate(CompiledExpression expression) throws ExpressionExecutionException {
        if (isSanitizedPayload(expression.expression())) {
          return resolvePayload(event, context);
        }
        try {
          return session.evaluate(expression);
        } catch (Exception e) {
          throw handledException(expression, e);
        }
      }

      @Override
      public TypedValue<?> evaluate(CompiledExpression expression, DataType expectedOutputType)
          throws ExpressionExecutionException {
        try {
          return session.evaluate(expression, expectedOutputType);
        } catch (Exception e) {
          throw handledException(expression, e);
        }
      }

      @Override
      public TypedValue<?> evaluate(CompiledExpression expression, long timeout) throws ExpressionExecutionException {
        if (isSanitizedPayload(expression.expression())) {
          return resolvePayload(event, context);
        }

        try {
          return session.evaluate(expression, timeout);
        } catch (Exception e) {
          throw handledException(expression, e);
        }
      }

      @Override
      public TypedValue<?> evaluateLogExpression(CompiledExpression expression) throws ExpressionExecutionException {
        try {
          return session.evaluateLogExpression(expression);
        } catch (Exception e) {
          throw handledException(expression, e);
        }
      }

      @Override
      public Iterator<TypedValue<?>> split(CompiledExpression expression) {
        try {
          return session.split(expression);
        } catch (Exception e) {
          throw handledException(expression, e);
        }
      }

      @Override
      public void close() {
        session.close();
      }
    };
  }
}
