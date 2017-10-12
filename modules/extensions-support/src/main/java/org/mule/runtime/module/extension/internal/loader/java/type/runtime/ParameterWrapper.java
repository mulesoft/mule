/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.type.runtime;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static org.springframework.core.ResolvableType.forMethodParameter;

import org.mule.runtime.module.extension.internal.loader.java.type.InfrastructureTypeMapping;
import org.mule.runtime.module.extension.internal.loader.java.type.ParameterElement;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
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

  public ParameterWrapper(Method owner, int index) {
    this.index = index;
    this.parameter = owner.getParameters()[index];
    this.owner = owner;
  }

  /**
   * @return the wrapped {@link Parameter}
   */
  @Override
  public Parameter getParameter() {
    return parameter;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TypeWrapper getType() {
    return new TypeWrapper(parameter.getType());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Annotation[] getAnnotations() {
    return parameter.getAnnotations();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <A extends Annotation> Optional<A> getAnnotation(Class<A> annotationClass) {
    return Optional.ofNullable(parameter.getAnnotation(annotationClass));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getAlias() {
    return ofNullable(InfrastructureTypeMapping.getMap().get(parameter.getType()))
        .map(InfrastructureTypeMapping.InfrastructureType::getName).orElse(ParameterElement.super.getAlias());
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

  /**
   * {@inheritDoc}
   */
  @Override
  public Type getJavaType() {
    return forMethodParameter(owner, index).getType();
  }
}
