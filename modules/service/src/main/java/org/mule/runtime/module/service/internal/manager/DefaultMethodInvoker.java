/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.service.internal.manager;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Default implementation of {@link MethodInvoker} which simply invokes the method using the supplied target and arguments.
 *
 * @since 4.2
 */
public class DefaultMethodInvoker implements MethodInvoker {

  /**
   * {@inheritDoc}
   */
  @Override
  public Object invoke(Object object, Method method, Object[] args) throws Throwable {
    try {
      return method.invoke(object, args);
    } catch (InvocationTargetException e) {
      throw e.getCause();
    }
  }
}
