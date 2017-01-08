/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.generics;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Interface to discover parameter names for methods and constructors.
 * <p/>
 * <p>
 * Parameter name discovery is not always possible, but various strategies are available to try, such as looking for debug
 * information that may have been emitted at compile time, and looking for argname annotation values optionally accompanying
 * AspectJ annotated methods.
 * <p/>
 * author: Spring
 */
public interface ParameterNameDiscoverer {

  /**
   * Return parameter names for this method, or <code>null</code> if they cannot be determined.
   *
   * @param method method to find parameter names for
   * @return an array of parameter names if the names can be resolved, or <code>null</code> if they cannot
   */
  String[] getParameterNames(Method method);

  /**
   * Return parameter names for this constructor, or <code>null</code> if they cannot be determined.
   *
   * @param ctor constructor to find parameter names for
   * @return an array of parameter names if the names can be resolved, or <code>null</code> if they cannot
   */
  String[] getParameterNames(Constructor ctor);

}
