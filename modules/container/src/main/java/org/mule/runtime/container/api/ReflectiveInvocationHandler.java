/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.container.api;

import static java.lang.reflect.Proxy.getInvocationHandler;
import static java.lang.reflect.Proxy.isProxyClass;
import static java.util.Arrays.asList;
import static org.mule.runtime.api.util.Preconditions.checkArgument;

import org.mule.runtime.api.service.Service;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.LinkedList;
import java.util.List;

/**
 * Extends {@link InvocationHandler} to provide and expose metadata about the inner object implementation.
 * <p>
 * This allows for nested object {@link Proxy}ies to work as expected.
 * 
 * @since 4.0
 */
public abstract class ReflectiveInvocationHandler<T> implements InvocationHandler {

  private final T innerObject;

  /**
   * Creates a new proxy for the provided service instance.
   *
   * @param innerObject object instance to wrap. Non null.
   */
  protected ReflectiveInvocationHandler(T innerObject) {
    checkArgument(innerObject != null, "service cannot be null");
    this.innerObject = innerObject;
  }

  /**
   * @return the methods declared in the implementation of the proxied service.
   */
  protected Method[] getImplementationDeclaredMethods() {
    if (isNestedProxy()) {
      return ((ReflectiveInvocationHandler) getInvocationHandler(getProxiedObject())).getImplementationDeclaredMethods();
    } else {
      List<Method> methods = new LinkedList<>();
      Class<?> clazz = getProxiedObject().getClass();
      while (clazz != Object.class) {
        methods.addAll(asList(clazz.getDeclaredMethods()));
        clazz = clazz.getSuperclass();
      }

      return methods.toArray(new Method[methods.size()]);
    }
  }

  /**
   * Performs the actual invocation on the proxied {@link Service}, or delegates the call to an inner proxy.
   * 
   * See {@link InvocationHandler#invoke(Object, Method, Object[])}
   */
  protected Object doInvoke(Object proxy, Method method, Object[] args) throws Throwable {
    if (isNestedProxy()) {
      return getInvocationHandler(getProxiedObject()).invoke(getProxiedObject(), method, args);
    } else {
      try {
        return method.invoke(getProxiedObject(), args);
      } catch (InvocationTargetException ite) {
        // Unwrap target exception to ensure InvocationTargetException (in case of unchecked exceptions) or
        // UndeclaredThrowableException (in case of checked exceptions) is not thrown by Service instead of target exception.
        throw ite.getTargetException();
      }
    }
  }

  /**
   * The proxied object.
   */
  protected T getProxiedObject() {
    return innerObject;
  }

  private boolean isNestedProxy() {
    return isProxyClass(getProxiedObject().getClass())
        && getInvocationHandler(getProxiedObject()) instanceof ReflectiveInvocationHandler;
  }
}
