/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.log4j.internal;

import static java.lang.String.format;

import java.lang.reflect.Field;
import java.net.URL;

public final class Utils {

  private Utils() {
    // Empty private constructor in order to avoid incorrect instantiations.
  }

  /**
   * Sets a field of an object with the given value. If this is a final field, there will be an attempt to update the value.
   * <p>
   * Notice: If the field is final and it was initialized using a constant, the value change may not be reflected in due compiler
   * optimizations. http://java.sun.com/docs/books/jls/third_edition/html/memory.html#17.5.3
   *
   * @param target    the object that holds the target field
   * @param fieldName the name of the field
   * @param value     the value to set
   * @param recursive flags to look for the field in superclasses of the target object
   * @throws IllegalAccessException the field is not reachable.
   * @throws NoSuchFieldException   the field does not exist.
   * @deprecated The usage of this method is discouraged because whether it succeeds or not depends on the Java version and also
   *             in the decisions of the target class owner about whether the field is strongly encapsulated or not.
   */
  @Deprecated
  public static void setFieldValue(Object target, String fieldName, Object value, boolean recursive)
      throws IllegalAccessException, NoSuchFieldException {
    Field field = getField(target.getClass(), fieldName, recursive);
    boolean isAccessible = field.isAccessible();

    try {
      field.setAccessible(true);
      field.set(target, value);
    } finally {
      field.setAccessible(isAccessible);
    }
  }

  /**
   * Gets a field of a given object.
   *
   * @param target    the object that contains the field
   * @param fieldName the name of the field
   * @param recursive flag to look for the field in superclasses of the target class
   *
   * @throws IllegalAccessException the field is not reachable.
   * @throws NoSuchFieldException   the field does not exist.
   * @deprecated The usage of this method is discouraged because whether it succeeds or not depends on the Java version and also
   *             in the decisions of the target class owner about whether the field is strongly encapsulated or not.
   */
  public static <T> T getFieldValue(Object target, String fieldName, boolean recursive)
      throws IllegalAccessException, NoSuchFieldException {

    Field f = getField(target.getClass(), fieldName, recursive);
    boolean isAccessible = f.isAccessible();
    try {
      f.setAccessible(true);
      return (T) f.get(target);
    } finally {
      f.setAccessible(isAccessible);
    }
  }

  private static Field getField(Class<?> targetClass, String fieldName, boolean recursive)
      throws NoSuchFieldException {
    Class<?> clazz = targetClass;
    Field field;
    while (!Object.class.equals(clazz)) {
      try {
        field = clazz.getDeclaredField(fieldName);
        return field;
      } catch (NoSuchFieldException e) {
        // ignore and look in superclass
        if (recursive) {
          clazz = clazz.getSuperclass();
        } else {
          break;
        }
      }
    }
    throw new NoSuchFieldException(format("Could not find field '%s' in class %s", fieldName, targetClass.getName()));
  }

  /**
   * Checks whether the protocol of an {@link URL} is {@code "file"} or not.
   * 
   * @param url the {@link URL}.
   * @return {@code true} if the protocol is {@code "file"}, or {@code false} otherwise.
   */
  public static boolean isFile(URL url) {
    return "file".equals(url.getProtocol());
  }
}
