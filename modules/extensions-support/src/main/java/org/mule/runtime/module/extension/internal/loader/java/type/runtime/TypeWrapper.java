/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.type.runtime;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

import org.mule.runtime.module.extension.internal.loader.java.type.FieldElement;
import org.mule.runtime.module.extension.internal.loader.java.type.Type;
import org.mule.runtime.module.extension.internal.util.IntrospectionUtils;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Wrapper for {@link Class} that provide utility methods to facilitate the introspection of a {@link Class}
 *
 * @since 4.0
 */
public class TypeWrapper implements Type {

  private final Class<?> aClass;

  public TypeWrapper(Class<?> aClass) {
    this.aClass = aClass;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Annotation[] getAnnotations() {
    return aClass.getAnnotations();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return aClass.getSimpleName();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <A extends Annotation> Optional<A> getAnnotation(Class<A> annotationClass) {
    return ofNullable(aClass.getAnnotation(annotationClass));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<FieldElement> getFields() {
    return IntrospectionUtils.getFields(aClass).stream().map(FieldWrapper::new).collect(toList());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<FieldElement> getAnnotatedFields(Class<? extends Annotation>... annotations) {
    return getFields().stream().filter(field -> Stream.of(annotations).anyMatch(field::isAnnotatedWith)).collect(toList());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Class<?> getDeclaringClass() {
    return aClass;
  }
}
