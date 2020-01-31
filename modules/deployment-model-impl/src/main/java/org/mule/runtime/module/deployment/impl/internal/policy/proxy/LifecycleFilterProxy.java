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
import static org.reflections.ReflectionUtils.getAllMethods;

import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.container.internal.MetadataInvocationHandler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Set;

import javax.inject.Inject;

/**
 * Proxies an object instance to filter invocations (without raising an exception) of lifecycle methods from {@link Startable},
 * {@link Stoppable} and {@link Disposable} interfaces and will also prevent any dependency injection.
 *
 * @since 4.0
 */
public class LifecycleFilterProxy<T> extends MetadataInvocationHandler<T> {

  private final Set<Method> lifecycleMethods;

  /**
   * Creates a new proxy for the provided object instance.
   *
   * @param object object instance to wrap. Non null.
   */
  private LifecycleFilterProxy(T object, Set<Method> lifecycleMethods) {
    super(object);
    this.lifecycleMethods = lifecycleMethods;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    if (lifecycleMethods.contains(method)) {
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

    final Set<Method> lifecycleMethods = getAllMethods(object.getClass(),
                                                       method -> method.getDeclaringClass() == Startable.class ||
                                                           method.getDeclaringClass() == Stoppable.class ||
                                                           method.getDeclaringClass() == Disposable.class ||
                                                           method.getName().equals("setMuleContext") ||
                                                           method.isAnnotationPresent(Inject.class));

    // If the object doesn't have any lifecycle methods to intercept, do not proxy it.
    if (lifecycleMethods.isEmpty()) {
      return object;
    }

    InvocationHandler handler = new LifecycleFilterProxy<>(object, lifecycleMethods);

    return (T) newProxyInstance(object.getClass().getClassLoader(),
                                findImplementedInterfaces(object.getClass()),
                                handler);
  }
}
