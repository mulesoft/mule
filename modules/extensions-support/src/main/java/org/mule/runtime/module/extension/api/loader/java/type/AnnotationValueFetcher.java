/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.loader.java.type;

import org.mule.api.annotation.NoImplement;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.function.Function;

/**
 * @param <A> Annotation type
 * @since 4.1
 */
@NoImplement
public interface AnnotationValueFetcher<A extends Annotation> {

  /**
   * Returns a {@link String} value of an {@link Annotation} property
   *
   * @param function A function which executes the logic of retrieving the property value.
   * @return The string value.
   */
  String getStringValue(Function<A, String> function);

  /**
   * Returns a {@link List} of {@link E} values of an {@link Annotation} property
   *
   * @param function A function which executes the logic of retrieving the property value.
   * @param <E>      The array type.
   * @return The list with the values
   */
  <E> List<E> getArrayValue(Function<A, E[]> function);

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
   * Returns a {@link Number} value of an {@link Annotation} property
   *
   * @param function A function which executes the logic of retrieving the property value.
   * @return The {@link Type}
   */
  <N extends Number> N getNumberValue(Function<A, N> function);

  /**
   * Returns a {@link Boolean} value of an {@link Annotation} property
   *
   * @param function A function which executes the logic of retrieving the property value.
   * @return The {@link Type}
   */
  Boolean getBooleanValue(Function<A, Boolean> function);

  /**
   * Returns the {@link Enum} value of an annotation property
   *
   * @param function A function which executes the logic of retrieving the property value.
   * @param <E>      The {@link Enum} type
   * @return The {@link Enum} value
   */
  <E extends Enum> E getEnumValue(Function<A, E> function);

  /**
   * Returns the list of {@link Enum} values of an annotation property
   *
   * @param function A function which executes the logic of retrieving the property value.
   * @param <E>      The {@link Enum} type
   * @return The {@link Enum} value
   *
   * @since 4.5
   */
  default <E extends Enum> List<E> getEnumArrayValue(Function<A, E[]> function) {
    return getArrayValue(function);
  }

  /**
   * Returns a {@link AnnotationValueFetcher} from an {@link Annotation} inside the current {@link Annotation}
   *
   * @param function A function which executes the logic of retrieving the property value.
   * @param <E>      The {@link Annotation} type
   * @return The {@link AnnotationValueFetcher} wrapping the inner annotation
   */
  <E extends Annotation> AnnotationValueFetcher<E> getInnerAnnotation(Function<A, E> function);

  /**
   * Returns a list of {@link AnnotationValueFetcher} from an {@link Annotation} inside the current {@link Annotation}
   *
   * @param function A function which executes the logic of retrieving the property value.
   * @param <E>      The {@link Annotation} type
   * @return The list {@link AnnotationValueFetcher} wrapping the inner annotations
   */
  <E extends Annotation> List<AnnotationValueFetcher<E>> getInnerAnnotations(Function<A, E[]> function);

}
