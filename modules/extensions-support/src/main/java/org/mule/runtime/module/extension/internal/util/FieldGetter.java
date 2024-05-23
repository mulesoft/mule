/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.util;

import static org.apache.commons.lang3.StringUtils.capitalize;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class FieldGetter<Target, Value> {

  /**
   * The {@link Field} in which the value is to be assigned
   */
  private final Field field;
  private final Method getterMethod;

  public FieldGetter(Field field) {
    Method getterMethod = null;

    this.field = field;
    try {
      field.setAccessible(true);
    } catch (Exception e) {
      // create a bean property getter fallback
      try {
        getterMethod =
            field.getDeclaringClass().getDeclaredMethod("get" + capitalize(field.getName()));
      } catch (NoSuchMethodException e1) {
        e.addSuppressed(e1);
        throw e;
      }
    }

    this.getterMethod = getterMethod;
  }

  /**
   * Gets the {@code value} into the {@code target} instance
   *
   * @param target the object from which the field value is to be get
   * @return value the value
   */
  public Value get(Target target) {
    if (getterMethod != null) {
      try {
        return (Value) getterMethod.invoke(target);
      } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
        throw new IllegalStateException("Unexpected reflection exception - " + ex.getClass().getName() + ": " + ex.getMessage());
      }
    } else {
      try {
        return (Value) field.get(target);
      } catch (IllegalAccessException ex) {
        throw new IllegalStateException("Unexpected reflection exception - " + ex.getClass().getName() + ": " + ex.getMessage());
      }
    }
  }

  /**
   * @return The {@link Field} to be get
   */
  public Field getField() {
    return field;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((field == null) ? 0 : field.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    FieldGetter other = (FieldGetter) obj;
    if (field == null) {
      if (other.field != null) {
        return false;
      }
    } else if (!field.equals(other.field)) {
      return false;
    }
    return true;
  }
}
