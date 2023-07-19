/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
   * @param value  the value to set
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
