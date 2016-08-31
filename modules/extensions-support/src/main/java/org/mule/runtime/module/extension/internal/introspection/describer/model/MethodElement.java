/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.describer.model;

import org.mule.runtime.extension.api.introspection.Named;

import java.lang.reflect.Method;

/**
 * A contract for an element to be considered as a Method
 *
 * @since 4.0
 */
public interface MethodElement extends WithParameters, WithReturnType, Named, WithAnnotations, WithAlias, WithDeclaringClass {

  /**
   * @return The represented method
   */
  // TODO MULE-10137 - Adapt logic to AST
  Method getMethod();
}
