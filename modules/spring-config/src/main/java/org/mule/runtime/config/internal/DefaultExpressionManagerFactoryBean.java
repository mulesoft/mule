/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.config.internal;

import static org.mule.runtime.core.internal.execution.ClassLoaderInjectorInvocationHandler.createClassLoaderInjectorInvocationHandler;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.internal.el.DefaultExpressionManager;

import javax.inject.Inject;

import org.springframework.beans.factory.FactoryBean;

/**
 * Creates the default {@link org.mule.runtime.core.api.el.ExpressionManager}
 * <p/>
 * This factory creates a proxy on top of the real expression manager. That proxy is used to set the right classloader on the
 * current thread's context classloader before calling any method on the delegate object.
 *
 * @since 4.0
 */
public class DefaultExpressionManagerFactoryBean implements FactoryBean<ExtendedExpressionManager> {

  @Inject
  private MuleContext muleContext;

  @Override
  public ExtendedExpressionManager getObject() throws Exception {
    DefaultExpressionManager delegate = new DefaultExpressionManager();
    muleContext.getInjector().inject(delegate);

    return (ExtendedExpressionManager) createClassLoaderInjectorInvocationHandler(delegate,
                                                                                  muleContext.getExecutionClassLoader());
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
