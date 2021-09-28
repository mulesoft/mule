/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.type.runtime;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.extension.api.declaration.type.DefaultExtensionsTypeLoaderFactory;
import org.mule.runtime.module.extension.api.loader.java.type.AnnotationValueFetcher;
import org.mule.runtime.module.extension.api.loader.java.type.Type;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * {@link AnnotationValueFetcher} implementation which works directly with classes.
 *
 * @param <T>
 * @since 4.1
 */
public class ClassBasedAnnotationValueFetcher<T extends Annotation> implements AnnotationValueFetcher<T> {

  private ClassTypeLoader typeLoader;
  private LazyValue<T> annotation;

  public ClassBasedAnnotationValueFetcher(Class<T> annotationClass, AnnotatedElement annotatedElement,
                                          ClassTypeLoader typeLoader) {
    this.typeLoader = typeLoader;
    this.annotation = new LazyValue<>(() -> annotatedElement.getAnnotation(annotationClass));
  }

  public ClassBasedAnnotationValueFetcher(T annotation, ClassTypeLoader typeLoader) {
    this.typeLoader = typeLoader;
    this.annotation = new LazyValue<>(annotation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getStringValue(Function<T, String> function) {
    return function.apply(annotation.get());
  }

  @Override
  public <E> List<E> getArrayValue(Function<T, E[]> function) {
    return asList(function.apply(annotation.get()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Type> getClassArrayValue(Function<T, Class[]> function) {
    return Stream.of(function.apply(annotation.get()))
        .map(e -> new TypeWrapper(e, typeLoader))
        .collect(toList());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TypeWrapper getClassValue(Function<T, Class> function) {
    return new TypeWrapper(function.apply(annotation.get()), new DefaultExtensionsTypeLoaderFactory().createTypeLoader());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <N extends Number> N getNumberValue(Function<T, N> function) {
    return function.apply(annotation.get());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Boolean getBooleanValue(Function<T, Boolean> function) {
    return function.apply(annotation.get());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <E extends Enum> E getEnumValue(Function<T, E> function) {
    return function.apply(annotation.get());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <E extends Annotation> AnnotationValueFetcher<E> getInnerAnnotation(Function<T, E> function) {
    return new ClassBasedAnnotationValueFetcher<>(function.apply(annotation.get()), typeLoader);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <E extends Annotation> List<AnnotationValueFetcher<E>> getInnerAnnotations(Function<T, E[]> function) {
    return Stream.of(function.apply(annotation.get()))
        .map(e -> new ClassBasedAnnotationValueFetcher<>(e, typeLoader))
        .collect(toList());
  }

}
