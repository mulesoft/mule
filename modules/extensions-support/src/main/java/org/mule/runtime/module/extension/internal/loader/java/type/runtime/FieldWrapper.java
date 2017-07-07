/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.type.runtime;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static org.springframework.core.ResolvableType.forField;
import org.mule.runtime.module.extension.internal.loader.java.type.FieldElement;
import org.mule.runtime.module.extension.internal.loader.java.type.InfrastructureTypeMapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Optional;

/**
 * Wrapper for {@link Field} that provide utility methods to facilitate the introspection of a {@link Field}
 *
 * @since 4.0
 */
public class FieldWrapper implements FieldElement {

  private final Field field;

  public FieldWrapper(Field field) {
    this.field = field;
  }

  /**
   * @return the wrapped {@link Field}
   */
  @Override
  public Field getField() {
    return field;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return field.getName();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Annotation[] getAnnotations() {
    return field.getAnnotations();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <A extends Annotation> Optional<A> getAnnotation(Class<A> annotationClass) {
    return ofNullable(field.getAnnotation(annotationClass));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TypeWrapper getType() {
    return new TypeWrapper(field.getType());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public java.lang.reflect.Type getJavaType() {
    return forField(field).getType();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getAlias() {
    return ofNullable(InfrastructureTypeMapping.getMap().get(field.getType()))
        .map(InfrastructureTypeMapping.InfrastructureType::getName).orElse(FieldElement.super.getAlias());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getOwnerDescription() {
    return format("Class: '%s'", field.getDeclaringClass().getSimpleName());
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof FieldWrapper) {
      return ((FieldWrapper) obj).getField().equals(field);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return field.hashCode();
  }
}
