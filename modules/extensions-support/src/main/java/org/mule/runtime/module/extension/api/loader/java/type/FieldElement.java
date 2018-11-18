/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.loader.java.type;

import org.mule.api.annotation.NoImplement;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.util.Optional;

/**
 * A contract for an element to be considered as a Field
 *
 * @since 4.0
 */
@NoImplement
public interface FieldElement extends ExtensionParameter {

  /**
   * @return The represented {@link Field}
   */
  Optional<Field> getField();

  /**
   * Sets the {@code value} into the {@code target} instance
   *
   * @param target the object on which the field value is to be set
   * @param value the value to set
   */
  default void set(Object object, Object value) {
    // Nothing to do by default
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default Optional<AnnotatedElement> getDeclaringElement() {
    Object field = getField();
    return (Optional<AnnotatedElement>) field;
  }
}
