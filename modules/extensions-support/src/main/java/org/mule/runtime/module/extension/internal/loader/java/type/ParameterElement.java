/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.type;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Parameter;

/**
 * A contract for an element from which a parameter can be derived
 *
 * @since 4.0
 */
public interface ParameterElement extends ExtensionParameter {

  /**
   * @return The represented {@link Parameter}
   */
  // TODO MULE-10137 - Adapt logic to AST
  Parameter getParameter();

  /**
   * {@inheritDoc}
   */
  @Override
  default AnnotatedElement getDeclaringElement() {
    return getParameter();
  }
}
