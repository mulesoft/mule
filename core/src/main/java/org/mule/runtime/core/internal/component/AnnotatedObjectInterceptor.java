/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.component;

import static java.util.Collections.synchronizedMap;

import org.mule.runtime.api.meta.AbstractAnnotatedObject;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;


public class AnnotatedObjectInterceptor extends AbstractAnnotatedObject implements MethodInterceptor {

  private Map<Method, Method> overridingMethods = synchronizedMap(new HashMap<>());

  public AnnotatedObjectInterceptor(Set<Method> managedMethods) {
    for (Method method : managedMethods) {
      overridingMethods.put(method, method);
    }
  }

  @Override
  public Object intercept(Object obj, java.lang.reflect.Method method, Object[] args, MethodProxy proxy) throws Throwable {
    if (overridingMethods.containsKey(method)) {
      return overridingMethods.get(method).invoke(this, args);
    } else {
      return proxy.invokeSuper(obj, args);
    }
  }

  public Map<Method, Method> getOverridingMethods() {
    return overridingMethods;
  }

}
