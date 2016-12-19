/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.type;

import static java.util.Arrays.stream;

import java.lang.annotation.Annotation;
import java.util.Optional;

/**
 * A generic contract for any kind of component that can be annotated
 *
 * @since 4.0
 */
public interface WithAnnotations {

  /**
   * @return the array of annotations that the {@link WithAnnotations} component is annotated with
   */
  Annotation[] getAnnotations();

  /**
   * Retrieves an annotation of the {@link WithAnnotations} component
   *
   * @param annotationClass Of the annotation to retrieve
   * @param <A> The annotation type
   * @return The {@link Optional} annotation to retrieve
   */
  <A extends Annotation> Optional<A> getAnnotation(Class<A> annotationClass);

  /**
   * @param annotation The annotation to verify if the, {@link WithAnnotations} is annotated with.
   * @return A {@code boolean} indicating if the {@link WithAnnotations} element is annotated with the given {@code annotation}
   */
  default boolean isAnnotatedWith(Class<? extends Annotation> annotation) {
    return stream(getAnnotations()).anyMatch(foundAnnotation -> foundAnnotation.annotationType().isAssignableFrom(annotation));
  }
}
