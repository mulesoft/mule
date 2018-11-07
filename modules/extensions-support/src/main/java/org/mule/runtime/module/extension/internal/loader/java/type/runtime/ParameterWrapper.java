/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.type.runtime;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static org.mule.runtime.module.extension.internal.loader.java.type.InfrastructureTypeMapping.getInfrastructureType;
import static org.springframework.core.ResolvableType.forMethodParameter;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.module.extension.api.loader.java.type.AnnotationValueFetcher;
import org.mule.runtime.module.extension.api.loader.java.type.ParameterElement;
import org.mule.runtime.module.extension.internal.loader.java.type.InfrastructureTypeMapping;

import javax.lang.model.element.VariableElement;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Optional;

/**
 * Wrapper for {@link Parameter} that provide utility methods to facilitate the introspection of a {@link Parameter}
 *
 * @since 4.0
 */
public final class ParameterWrapper implements ParameterElement {

  private final Parameter parameter;
  private final Method owner;
  private final int index;
  private final ClassTypeLoader typeLoader;

  public ParameterWrapper(Method owner, int index, ClassTypeLoader typeLoader) {
    this.index = index;
    this.typeLoader = typeLoader;
    this.parameter = owner.getParameters()[index];
    this.owner = owner;
  }

  /**
   * @return the wrapped {@link Parameter}
   */
  @Override
  public Optional<Parameter> getParameter() {
    return Optional.ofNullable(parameter);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TypeWrapper getType() {
    return new ParameterTypeWrapper(forMethodParameter(owner, index), typeLoader);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <A extends Annotation> Optional<A> getAnnotation(Class<A> annotationClass) {
    return Optional.ofNullable(parameter.getAnnotation(annotationClass));
  }

  @Override
  public <A extends Annotation> Optional<AnnotationValueFetcher<A>> getValueFromAnnotation(Class<A> annotationClass) {
    return isAnnotatedWith(annotationClass)
        ? Optional.of(new ClassBasedAnnotationValueFetcher<>(annotationClass, parameter, typeLoader))
        : empty();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getAlias() {
    return getInfrastructureType(getType())
        .map(InfrastructureTypeMapping.InfrastructureType::getName)
        .orElse(ParameterElement.super.getAlias());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return parameter.getName();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getOwnerDescription() {
    return format("Method: '%s'", owner.getName());
  }

  @Override
  public Optional<VariableElement> getElement() {
    return empty();
  }
}
