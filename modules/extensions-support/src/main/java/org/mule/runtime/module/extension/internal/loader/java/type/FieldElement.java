/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.type;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;

/**
 * A contract for an element to be considered as a Field
 *
 * @since 4.0
 */
public interface FieldElement extends ExtensionParameter {

  /**
   * @return The represented {@link Field}
   */
  // TODO MULE-10137 - Adapt logic to AST
  Field getField();

  /**
   * {@inheritDoc}
   */
  @Override
  default AnnotatedElement getDeclaringElement() {
    return getField();
  }
}
