/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.el;

import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_DW_EXPRESSION_LANGUAGE_ADAPTER;
import static org.mule.runtime.core.internal.execution.ClassLoaderInjectorInvocationHandler.createClassLoaderInjectorInvocationHandler;

import static java.util.Collections.emptyList;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.internal.el.DefaultBindingContextBuilder;
import org.mule.runtime.core.internal.el.DefaultExpressionManager;
import org.mule.runtime.core.internal.el.ExtendedExpressionLanguageAdaptor;
import org.mule.runtime.core.privileged.el.GlobalBindingContextProvider;

import java.util.List;

import org.slf4j.Logger;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Creates the default {@link org.mule.runtime.core.api.el.ExpressionManager}
 * <p>
 * This factory creates a proxy on top of the real expression manager. That proxy is used to set the right classloader on the
 * current thread's context classloader before calling any method on the delegate object.
 *
 * @since 4.0
 */
public class DefaultExpressionManagerFactoryBean implements FactoryBean<ExtendedExpressionManager> {

  private static final Logger LOGGER = getLogger(DefaultExpressionManagerFactoryBean.class);

  @Inject
  private MuleContext muleContext;

  @Inject
  @Named(OBJECT_DW_EXPRESSION_LANGUAGE_ADAPTER)
  private ExtendedExpressionLanguageAdaptor dwExpressionLanguage;

  @Inject
  // Avoid this to fail on unit tests with an incomplete repository
  @Autowired(required = false)
  private final List<GlobalBindingContextProvider> globalBindingContextProviders = emptyList();

  @Override
  public ExtendedExpressionManager getObject() throws Exception {
    DefaultExpressionManager delegate = createBaseObject();
    ExtendedExpressionLanguageAdaptor expressionLanguage = createExpressionLanguage();
    populateBindings(expressionLanguage);
    delegate.setExpressionLanguage(expressionLanguage);
    delegate.setMuleConfiguration(muleContext.getConfiguration());
    delegate.setStreamingManager(muleContext.getStreamingManager());

    muleContext.getInjector().inject(delegate);
    return (ExtendedExpressionManager) createClassLoaderInjectorInvocationHandler(delegate,
                                                                                  muleContext.getExecutionClassLoader());
  }

  protected void populateBindings(ExtendedExpressionLanguageAdaptor expressionLanguage) {
    BindingContext.Builder contextBuilder = BindingContext.builder();

    globalBindingContextProviders.stream()
        .map(GlobalBindingContextProvider::getBindingContext)
        .forEach(contextBuilder::addAll);

    expressionLanguage.addGlobalBindings(contextBuilder instanceof DefaultBindingContextBuilder
        ? ((DefaultBindingContextBuilder) contextBuilder).flattenAndBuild()
        : contextBuilder.build());
  }

  protected DefaultExpressionManager createBaseObject() {
    return new DefaultExpressionManager();
  }

  private ExtendedExpressionLanguageAdaptor createExpressionLanguage() {
    if (dwExpressionLanguage == null) {
      throw new IllegalStateException("No expression language installed");
    }
    return dwExpressionLanguage;
  }

  @Override
  public Class<?> getObjectType() {
    return ExtendedExpressionManager.class;
  }

  @Override
  public boolean isSingleton() {
    return true;
  }
}
