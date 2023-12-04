/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.component;

import java.lang.reflect.Method;
import java.util.Set;

public interface DynamicallyComponentInterceptor {

  public Object intercept(Object obj, Method method, Object[] args,
                          Method superMethod, Object defaultValue)
      throws Throwable;

  public Set<Method> getOverridingMethods();

}
