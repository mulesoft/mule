/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.lazy;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;

import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.message.api.el.TypeBindings;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.CompiledExpression;
import org.mule.runtime.api.el.ValidationResult;
import org.mule.runtime.api.el.validation.ConstraintViolation;
import org.mule.runtime.api.el.validation.ValidationPhase;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.api.util.func.CheckedSupplier;
import org.mule.runtime.core.internal.el.ExpressionLanguageSessionAdaptor;
import org.mule.runtime.core.internal.el.ExtendedExpressionLanguageAdaptor;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defers the creation of an {@link ExpressionLanguageSessionAdaptor} until the moment in which it's actually asked to resolve an
 * expression.
 * <p>
 * A {@link CheckedSupplier} will be used to obtain the delegate. The supplier will only be invoked <b>once</b> and the return
 * value will be cached.
 *
 * @since 4.2.0
 */
public class LazyExpressionLanguageAdaptor implements ExtendedExpressionLanguageAdaptor, Initialisable, Disposable {

  private static final Logger LOGGER = LoggerFactory.getLogger(LazyExpressionLanguageAdaptor.class);

  private List<BindingContext> globalBindings = new LinkedList<>();
  private volatile boolean initialised = false;
  private CheckedSupplier<ExtendedExpressionLanguageAdaptor> delegateSupplier;
  private ExtendedExpressionLanguageAdaptor delegate;
  private boolean shouldInitialise;

  /**
   * Creates a new instance
   *
   * @param delegateSupplier the supplier that provides the wrapped adaptor
   */
  public LazyExpressionLanguageAdaptor(CheckedSupplier<ExtendedExpressionLanguageAdaptor> delegateSupplier) {
    this.delegateSupplier = delegateSupplier;
  }

  @Override
  public void addGlobalBindings(BindingContext bindingContext) {
    if (initialised) {
      delegate.addGlobalBindings(bindingContext);
    } else {
      synchronized (this) {
        if (initialised) {
          delegate.addGlobalBindings(bindingContext);
        } else {
          globalBindings.add(bindingContext);
        }
      }
    }
  }

  private ExtendedExpressionLanguageAdaptor delegate() {
    if (!initialised) {
      synchronized (this) {
        if (!initialised) {
          delegate = delegateSupplier.get();
          if (shouldInitialise) {
            doInitialise();
          }
          globalBindings.forEach(delegate::addGlobalBindings);
          globalBindings = null;
          initialised = true;
        }
      }
      delegateSupplier = null;
    }

    return delegate;
  }

  @Override
  public TypedValue evaluate(String expression, CoreEvent event, CoreEvent.Builder eventBuilder,
                             ComponentLocation componentLocation, BindingContext bindingContext)
      throws ExpressionRuntimeException {
    return delegate().evaluate(expression, event, eventBuilder, componentLocation, bindingContext);
  }

  @Override
  public TypedValue evaluate(String expression, CoreEvent event, ComponentLocation componentLocation,
                             BindingContext bindingContext)
      throws ExpressionRuntimeException {
    return delegate().evaluate(expression, event, componentLocation, bindingContext);
  }

  @Override
  public TypedValue evaluate(String expression, CoreEvent event, BindingContext context) throws ExpressionRuntimeException {
    return delegate().evaluate(expression, event, context);
  }

  @Override
  public TypedValue evaluate(String expression, DataType expectedOutputType, CoreEvent event, BindingContext context)
      throws ExpressionRuntimeException {
    return delegate().evaluate(expression, expectedOutputType, event, context);
  }

  @Override
  public TypedValue evaluate(String expression, DataType expectedOutputType, CoreEvent event, ComponentLocation componentLocation,
                             BindingContext context, boolean failOnNull)
      throws ExpressionRuntimeException {
    return delegate().evaluate(expression, expectedOutputType, event, componentLocation, context, failOnNull);
  }

  @Override
  public CompiledExpression compile(String expression, BindingContext bindingContext) {
    return delegate().compile(expression, bindingContext);
  }

  @Override
  public TypedValue<?> evaluateLogExpression(String expression, CoreEvent event, ComponentLocation componentLocation,
                                             BindingContext bindingContext)
      throws ExpressionRuntimeException {
    return delegate().evaluateLogExpression(expression, event, componentLocation, bindingContext);
  }

  @Override
  public ValidationResult validate(String expression) {
    return delegate().validate(expression);
  }

  @Override
  public List<ConstraintViolation> validate(String script, String nameIdentifier, ValidationPhase validationScopePhase,
                                            TypeBindings typeBindings, Optional<MetadataType> outputType) {
    return delegate().validate(script, nameIdentifier, validationScopePhase, typeBindings, outputType);
  }

  @Override
  public Iterator<TypedValue<?>> split(String expression, CoreEvent event, ComponentLocation componentLocation,
                                       BindingContext bindingContext)
      throws ExpressionRuntimeException {
    return delegate().split(expression, event, componentLocation, bindingContext);
  }

  @Override
  public Iterator<TypedValue<?>> split(String expression, CoreEvent event, BindingContext bindingContext)
      throws ExpressionRuntimeException {
    return delegate().split(expression, event, bindingContext);
  }

  @Override
  public ExpressionLanguageSessionAdaptor openSession(ComponentLocation componentLocation, CoreEvent event,
                                                      BindingContext context) {
    return delegate().openSession(componentLocation, event, context);
  }

  @Override
  public void dispose() {
    if (delegate != null) {
      disposeIfNeeded(delegate, LOGGER);
    }
  }

  @Override
  public void initialise() throws InitialisationException {
    if (initialised) {
      doInitialise();
    } else {
      shouldInitialise = true;
    }
  }

  private void doInitialise() {
    try {
      initialiseIfNeeded(delegate);
    } catch (InitialisationException e) {
      throw new MuleRuntimeException(createStaticMessage("Failed to initialise the ExpressionLanguageAdaptor"), e);
    }
  }

}
