/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.execution;

import static java.lang.Thread.currentThread;
import static java.lang.reflect.Proxy.newProxyInstance;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.api.util.ClassUtils.findImplementedInterfaces;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Proxies a given object in order to set the right classloader on the current thread's context classloader before calling any
 * method on it.
 *
 * @since 4.0
 */
public class ClassLoaderInjectorInvocationHandler implements InvocationHandler {

  private final ClassLoader classLoader;
  private final Object delegate;

  private ClassLoaderInjectorInvocationHandler(Object delegate, ClassLoader classLoader) {
    this.classLoader = classLoader;
    this.delegate = delegate;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    try {
      ClassLoader originalLoader = currentThread().getContextClassLoader();

      try {
        currentThread().setContextClassLoader(classLoader);

        return method.invoke(delegate, args);
      } finally {
        currentThread().setContextClassLoader(originalLoader);
      }
    } catch (InvocationTargetException ite) {
      throw ite.getTargetException();
    }
  }

  /**
   * Creates a new proxy
   *
   * @param delegate object to be proxied. Non null
   * @param classLoader classloader to be configured on the current thread's context classlaoder before invoking a method on {@link #delegate}. Non null.
   * @return a proxy for the given object
   */
  public static Object createClassLoaderInjectorInvocationHandler(Object delegate, ClassLoader classLoader) {
    checkArgument(delegate != null, "delegate cannot be null");
    checkArgument(classLoader != null, "classloader cannot be null");

    InvocationHandler handler = new ClassLoaderInjectorInvocationHandler(delegate, classLoader);

    return newProxyInstance(classLoader, findImplementedInterfaces(delegate.getClass()), handler);
  }
}
