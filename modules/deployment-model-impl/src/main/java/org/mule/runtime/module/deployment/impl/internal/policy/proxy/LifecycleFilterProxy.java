/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.policy.proxy;

import static java.lang.reflect.Proxy.newProxyInstance;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.api.util.ClassUtils.findImplementedInterfaces;

import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.container.api.MetadataInvocationHandler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Proxies an object instance to filter invocations (without raising an exception) of lifecycle methods from {@link Startable},
 * {@link Stoppable} and {@link Disposable} interfaces.
 *
 * @since 4.0
 */
public class LifecycleFilterProxy<T> extends MetadataInvocationHandler<T> {

  /**
   * Creates a new proxy for the provided object instance.
   *
   * @param object object instance to wrap. Non null.
   */
  private LifecycleFilterProxy(T object) {
    super(object);
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    if (method.getDeclaringClass() == Startable.class ||
        method.getDeclaringClass() == Stoppable.class ||
        method.getDeclaringClass() == Disposable.class) {
      return null;
    }

    return doInvoke(proxy, method, args);
  }

  /**
   * Creates a proxy for the provided object instance.
   *
   * @param object object to wrap. Non null.
   * @return a new proxy instance.
   */
  public static <T> T createLifecycleFilterProxy(T object) {
    checkArgument(object != null, "object cannot be null");
    InvocationHandler handler = new LifecycleFilterProxy(object);

    return (T) newProxyInstance(object.getClass().getClassLoader(),
                                findImplementedInterfaces(object.getClass()),
                                handler);
  }
}
