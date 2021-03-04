/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util;

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
