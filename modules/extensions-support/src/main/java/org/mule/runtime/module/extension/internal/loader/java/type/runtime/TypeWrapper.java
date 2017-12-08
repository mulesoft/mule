/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.type.runtime;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

import org.mule.runtime.module.extension.internal.loader.java.type.AnnotationValueFetcher;
import org.mule.runtime.module.extension.internal.loader.java.type.FieldElement;
import org.mule.runtime.module.extension.internal.loader.java.type.Type;
import org.mule.runtime.module.extension.internal.loader.java.type.GenericInfo;
import org.mule.runtime.module.extension.internal.util.IntrospectionUtils;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.core.ResolvableType;

/**
 * Wrapper for {@link Class} that provide utility methods to facilitate the introspection of a {@link Class}
 *
 * @since 4.0
 */
public class TypeWrapper implements Type {

  private final Class<?> aClass;
  private final java.lang.reflect.Type type;
  private List<GenericInfo> generics = Collections.emptyList();

  public TypeWrapper(Class<?> aClass) {
    this.aClass = aClass;
    this.type = aClass;
  }

  public TypeWrapper(ResolvableType resolvableType) {
    this.aClass = resolvableType.getRawClass();
    this.type = resolvableType.getType();
    generics = new ArrayList<>();
    for (ResolvableType type : resolvableType.getGenerics()) {
      TypeWrapper concreteType = new TypeWrapper(type);
      generics.add(new GenericInfo(concreteType, concreteType.getGenerics()));
    }
  }

  public TypeWrapper(java.lang.reflect.Type type) {
    this(ResolvableType.forType(type));
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

  @Override
  public <A extends Annotation> Optional<AnnotationValueFetcher<A>> getValueFromAnnotation(Class<A> annotationClass) {
    return isAnnotatedWith(annotationClass) ? of(new ClassBasedAnnotationValueFetcher<>(annotationClass, aClass)) : empty();
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


  @Override
  public boolean isAssignableFrom(Class<?> clazz) {
    return aClass != null && aClass.isAssignableFrom(clazz);
  }

  @Override
  public boolean isAssignableTo(Class<?> clazz) {
    return aClass != null && clazz.isAssignableFrom(aClass);
  }

  @Override
  public java.lang.reflect.Type getReflectType() {
    return type;
  }

  @Override
  public String getTypeName() {
    return aClass.getTypeName();
  }

  @Override
  public List<GenericInfo> getGenerics() {
    return generics;
  }
}
