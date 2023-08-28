/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.loader.java.property;

import org.mule.runtime.api.meta.model.ModelProperty;

import java.lang.reflect.Field;

/**
 * Base implementation for {@link ModelProperty} that act over a particular {@link Field}
 *
 * @since 4.0
 */
public abstract class InjectedFieldModelProperty implements ModelProperty {

  /**
   * The {@link Field} on which the default encoding should be injected
   */
  private final Field field;

  /**
   * Creates a new instance
   *
   * @param field on which the value should be injected
   */
  public InjectedFieldModelProperty(Field field) {
    this.field = field;
  }

  /**
   * @return on which the value should be injected
   */
  public Field getField() {
    return field;
  }

  /**
   * @return {@code false}
   */
  @Override
  public boolean isPublic() {
    return false;
  }
}
