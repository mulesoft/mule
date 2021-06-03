/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.lazy;

import static java.lang.Thread.currentThread;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.DefaultExpressionLanguageFactoryService;
import org.mule.runtime.api.el.ExpressionExecutionException;
import org.mule.runtime.api.el.ValidationResult;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.el.ExpressionManagerSession;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.event.CoreEvent.Builder;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.internal.el.DefaultExpressionManager;
import org.mule.runtime.core.internal.el.ExtendedExpressionLanguageAdaptor;

import java.util.Iterator;

/**
 * Expression manager that does not initialize the underlying expression language support until a first usage is done.
 *
 * @since 4.4
 */
public class LazyExpressionManager extends DefaultExpressionManager {

  public static final String NON_LAZY_EXPRESSION_MANAGER = "_muleNonLazyExpressionManager";

  private final ClassLoader executionClassLoader;

  public LazyExpressionManager(ClassLoader executionClassLoader) {
    this.executionClassLoader = executionClassLoader;
  }

  @Override
  protected ExtendedExpressionLanguageAdaptor createExpressionLanguageAdaptor(DefaultExpressionLanguageFactoryService service) {
    return new LazyExpressionLanguageAdaptor(() -> super.createExpressionLanguageAdaptor(service));
  }

  @Override
  public TypedValue evaluate(String expression) throws ExpressionRuntimeException {
    final ClassLoader originalTccl = currentThread().getContextClassLoader();
    try {
      if (originalTccl != executionClassLoader) {
        currentThread().setContextClassLoader(executionClassLoader);
      }

      return super.evaluate(expression);
    } finally {
      if (originalTccl != executionClassLoader) {
        currentThread().setContextClassLoader(originalTccl);
      }
    }
  }

  @Override
  public TypedValue evaluate(String expression, CoreEvent event) throws ExpressionRuntimeException {
    final ClassLoader originalTccl = currentThread().getContextClassLoader();
    try {
      if (originalTccl != executionClassLoader) {
        currentThread().setContextClassLoader(executionClassLoader);
      }

      return super.evaluate(expression, event);
    } finally {
      if (originalTccl != executionClassLoader) {
        currentThread().setContextClassLoader(originalTccl);
      }
    }
  }

  @Override
  public TypedValue evaluate(String expression, CoreEvent event, ComponentLocation componentLocation)
      throws ExpressionRuntimeException {
    final ClassLoader originalTccl = currentThread().getContextClassLoader();
    try {
      if (originalTccl != executionClassLoader) {
        currentThread().setContextClassLoader(executionClassLoader);
      }

      return super.evaluate(expression, event, componentLocation);
    } finally {
      if (originalTccl != executionClassLoader) {
        currentThread().setContextClassLoader(originalTccl);
      }
    }
  }

  @Override
  public TypedValue evaluate(String expression, CoreEvent event, BindingContext context) throws ExpressionRuntimeException {
    final ClassLoader originalTccl = currentThread().getContextClassLoader();
    try {
      if (originalTccl != executionClassLoader) {
        currentThread().setContextClassLoader(executionClassLoader);
      }

      return super.evaluate(expression, event, context);
    } finally {
      if (originalTccl != executionClassLoader) {
        currentThread().setContextClassLoader(originalTccl);
      }
    }
  }

  @Override
  public TypedValue evaluate(String expression, CoreEvent event, ComponentLocation componentLocation, BindingContext context)
      throws ExpressionRuntimeException {
    final ClassLoader originalTccl = currentThread().getContextClassLoader();
    try {
      if (originalTccl != executionClassLoader) {
        currentThread().setContextClassLoader(executionClassLoader);
      }

      return super.evaluate(expression, event, componentLocation, context);
    } finally {
      if (originalTccl != executionClassLoader) {
        currentThread().setContextClassLoader(originalTccl);
      }
    }
  }

  @Override
  public TypedValue evaluate(String expression, DataType expectedOutputType) throws ExpressionRuntimeException {
    final ClassLoader originalTccl = currentThread().getContextClassLoader();
    try {
      if (originalTccl != executionClassLoader) {
        currentThread().setContextClassLoader(executionClassLoader);
      }

      return super.evaluate(expression, expectedOutputType);
    } finally {
      if (originalTccl != executionClassLoader) {
        currentThread().setContextClassLoader(originalTccl);
      }
    }
  }

  @Override
  public TypedValue evaluate(String expression, DataType expectedOutputType, BindingContext context, CoreEvent event)
      throws ExpressionRuntimeException {
    final ClassLoader originalTccl = currentThread().getContextClassLoader();
    try {
      if (originalTccl != executionClassLoader) {
        currentThread().setContextClassLoader(executionClassLoader);
      }

      return super.evaluate(expression, expectedOutputType, context, event);
    } finally {
      if (originalTccl != executionClassLoader) {
        currentThread().setContextClassLoader(originalTccl);
      }
    }
  }

  @Override
  public TypedValue evaluate(String expression, DataType expectedOutputType, BindingContext context, CoreEvent event,
                             ComponentLocation componentLocation, boolean failOnNull)
      throws ExpressionRuntimeException {
    final ClassLoader originalTccl = currentThread().getContextClassLoader();
    try {
      if (originalTccl != executionClassLoader) {
        currentThread().setContextClassLoader(executionClassLoader);
      }

      return super.evaluate(expression, expectedOutputType, context, event, componentLocation, failOnNull);
    } finally {
      if (originalTccl != executionClassLoader) {
        currentThread().setContextClassLoader(originalTccl);
      }
    }
  }

  @Override
  public boolean evaluateBoolean(String expression, CoreEvent event, ComponentLocation componentLocation)
      throws ExpressionRuntimeException {
    final ClassLoader originalTccl = currentThread().getContextClassLoader();
    try {
      if (originalTccl != executionClassLoader) {
        currentThread().setContextClassLoader(executionClassLoader);
      }

      return super.evaluateBoolean(expression, event, componentLocation);
    } finally {
      if (originalTccl != executionClassLoader) {
        currentThread().setContextClassLoader(originalTccl);
      }
    }
  }

  @Override
  public boolean evaluateBoolean(String expression, CoreEvent event, ComponentLocation componentLocation, boolean nullReturnsTrue,
                                 boolean nonBooleanReturnsTrue)
      throws ExpressionRuntimeException {
    final ClassLoader originalTccl = currentThread().getContextClassLoader();
    try {
      if (originalTccl != executionClassLoader) {
        currentThread().setContextClassLoader(executionClassLoader);
      }

      return super.evaluateBoolean(expression, event, componentLocation, nullReturnsTrue, nonBooleanReturnsTrue);
    } finally {
      if (originalTccl != executionClassLoader) {
        currentThread().setContextClassLoader(originalTccl);
      }
    }
  }

  @Override
  public boolean evaluateBoolean(String expression, BindingContext bindingCtx, ComponentLocation componentLocation,
                                 boolean nullReturnsTrue, boolean nonBooleanReturnsTrue)
      throws ExpressionRuntimeException {
    final ClassLoader originalTccl = currentThread().getContextClassLoader();
    try {
      if (originalTccl != executionClassLoader) {
        currentThread().setContextClassLoader(executionClassLoader);
      }

      return super.evaluateBoolean(expression, bindingCtx, componentLocation, nullReturnsTrue, nonBooleanReturnsTrue);
    } finally {
      if (originalTccl != executionClassLoader) {
        currentThread().setContextClassLoader(originalTccl);
      }
    }
  }

  @Override
  public Iterator<TypedValue<?>> split(String expression, CoreEvent event, ComponentLocation componentLocation,
                                       BindingContext bindingContext)
      throws ExpressionRuntimeException {
    final ClassLoader originalTccl = currentThread().getContextClassLoader();
    try {
      if (originalTccl != executionClassLoader) {
        currentThread().setContextClassLoader(executionClassLoader);
      }

      return super.split(expression, event, componentLocation, bindingContext);
    } finally {
      if (originalTccl != executionClassLoader) {
        currentThread().setContextClassLoader(originalTccl);
      }
    }
  }

  @Override
  public Iterator<TypedValue<?>> split(String expression, CoreEvent event, BindingContext bindingContext)
      throws ExpressionRuntimeException {
    final ClassLoader originalTccl = currentThread().getContextClassLoader();
    try {
      if (originalTccl != executionClassLoader) {
        currentThread().setContextClassLoader(executionClassLoader);
      }

      return super.split(expression, event, bindingContext);
    } finally {
      if (originalTccl != executionClassLoader) {
        currentThread().setContextClassLoader(originalTccl);
      }
    }
  }

  @Override
  public String parseLogTemplate(String template, CoreEvent event, ComponentLocation componentLocation,
                                 BindingContext bindingContext)
      throws ExpressionRuntimeException {
    final ClassLoader originalTccl = currentThread().getContextClassLoader();
    try {
      if (originalTccl != executionClassLoader) {
        currentThread().setContextClassLoader(executionClassLoader);
      }

      return super.parseLogTemplate(template, event, componentLocation, bindingContext);
    } finally {
      if (originalTccl != executionClassLoader) {
        currentThread().setContextClassLoader(originalTccl);
      }
    }
  }

  @Override
  public ExpressionManagerSession openSession(BindingContext context) {
    final ClassLoader originalTccl = currentThread().getContextClassLoader();
    try {
      if (originalTccl != executionClassLoader) {
        currentThread().setContextClassLoader(executionClassLoader);
      }

      return super.openSession(context);
    } finally {
      if (originalTccl != executionClassLoader) {
        currentThread().setContextClassLoader(originalTccl);
      }
    }
  }

  @Override
  public ExpressionManagerSession openSession(ComponentLocation componentLocation, CoreEvent event, BindingContext context) {
    final ClassLoader originalTccl = currentThread().getContextClassLoader();
    try {
      if (originalTccl != executionClassLoader) {
        currentThread().setContextClassLoader(executionClassLoader);
      }

      return super.openSession(componentLocation, event, context);
    } finally {
      if (originalTccl != executionClassLoader) {
        currentThread().setContextClassLoader(originalTccl);
      }
    }
  }

  @Override
  public boolean isExpression(String expression) {
    final ClassLoader originalTccl = currentThread().getContextClassLoader();
    try {
      if (originalTccl != executionClassLoader) {
        currentThread().setContextClassLoader(executionClassLoader);
      }

      return super.isExpression(expression);
    } finally {
      if (originalTccl != executionClassLoader) {
        currentThread().setContextClassLoader(originalTccl);
      }
    }
  }

  @Override
  public void addGlobalBindings(BindingContext bindingContext) {
    final ClassLoader originalTccl = currentThread().getContextClassLoader();
    try {
      if (originalTccl != executionClassLoader) {
        currentThread().setContextClassLoader(executionClassLoader);
      }

      super.addGlobalBindings(bindingContext);
    } finally {
      if (originalTccl != executionClassLoader) {
        currentThread().setContextClassLoader(originalTccl);
      }
    }
  }

  @Override
  public TypedValue<?> evaluate(String expression, BindingContext context) throws ExpressionExecutionException {
    final ClassLoader originalTccl = currentThread().getContextClassLoader();
    try {
      if (originalTccl != executionClassLoader) {
        currentThread().setContextClassLoader(executionClassLoader);
      }

      return super.evaluate(expression, context);
    } finally {
      if (originalTccl != executionClassLoader) {
        currentThread().setContextClassLoader(originalTccl);
      }
    }
  }

  @Override
  public TypedValue<?> evaluate(String expression, DataType expectedOutputType, BindingContext context)
      throws ExpressionExecutionException {
    final ClassLoader originalTccl = currentThread().getContextClassLoader();
    try {
      if (originalTccl != executionClassLoader) {
        currentThread().setContextClassLoader(executionClassLoader);
      }

      return super.evaluate(expression, expectedOutputType, context);
    } finally {
      if (originalTccl != executionClassLoader) {
        currentThread().setContextClassLoader(originalTccl);
      }
    }
  }

  @Override
  public TypedValue<?> evaluateLogExpression(String expression, BindingContext context) throws ExpressionExecutionException {
    final ClassLoader originalTccl = currentThread().getContextClassLoader();
    try {
      if (originalTccl != executionClassLoader) {
        currentThread().setContextClassLoader(executionClassLoader);
      }

      return super.evaluateLogExpression(expression, context);
    } finally {
      if (originalTccl != executionClassLoader) {
        currentThread().setContextClassLoader(originalTccl);
      }
    }
  }

  @Override
  public ValidationResult validate(String expression) {
    final ClassLoader originalTccl = currentThread().getContextClassLoader();
    try {
      if (originalTccl != executionClassLoader) {
        currentThread().setContextClassLoader(executionClassLoader);
      }

      return super.validate(expression);
    } finally {
      if (originalTccl != executionClassLoader) {
        currentThread().setContextClassLoader(originalTccl);
      }
    }
  }

  @Override
  public Iterator<TypedValue<?>> split(String expression, BindingContext context) {
    final ClassLoader originalTccl = currentThread().getContextClassLoader();
    try {
      if (originalTccl != executionClassLoader) {
        currentThread().setContextClassLoader(executionClassLoader);
      }

      return super.split(expression, context);
    } finally {
      if (originalTccl != executionClassLoader) {
        currentThread().setContextClassLoader(originalTccl);
      }
    }
  }

  @Override
  public TypedValue evaluate(String expression, CoreEvent event, Builder eventBuilder, ComponentLocation componentLocation)
      throws ExpressionRuntimeException {
    final ClassLoader originalTccl = currentThread().getContextClassLoader();
    try {
      if (originalTccl != executionClassLoader) {
        currentThread().setContextClassLoader(executionClassLoader);
      }

      return super.evaluate(expression, event, eventBuilder, componentLocation);
    } finally {
      if (originalTccl != executionClassLoader) {
        currentThread().setContextClassLoader(originalTccl);
      }
    }
  }

  @Override
  public String parse(String expression, CoreEvent event, ComponentLocation componentLocation) throws ExpressionRuntimeException {
    final ClassLoader originalTccl = currentThread().getContextClassLoader();
    try {
      if (originalTccl != executionClassLoader) {
        currentThread().setContextClassLoader(executionClassLoader);
      }

      return super.parse(expression, event, componentLocation);
    } finally {
      if (originalTccl != executionClassLoader) {
        currentThread().setContextClassLoader(originalTccl);
      }
    }
  }

}
