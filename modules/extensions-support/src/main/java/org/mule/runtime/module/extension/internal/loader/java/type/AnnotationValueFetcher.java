/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.type;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.function.Function;

/**
 * @param <A> Annotation type
 * @since 4.1
 */
public interface AnnotationValueFetcher<A extends Annotation> {

  /**
   * Returns a {@link String} value of an {@link Annotation} property
   *
   * @param function A function which executes the logic of retrieving the property value.
   * @return The string value.
   */
  String getStringValue(Function<A, String> function);

  /**
   * Return the list of {@link Type} representing a {@link Class[]} property.
   *
   * @param function A function which executes the logic of retrieving the property value.
   * @return The list of {@link Type}
   */
  List<Type> getClassArrayValue(Function<A, Class[]> function);

  /**
   * Return the {@link Type} representing a {@link Class} property.
   *
   * @param function A function which executes the logic of retrieving the property value.
   * @return The {@link Type}
   */
  Type getClassValue(Function<A, Class> function);

  /**
   * Return the {@link Type} representing a {@link Class} property.
   *
   * @param function A function which executes the logic of retrieving the property value.
   * @return The {@link Type}
   */
  <N extends Number> N getNumberValue(Function<A, N> function);

  /**
   * Returns the {@link Enum} value of an annotation property
   *
   * @param function A function which executes the logic of retrieving the property value.
   * @param <E> The {@link Enum} type
   * @return The {@link Enum} value
   */
  <E extends Enum> E getEnumValue(Function<A, E> function);

}
