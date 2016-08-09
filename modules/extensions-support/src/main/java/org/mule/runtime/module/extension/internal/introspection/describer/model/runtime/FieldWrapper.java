/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.describer.model.runtime;

import static java.lang.String.format;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.module.extension.internal.introspection.describer.model.FieldElement;
import org.mule.runtime.module.extension.internal.introspection.describer.model.InfrastructureTypeMapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Optional;

import org.springframework.core.ResolvableType;

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
    return Optional.ofNullable(field.getAnnotation(annotationClass));
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
  public MetadataType getMetadataType(ClassTypeLoader typeLoader) {
    return typeLoader.load(ResolvableType.forField(field).getType());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getAlias() {
    return InfrastructureTypeMapping.getMap().getOrDefault(field.getType(), FieldElement.super.getAlias());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getOwnerDescription() {
    return format("Class: '%s'", field.getDeclaringClass().getSimpleName());
  }
}
